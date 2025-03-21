package io.mertkaniscan.automation_engine.services.device_services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mertkaniscan.automation_engine.models.Device;
import io.mertkaniscan.automation_engine.services.main_services.DeviceService;
import static io.mertkaniscan.automation_engine.utils.DeviceJsonMessageFactory.createValveActuatorCommand;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class ActuatorCommandSocketService {

    private static final Logger logger = LogManager.getLogger(ActuatorCommandSocketService.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Autowired
    private DeviceService deviceService;

    public String sendActuatorCommand(int deviceID, int degree) throws Exception {
        Device device = deviceService.getDeviceById(deviceID);

        if (device == null) {
            throw new Exception("Device not found with ID: " + deviceID);
        }

        if (!device.isActuator()) {
            throw new Exception("Device with ID " + deviceID + " is not an actuator device.");
        }

        logger.info("Locking device with ID: {}", device.getDeviceID());
        device.lock();
        try {
            return communicateWithActuator(device, degree);
        } finally {
            logger.info("Unlocking device with ID: {}", device.getDeviceID());
            device.unlock();
        }
    }

    private String communicateWithActuator(Device device, int degree) throws Exception {
        String deviceIp = device.getDeviceIp();

        Callable<String> sendCommandTask = () -> {
            try (Socket socket = new Socket(deviceIp, 5000);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // Create the actuator command JSON
                String command = createValveActuatorCommand(degree);
                out.println(command);

                // Read response from the actuator
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
                logger.error("Error communicating with actuator ID {}: {}", device.getDeviceID(), e.getMessage());
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

    public void startIrrigation(int fieldId, double flowRate, int duration) throws Exception {
        List<Device> actuators = deviceService.getDevicesByFieldID(fieldId).stream()
                .filter(Device::isActuator)
                .toList();

        if (actuators.isEmpty()) {
            throw new Exception("No actuators found for field ID: " + fieldId);
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(actuators.size());

        for (Device actuator : actuators) {
            Map<Double, Integer> calibrationMap = actuator.getCalibrationMap();

            if (calibrationMap == null || calibrationMap.isEmpty()) {
                throw new Exception("Calibration map is missing or empty for actuator ID: " + actuator.getDeviceID());
            }

            // Find the corresponding degree for the requested flow rate
            Integer degree = calibrationMap.get(flowRate);
            if (degree == null) {
                throw new Exception("Flow rate " + flowRate + " is not calibrated for actuator ID: " + actuator.getDeviceID());
            }

            logger.info("Locking actuator with ID: {} for opening", actuator.getDeviceID());
            actuator.lock();
            try {
                sendActuatorCommand(actuator.getDeviceID(), degree);
            } finally {
                logger.info("Unlocking actuator with ID: {} after opening", actuator.getDeviceID());
                actuator.unlock();
            }

            // Schedule a task to close the actuator after the specified duration
            scheduler.schedule(() -> {
                logger.info("Locking actuator with ID: {} for closing", actuator.getDeviceID());
                actuator.lock();
                try {
                    sendActuatorCommand(actuator.getDeviceID(), 0); // Close the valve
                } catch (Exception e) {
                    logger.error("Error closing actuator ID {}: {}", actuator.getDeviceID(), e.getMessage());
                } finally {
                    logger.info("Unlocking actuator with ID: {} after closing", actuator.getDeviceID());
                    actuator.unlock();
                }
            }, duration, TimeUnit.SECONDS);
        }

        // Optional: Shut down the scheduler gracefully after all tasks are complete
        scheduler.shutdown();
    }
}
