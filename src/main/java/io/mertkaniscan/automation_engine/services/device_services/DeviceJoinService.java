package io.mertkaniscan.automation_engine.services.device_services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.mertkaniscan.automation_engine.services.main_services.DeviceService;
import io.mertkaniscan.automation_engine.utils.DeviceJsonMessageFactory;
import io.mertkaniscan.automation_engine.utils.DeviceSocketWrapper;
import io.mertkaniscan.automation_engine.models.Device;
import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.utils.config_loader.DeviceCommandConfigLoader;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
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

@Service
public class DeviceJoinService {

    private static final Logger logger = LogManager.getLogger(DeviceJoinService.class);

    private final ExecutorService clientHandlerPool = Executors.newCachedThreadPool();
    private final AtomicInteger activeDeviceTCPConnections = new AtomicInteger(0);
    private final List<DeviceSocketWrapper> activeDeviceSockets = new CopyOnWriteArrayList<>();

    private final DeviceService deviceService;
    private final FieldService fieldService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public DeviceJoinService(DeviceService deviceService, FieldService fieldService, SimpMessagingTemplate messagingTemplate) {
        this.deviceService = deviceService;
        this.fieldService = fieldService;
        this.messagingTemplate = messagingTemplate;
    }

    public void startJoinServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)){
            logger.info("Listening for device join requests on port {}.", port);

            while (true) {
                Socket deviceSocket = serverSocket.accept();
                String clientIp = deviceSocket.getInetAddress().getHostAddress();

                logger.info("Accepted connection from device IP {}.", clientIp);
                clientHandlerPool.submit(() -> handleDeviceJoinRequest(deviceSocket));
            }
        } catch (IOException e) {
            logger.error("Could not listen on port {}.", port, e);
        }
    }

    private void handleDeviceJoinRequest(Socket deviceSocket) {
        activeDeviceTCPConnections.incrementAndGet();

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(deviceSocket.getInputStream()));
            logger.info("Device connected: {}", deviceSocket.getInetAddress());

            String joinRequest = in.readLine();
            if (joinRequest != null) {
                processJoinRequest(joinRequest, deviceSocket);
            }

        } catch (IOException e) {
            logger.error("Error while handling device join request.", e);
        } finally {
            activeDeviceTCPConnections.decrementAndGet();
        }
    }

    private void processJoinRequest(String joinRequest, Socket deviceSocket) {
        logger.info("Received join request: {}", joinRequest);

        try {
            JsonObject parsedRequest = JsonParser.parseString(joinRequest).getAsJsonObject();
            int deviceID = parsedRequest.get("deviceId").getAsInt();
            String deviceType = parsedRequest.get("deviceType").getAsString();
            String deviceModel = parsedRequest.get("deviceModel").getAsString();

            Device device = new Device(deviceID, null, deviceModel, deviceSocket.getInetAddress().getHostAddress(), deviceType);
            DeviceSocketWrapper deviceWrapper = new DeviceSocketWrapper(deviceSocket, device);

            activeDeviceSockets.add(deviceWrapper);

            // Check if the device is already registered
            Device existingDevice = deviceService.getDeviceById(deviceID);

            if (existingDevice == null) {
                // New device, awaiting user approval
                messagingTemplate.convertAndSend("/topic/joinRequest", deviceWrapper.getDeviceObj());
                logger.info("Device join request sent to user.");
            } else {

                // Device is already registered, update IP address if necessary
                if (!existingDevice.getDeviceIp().equals(device.getDeviceIp())) {
                    existingDevice.setDeviceIp(device.getDeviceIp());
                    deviceService.updateDevice(deviceID, existingDevice);
                    logger.info("Device IP address updated.");
                }

                // Send accept message to device
                sendDeviceJoinAcceptResponse(deviceSocket);
                logger.info("Device join accept message sent.");

                closeAndRemoveSocket(deviceWrapper);
            }

        } catch (Exception e) {
            logger.error("Error processing join request.", e);
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

            sendDeviceJoinAcceptResponse(deviceWrapper.getSocket());

            deviceService.saveDevice(device);
            logger.info("Device accepted and assigned to field: {}", fieldID);

            closeAndRemoveSocket(deviceWrapper);
            return "Device accepted and assigned to field: " + fieldID;
        } catch (Exception e) {
            logger.error("Error while accepting device.", e);
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
            logger.info("Device refused.");

            closeAndRemoveSocket(deviceWrapper);
            return "Device refused.";

        } catch (Exception e) {
            logger.error("Error while refusing device.", e);
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
                logger.info("Socket closed.");
            }
        } catch (IOException e) {
            logger.error("Error while closing socket.", e);
        } finally {
            activeDeviceSockets.remove(deviceWrapper);
            activeDeviceTCPConnections.decrementAndGet();
            logger.info("Active device TCP connection count decremented.");
        }
    }

    public static void sendDeviceJoinAcceptResponse(Socket deviceSocket) {

        try {
            PrintWriter out = new PrintWriter(deviceSocket.getOutputStream(), true);
            String message = DeviceCommandConfigLoader.getDeviceJoinResponse("join_accepted");
            Thread.sleep(1000);

            out.println(message);
            logger.info("Join accept message sent to the device");

        } catch (IOException e) {
            logger.error("Error while sending join accept response", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendDeviceJoinRefuseResponse(Socket deviceSocket) {

        try {
            PrintWriter out = new PrintWriter(deviceSocket.getOutputStream(), true);
            String message = DeviceCommandConfigLoader.getDeviceJoinResponse("join_refused");
            Thread.sleep(1000);

            out.println(message);
            logger.info("Join refuse message sent to the device");

        } catch (IOException e) {
            logger.error("Error while sending join refuse response", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
