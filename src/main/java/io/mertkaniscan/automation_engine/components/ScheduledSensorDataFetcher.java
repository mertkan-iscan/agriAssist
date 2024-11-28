package io.mertkaniscan.automation_engine.components;

import io.mertkaniscan.automation_engine.models.Device;
import io.mertkaniscan.automation_engine.models.SensorData;

import io.mertkaniscan.automation_engine.services.main_services.DeviceService;
import io.mertkaniscan.automation_engine.services.main_services.SensorDataService;
import io.mertkaniscan.automation_engine.services.device_services.SensorDataSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class ScheduledSensorDataFetcher {

    private static final Logger logger = LogManager.getLogger(ScheduledSensorDataFetcher.class);

    @Autowired
    @Lazy
    private DeviceService deviceService;

    @Autowired
    private SensorDataSocketService sensorDataSocketService;

    @Autowired
    private SensorDataService sensorDataService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();


    public void initializeDeviceTasks() {
        List<Device> devices = deviceService.getAllDevices();
        for (Device device : devices) {
            scheduleDeviceTask(device);
        }
    }

    // Schedule fetching task for each device based on its interval
    public void scheduleDeviceTask(Device device) {
        FetchInterval interval = device.getFetchInterval() != null ? device.getFetchInterval() : FetchInterval.ONE_MINUTE; // Default to 1 minute if not set

        // Cancel the existing task if it exists
        cancelExistingTask(device.getDeviceID());

        // Schedule the new task
        ScheduledFuture<?> scheduledTask = scheduler.scheduleAtFixedRate(() -> fetchSensorDataForDevice(device), 0, interval.toMilliseconds(), TimeUnit.MILLISECONDS);
        scheduledTasks.put(device.getDeviceID(), scheduledTask);
    }

    // Method to fetch sensor data for a specific device
    private void fetchSensorDataForDevice(Device device) {
        if (device.isSensor()) {
            try {
                List<SensorData> sensorDataList = sensorDataSocketService.fetchSensorData(device.getDeviceID());

                // Save all valid SensorData objects
                for (SensorData sensorData : sensorDataList) {
                    sensorDataService.saveSensorData(sensorData);
                }

                // If data is successfully fetched, mark the device as Active (if not already)
                if (!sensorDataList.isEmpty()) {
                    logger.info("Sensor data fetched and saved for device {}.", device.getDeviceID());

                    if (!"ACTIVE".equals(device.getDeviceStatus())) {
                        device.setDeviceStatus(Device.DeviceStatus.ACTIVE);
                        deviceService.updateDevice(device.getDeviceID(), device);  // Update device status in DB
                    }
                } else {
                    logger.warn("No valid sensor data received from device {}.", device.getDeviceID());
                }

            } catch (Exception e) {
                logger.error("Error fetching sensor data for device {}: {}", device.getDeviceID(), e.getMessage());

                // If the sensor data can't be fetched, mark the device as inactive
                if (!"INACTIVE".equals(device.getDeviceStatus())) {
                    device.setDeviceStatus(Device.DeviceStatus.INACTIVE);
                    deviceService.updateDevice(device.getDeviceID(), device);  // Update device status in DB
                }
            }
        }
    }

    // Method to cancel an existing task for a device
    private void cancelExistingTask(int deviceID) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(deviceID);
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
            logger.info("Cancelled existing task for device {}.", deviceID);
        }
    }

    // Reschedule the fetching task when the interval is updated
    public void rescheduleDeviceTask(int deviceID, FetchInterval newInterval) {
        // Fetch the device and update its interval
        Device device = deviceService.getDeviceById(deviceID);
        if (device != null) {
            device.setFetchInterval(newInterval);
            scheduleDeviceTask(device); // Reschedule the task with the new interval
        }
    }
}
