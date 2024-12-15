package io.mertkaniscan.automation_engine.services.device_services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.mertkaniscan.automation_engine.models.SensorData;
import io.mertkaniscan.automation_engine.services.SensorConfigService;
import io.mertkaniscan.automation_engine.services.main_services.DeviceService;
import io.mertkaniscan.automation_engine.utils.DeviceJsonMessageFactory;
import io.mertkaniscan.automation_engine.models.Device;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Service
public class SensorDataSocketService {

    private static final Logger logger = LogManager.getLogger(SensorDataSocketService.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private final DeviceService deviceService;
    private final SensorConfigService sensorConfigService;

    public SensorDataSocketService(DeviceService deviceService, SensorConfigService sensorConfigService) {
        this.deviceService = deviceService;
        this.sensorConfigService = sensorConfigService;
    }

    public <T> List<T> fetchSensorData(int deviceID, DataParser<T> parser) throws Exception {
        Device device = deviceService.getDeviceById(deviceID);

        if (device == null) {
            throw new Exception("Device not found with ID: " + deviceID);
        }

        if (!device.isSensor()) {
            throw new Exception("Device with ID " + deviceID + " is not a sensor device.");
        }

        logger.info("Locking device with ID: {}", device.getDeviceID());
        device.lock();
        try {
            return communicateWithDevice(device, parser);
        } finally {
            logger.info("Unlocking device with ID: {}", device.getDeviceID());
            device.unlock();
        }
    }

    private <T> List<T> communicateWithDevice(Device device, DataParser<T> parser) throws Exception {
        String deviceIp = device.getDeviceIp();

        Callable<List<T>> fetchSensorDataTask = () -> {
            try (Socket socket = new Socket(deviceIp, 5000);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                String command = DeviceJsonMessageFactory.pullSensorData();
                out.println(command);

                String sensorDataJson = in.readLine();

                if (sensorDataJson == null || sensorDataJson.isEmpty()) {
                    throw new Exception("Received empty sensor data from device ID: " + device.getDeviceID());
                }

                return parser.parse(sensorDataJson, device);

            } catch (IOException e) {
                throw new Exception("Error communicating with device ID " + device.getDeviceID() + ": " + e.getMessage());
            }
        };

        Future<List<T>> future = executorService.submit(fetchSensorDataTask);

        try {
            return future.get(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(true);
            throw new Exception("Timeout: No response from device within 10 seconds. Device ID: " + device.getDeviceID());
        }
    }

    public List<SensorData> fetchSensorData(int deviceID) throws Exception {
        return fetchSensorData(deviceID, this::parseAndValidateSensorData);
    }

    public List<SensorDataDTO> fetchSensorDataValue(int deviceID) throws Exception {
        return fetchSensorData(deviceID, this::parseAndValidateSensorDataValue);
    }

    private List<SensorData> parseAndValidateSensorData(String sensorDataJson, Device device) throws Exception {

        return parseSensorData(sensorDataJson, device, (dataType, dataValue) -> {

            SensorData sensorData = new SensorData();
            sensorData.setDataType(dataType);
            sensorData.setDataValue(dataValue);
            sensorData.setDevice(device);
            sensorData.setField(device.getField());
            return sensorData;
        });
    }

    private List<SensorDataDTO> parseAndValidateSensorDataValue(String sensorDataJson, Device device) throws Exception {
        return parseSensorData(sensorDataJson, device, SensorDataDTO::new);
    }

    private <T> List<T> parseSensorData(String sensorDataJson, Device device, DataFactory<T> factory) throws Exception {
        List<T> sensorDataList = new ArrayList<>();

        try {
            JsonObject jsonObject = JsonParser.parseString(sensorDataJson).getAsJsonObject();
            String sensorModel = device.getDeviceModel();
            List<String> expectedDataTypes = sensorConfigService.getExpectedDataTypesForSensorType(sensorModel);

            if (expectedDataTypes == null) {
                logger.warn("No configuration found for sensor type: {}", sensorModel);
                return sensorDataList;
            }

            for (String expectedDataType : expectedDataTypes) {
                if (jsonObject.has(expectedDataType)) {
                    JsonElement jsonElement = jsonObject.get(expectedDataType);
                    Double dataValue;

                    if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isNumber()) {
                        dataValue = jsonElement.getAsDouble();
                    } else {
                        logger.warn("Data value for '{}' is not a valid number. Device ID: {}", expectedDataType, device.getDeviceID());
                        continue;
                    }

                    T sensorData = factory.create(expectedDataType, dataValue);
                    sensorDataList.add(sensorData);
                } else {
                    logger.warn("Expected data type '{}' not found in sensor data from device ID {}.", expectedDataType, device.getDeviceID());
                }
            }

        } catch (JsonSyntaxException e) {
            throw new Exception("Invalid JSON format received from device ID " + device.getDeviceID() + ": " + e.getMessage());
        }

        return sensorDataList;
    }

    public Optional<Double> getDataValueByType(List<SensorDataDTO> sensorDataList, String dataType) {
        return sensorDataList.stream()
                .filter(sensorDataDTO -> sensorDataDTO.getDataType().equals(dataType))
                .map(SensorDataDTO::getDataValue)
                .findFirst();
    }

    @FunctionalInterface
    private interface DataParser<T> {
        List<T> parse(String json, Device device) throws Exception;
    }

    @FunctionalInterface
    private interface DataFactory<T> {
        T create(String dataType, Double dataValue);
    }
}
