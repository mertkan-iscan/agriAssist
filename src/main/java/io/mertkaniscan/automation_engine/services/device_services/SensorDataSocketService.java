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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
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

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private SensorConfigService sensorConfigService;


    public List<SensorData> fetchSensorData(int deviceID) throws Exception {

        Device device = deviceService.getDeviceById(deviceID);

        if (device == null) {
            throw new Exception("Device not found with ID: " + deviceID);
        }

        // Check if the device is a sensor
        if (!device.isSensor()) {
            throw new Exception("Device with ID " + deviceID + " is not a sensor device.");
        }

        String deviceIp = device.getDeviceIp();

        Callable<List<SensorData>> fetchSensorDataTask = () -> {
            try (Socket socket = new Socket(deviceIp, 5000);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                String command = DeviceJsonMessageFactory.pullSensorData();
                out.println(command);

                // Read sensor data
                String sensorDataJson = in.readLine();

                if (sensorDataJson == null || sensorDataJson.isEmpty()) {
                    throw new Exception("Received empty sensor data from device ID: " + deviceID);
                }

                // Parse and validate sensor data
                return parseAndValidateSensorData(sensorDataJson, device);

            } catch (IOException e) {
                throw new Exception("Error communicating with device ID " + deviceID + ": " + e.getMessage());
            }
        };

        Future<List<SensorData>> future = executorService.submit(fetchSensorDataTask);

        try {
            return future.get(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(true); // Cancel the task if it times out
            throw new Exception("Timeout: No response from device within 10 seconds. Device ID: " + deviceID);
        }
    }

    public List<SensorDataDTO> fetchSensorDataValue(int deviceID) throws Exception {

        Device device = deviceService.getDeviceById(deviceID);

        if (device == null) {
            throw new Exception("Device not found with ID: " + deviceID);
        }

        // Check if the device is a sensor
        if (!device.isSensor()) {
            throw new Exception("Device with ID " + deviceID + " is not a sensor device.");
        }

        String deviceIp = device.getDeviceIp();

        Callable<List<SensorDataDTO>> fetchSensorDataValueTask = () -> {
            try (Socket socket = new Socket(deviceIp, 5000);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                String command = DeviceJsonMessageFactory.pullSensorData();
                out.println(command);

                // Read sensor data
                String sensorDataJson = in.readLine();

                if (sensorDataJson == null || sensorDataJson.isEmpty()) {
                    throw new Exception("Received empty sensor data from device ID: " + deviceID);
                }

                // Parse and validate sensor data for DTO
                return parseAndValidateSensorDataValue(sensorDataJson, device);

            } catch (IOException e) {
                throw new Exception("Error communicating with device ID " + deviceID + ": " + e.getMessage());
            }
        };

        Future<List<SensorDataDTO>> future = executorService.submit(fetchSensorDataValueTask);

        try {
            return future.get(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(true); // Cancel the task if it times out
            throw new Exception("Timeout: No response from device within 10 seconds. Device ID: " + deviceID);
        }
    }


    private List<SensorData> parseAndValidateSensorData(String sensorDataJson, Device device) throws Exception {
        List<SensorData> sensorDataList = new ArrayList<>();

        try {
            JsonObject jsonObject = JsonParser.parseString(sensorDataJson).getAsJsonObject();

            // Get the expected data types for this sensor type
            String sensorModel = device.getDeviceModel(); // Assuming device has a getDeviceType() method
            List<String> expectedDataTypes = sensorConfigService.getExpectedDataTypesForSensorType(sensorModel);

            if (expectedDataTypes == null) {
                logger.warn("No configuration found for sensor type: {}", sensorModel);
                return sensorDataList;
            }

            for (String expectedDataType : expectedDataTypes) {

                if (jsonObject.has(expectedDataType)) {

                    JsonElement jsonElement = jsonObject.get(expectedDataType);
                    BigDecimal dataValue;

                    if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isNumber()) {
                        dataValue = jsonElement.getAsBigDecimal();
                    } else {
                        logger.warn("Data value for '{}' is not a valid number. Device ID: {}", expectedDataType, device.getDeviceID());
                        continue;
                    }

                    // Create and add SensorData
                    SensorData sensorData = new SensorData();

                    sensorData.setDevice(device);
                    sensorData.setField(device.getField());
                    sensorData.setDataType(expectedDataType);
                    sensorData.setDataValue(dataValue);

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

    private List<SensorDataDTO> parseAndValidateSensorDataValue(String sensorDataJson, Device device) throws Exception {
        List<SensorDataDTO> sensorDataList = new ArrayList<>();

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
                    BigDecimal dataValue;

                    if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isNumber()) {
                        dataValue = jsonElement.getAsBigDecimal();
                    } else {
                        logger.warn("Data value for '{}' is not a valid number. Device ID: {}", expectedDataType, device.getDeviceID());
                        continue;
                    }

                    // Create and add SensorDataDTO
                    SensorDataDTO sensorData = new SensorDataDTO(expectedDataType, dataValue);
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

}
