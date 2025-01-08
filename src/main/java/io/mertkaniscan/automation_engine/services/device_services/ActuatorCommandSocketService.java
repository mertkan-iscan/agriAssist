package io.mertkaniscan.automation_engine.services.device_services;

import io.mertkaniscan.automation_engine.utils.SensorReadingConverter;
import io.mertkaniscan.automation_engine.utils.config_loader.DeviceCommandConfigLoader;
import lombok.extern.slf4j.Slf4j;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mertkaniscan.automation_engine.models.Device;
import io.mertkaniscan.automation_engine.services.main_services.DeviceService;
import io.mertkaniscan.automation_engine.components.DeviceLockManager;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

@Slf4j
@Service
public class ActuatorCommandSocketService {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private final DeviceLockManager deviceLockManager;
    private final DeviceService deviceService;
    private final DeviceCommandConfigLoader deviceCommandConfigLoader;
    private final SensorReadingConverter sensorReadingConverter;

    public ActuatorCommandSocketService(DeviceLockManager deviceLockManager, DeviceService deviceService, DeviceCommandConfigLoader deviceCommandConfigLoader, SensorReadingConverter sensorReadingConverter) {
        this.deviceLockManager = deviceLockManager;
        this.deviceService = deviceService;
        this.deviceCommandConfigLoader = deviceCommandConfigLoader;
        this.sensorReadingConverter = sensorReadingConverter;
    }

    public String sendActuatorCommand(int deviceID, int degree) throws Exception {
        Device device = deviceService.getDeviceById(deviceID);

        if (device == null) {
            throw new Exception("Device not found with ID: " + deviceID);
        }

        if (!device.isActuator()) {
            throw new Exception("Device with ID " + deviceID + " is not an actuator device.");
        }

        Lock lock = deviceLockManager.getLockForDevice(deviceID);

        try {
            if (!lock.tryLock(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Device is already locked by another operation.");
            }

            return communicateWithActuator(device, degree);
        } finally {
            lock.unlock();
        }
    }


    private String communicateWithActuator(Device device, int degree) throws Exception {
        String deviceIp = device.getDeviceIp();
        Integer devicePort = device.getDevicePort();

        Callable<String> sendCommandTask = () -> {

            try (Socket socket = new Socket(deviceIp, devicePort);

                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                String command = deviceCommandConfigLoader.getValveActuatorCommand(degree);

                out.println(command);

                String responseJson = in.readLine();

                if (responseJson == null || responseJson.isEmpty()) {
                    throw new Exception("Empty response received from actuator with ID: " + device.getDeviceID());
                }

                // Validate the response
                JsonObject response = JsonParser.parseString(responseJson).getAsJsonObject();
                if (!"success".equalsIgnoreCase(response.get("messageType").getAsString())) {
                    throw new Exception("Failed response from actuator: " + responseJson);
                }

                return responseJson;

            } catch (Exception e) {
                log.error("Error communicating with actuator ID {}: {}", device.getDeviceID(), e.getMessage());
                throw new Exception("Error communicating with actuator ID " + device.getDeviceID() + ": " + e.getMessage());
            }
        };

        Future<String> future = executorService.submit(sendCommandTask);

        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.cancel(true); // Cancel the task if it times out
            throw new Exception("Timeout: No response from actuator within 10 seconds. Device ID: " + device.getDeviceID());
        }
    }

    public void startIrrigation(int fieldId, double flowRate) throws Exception {
        List<Device> actuators = deviceService.getDevicesByFieldID(fieldId).stream()
                .filter(Device::isActuator)
                .toList();

        if (actuators.isEmpty()) {
            throw new Exception("No actuators found for field ID: " + fieldId);
        }

        for (Device actuator : actuators) {

            Integer degree = getCalibrationValue(flowRate, actuator);

            log.info("Locking actuator with ID: {} for opening", actuator.getDeviceID());

            sendActuatorCommand(actuator.getDeviceID(), degree);
        }
    }

    @NotNull
    private Integer getCalibrationValue(double flowRate, Device actuator) throws Exception {
        Map<String, Integer> calibrationMap = actuator.getCalibrationPolynomial();

        if (calibrationMap == null || calibrationMap.isEmpty()) {
            throw new Exception("Calibration map is missing or empty for actuator ID: " + actuator.getDeviceID());
        }

        // Find the corresponding degree for the requested flow rate
        Integer degree = sensorReadingConverter.convertFlowRate(flowRate, calibrationMap, actuator.getMinFlowRate(), actuator.getMaxFlowRate());

        if (degree == null) {
            throw new Exception("Flow rate " + flowRate + " is not calibrated for actuator ID: " + actuator.getDeviceID());
        }
        return degree;
    }


    public void stopIrrigation(int fieldId) throws Exception {
        List<Device> actuators = deviceService.getDevicesByFieldID(fieldId).stream()
                .filter(Device::isActuator)
                .toList();

        if (actuators.isEmpty()) {
            throw new Exception("No actuators found for field ID: " + fieldId);
        }

        for (Device actuator : actuators) {
            log.info("Locking actuator with ID: {} for closing", actuator.getDeviceID());
            try {
                sendActuatorCommand(actuator.getDeviceID(), 0); // Close the valve
                log.info("Irrigation stopped for actuator ID: {}", actuator.getDeviceID());
            } catch (Exception e) {
                log.error("Error stopping irrigation for actuator ID {}: {}", actuator.getDeviceID(), e.getMessage());
                throw new Exception("Failed to stop irrigation for actuator ID: " + actuator.getDeviceID());
            }
        }
    }
}
