package io.mertkaniscan.automation_engine.services.device_services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.mertkaniscan.automation_engine.components.ScheduledSensorDataFetcher;
import io.mertkaniscan.automation_engine.services.main_services.DeviceService;
import io.mertkaniscan.automation_engine.utils.DeviceSocketWrapper;
import io.mertkaniscan.automation_engine.models.Device;
import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.utils.config_loader.DeviceCommandConfigLoader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class DeviceJoinService {


    private final ExecutorService clientHandlerPool = Executors.newCachedThreadPool();
    private final AtomicInteger activeDeviceTCPConnections = new AtomicInteger(0);
    private final List<DeviceSocketWrapper> activeDeviceSockets = new CopyOnWriteArrayList<>();

    private final DeviceService deviceService;
    private final FieldService fieldService;
    private final ScheduledSensorDataFetcher scheduledSensorDataFetcher;
    private final SimpMessagingTemplate messagingTemplate;
    private final DeviceCommandConfigLoader deviceCommandConfigLoader;

    @Autowired
    public DeviceJoinService(DeviceService deviceService, FieldService fieldService, ScheduledSensorDataFetcher scheduledSensorDataFetcher, SimpMessagingTemplate messagingTemplate, DeviceCommandConfigLoader deviceCommandConfigLoader) {
        this.deviceService = deviceService;
        this.fieldService = fieldService;
        this.scheduledSensorDataFetcher = scheduledSensorDataFetcher;
        this.messagingTemplate = messagingTemplate;
        this.deviceCommandConfigLoader = deviceCommandConfigLoader;
    }

    public void startJoinServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)){
            log.info("Listening for device join requests on port {}.", port);

            while (true) {
                Socket deviceSocket = serverSocket.accept();
                String clientIp = deviceSocket.getInetAddress().getHostAddress();

                log.info("Accepted connection from device IP {}.", clientIp);
                clientHandlerPool.submit(() -> handleDeviceJoinRequest(deviceSocket));
            }
        } catch (IOException e) {
            log.error("Could not listen on port {}.", port, e);
        }
    }

    private void handleDeviceJoinRequest(Socket deviceSocket) {
        activeDeviceTCPConnections.incrementAndGet();

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(deviceSocket.getInputStream()));
            log.info("Device connected: {}", deviceSocket.getInetAddress());

            String joinRequest = in.readLine();
            if (joinRequest != null) {
                processJoinRequest(joinRequest, deviceSocket);
            }

        } catch (IOException e) {
            log.error("Error while handling device join request.", e);
        } finally {
            activeDeviceTCPConnections.decrementAndGet();
        }
    }

    private void processJoinRequest(String joinRequest, Socket deviceSocket) {
        log.info("Received join request: {}", joinRequest);

        try {
            JsonObject parsedRequest = JsonParser.parseString(joinRequest).getAsJsonObject();
            int deviceID = parsedRequest.get("deviceId").getAsInt();
            String deviceType = parsedRequest.get("deviceType").getAsString();
            String deviceModel = parsedRequest.get("deviceModel").getAsString();

            int devicePort = parsedRequest.has("devicePort") ? parsedRequest.get("devicePort").getAsInt() : 5000;

            Device device = new Device(deviceID, null, deviceModel, deviceSocket.getInetAddress().getHostAddress(), deviceType);
            device.setDevicePort(devicePort);

            DeviceSocketWrapper deviceWrapper = new DeviceSocketWrapper(deviceSocket, device);

            activeDeviceSockets.add(deviceWrapper);

            // Check if the device is already registered
            Device existingDevice = deviceService.getDeviceById(deviceID);

            if (existingDevice == null) {
                // New device, awaiting user approval
                messagingTemplate.convertAndSend("/topic/joinRequest", deviceWrapper.getDeviceObj());
                log.info("Device join request sent to user.");
            } else {

                // Device is already registered, update IP address if necessary
                if (!existingDevice.getDeviceIp().equals(device.getDeviceIp())) {
                    existingDevice.setDeviceIp(device.getDeviceIp());
                    deviceService.updateDevice(deviceID, existingDevice);
                    log.info("Device IP address updated.");
                }

                // Send accept message to device
                sendDeviceJoinAcceptResponse(deviceSocket);
                log.info("Device join accept message sent.");

                closeAndRemoveSocket(deviceWrapper);
            }

        } catch (Exception e) {
            log.error("Error processing join request.", e);
        }
    }

    @Transactional
    public String acceptDevice(int deviceID, int fieldID) {
        DeviceSocketWrapper deviceWrapper = findDeviceSocketById(deviceID);

        if (deviceWrapper == null) {
            return "Device not found or already processed.";
        }

        try {
            Device device = deviceWrapper.getDeviceObj();

            Field field = fieldService.getFieldById(fieldID);
            if (field == null) {
                return "Field not found with ID: " + fieldID;
            }

            device.setField(field);
            device.setDeviceStatus(Device.DeviceStatus.ACTIVE);

            // Set default calibration polynomial if it's a sensor
            if (device.isSensor()) {
                device.setDefaultCalibrationPolynomial();
                log.info("Default calibration polynomial set.");

                scheduledSensorDataFetcher.scheduleDeviceTask(device);
                log.info("Scheduled sensor data fetching for device ID: {}", deviceID);
            }

            // Save device to the database
            deviceService.saveDevice(device);
            log.info("Device accepted and assigned to field: {}", fieldID);

            // Send join accept response to the device
            sendDeviceJoinAcceptResponse(deviceWrapper.getSocket());

            closeAndRemoveSocket(deviceWrapper);

            return "Device accepted and assigned to field: " + fieldID;

        } catch (Exception e) {
            log.error("Error while accepting device.", e);
            return "Error while accepting device: " + e.getMessage();
        }
    }

    @Transactional
    public String refuseDevice(int deviceID) {
        DeviceSocketWrapper deviceWrapper = findDeviceSocketById(deviceID);
        if (deviceWrapper == null) {
            return "Device not found or already processed.";
        }

        try {
            sendDeviceJoinRefuseResponse(deviceWrapper.getSocket());
            log.info("Device refused.");

            closeAndRemoveSocket(deviceWrapper);
            return "Device refused.";

        } catch (Exception e) {
            log.error("Error while refusing device.", e);
            return "Error while refusing device: " + e.getMessage();
        }
    }

    private DeviceSocketWrapper findDeviceSocketById(int deviceID) {
        return activeDeviceSockets.stream()
                .filter(wrapper -> wrapper.getDeviceID() == deviceID)
                .findFirst()
                .orElse(null);
    }

    private void closeAndRemoveSocket(DeviceSocketWrapper deviceWrapper) {
        try {
            if (!deviceWrapper.getSocket().isClosed()) {
                deviceWrapper.getSocket().close();
                log.info("Socket closed.");
            }
        } catch (IOException e) {
            log.error("Error while closing socket.", e);
        } finally {
            activeDeviceSockets.remove(deviceWrapper);
            activeDeviceTCPConnections.decrementAndGet();
            log.info("Active device TCP connection count decremented.");
        }
    }

    public void sendDeviceJoinAcceptResponse(Socket deviceSocket) {

        try {
            PrintWriter out = new PrintWriter(deviceSocket.getOutputStream(), true);
            String message = deviceCommandConfigLoader.getDeviceJoinResponse("join_accepted");
            Thread.sleep(1000);

            out.println(message);
            log.info("Join accept message sent to the device");

        } catch (IOException e) {
            log.error("Error while sending join accept response", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendDeviceJoinRefuseResponse(Socket deviceSocket) {

        try {
            PrintWriter out = new PrintWriter(deviceSocket.getOutputStream(), true);
            String message = deviceCommandConfigLoader.getDeviceJoinResponse("join_refused");
            Thread.sleep(1000);

            out.println(message);
            log.info("Join refuse message sent to the device");

        } catch (IOException e) {
            log.error("Error while sending join refuse response", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
