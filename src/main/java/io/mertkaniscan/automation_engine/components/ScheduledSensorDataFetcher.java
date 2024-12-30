package io.mertkaniscan.automation_engine.components;

import io.mertkaniscan.automation_engine.models.Device;
import io.mertkaniscan.automation_engine.models.SensorData;

import io.mertkaniscan.automation_engine.services.main_services.DeviceService;
import io.mertkaniscan.automation_engine.services.main_services.SensorDataService;
import io.mertkaniscan.automation_engine.services.device_services.SensorDataSocketService;
import io.mertkaniscan.automation_engine.services.notification_services.EmailNotificationService;
import io.mertkaniscan.automation_engine.utils.FetchInterval;
import org.hibernate.validator.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class ScheduledSensorDataFetcher {

    private static final Logger logger = LogManager.getLogger(ScheduledSensorDataFetcher.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private final DeviceService deviceService;
    private final SensorDataSocketService sensorDataSocketService;
    private final SensorDataService sensorDataService;
    private final EmailNotificationService emailNotificationService;

    @Autowired
    public ScheduledSensorDataFetcher(DeviceService deviceService, SensorDataSocketService sensorDataSocketService, SensorDataService sensorDataService, EmailNotificationService emailNotificationService){
        this.deviceService = deviceService;
        this.sensorDataSocketService = sensorDataSocketService;
        this.sensorDataService = sensorDataService;
        this.emailNotificationService = emailNotificationService;
    }

    public void initializeDeviceTasks() {
        List<Device> devices = deviceService.getAllDevices()
                .stream()
                .filter(Device::isSensor)
                .toList();

        for (Device device : devices) {
            scheduleDeviceTask(device);
        }
    }

    public void scheduleDeviceTask(Device device) {
        if (!device.isSensor()) {
            logger.warn("Attempted to schedule task for non-sensor device. Device ID: {}, Type: {}", device.getDeviceID(), device.getDeviceType());
            return;
        }

        FetchInterval interval = device.getFetchInterval() != null ? device.getFetchInterval() : FetchInterval.ONE_MINUTE;
        cancelExistingTask(device.getDeviceID());

        ScheduledFuture<?> scheduledTask = scheduler.scheduleAtFixedRate(
                () -> fetchSensorDataForDevice(device), 0, interval.toMilliseconds(), TimeUnit.MILLISECONDS
        );
        scheduledTasks.put(device.getDeviceID(), scheduledTask);
    }


    private void fetchSensorDataForDevice(Device device) {
        if (!device.isSensor()) {
            logger.warn("Attempted to fetch sensor data for a non-sensor device. Device ID: {}, Type: {}", device.getDeviceID(), device.getDeviceType());
            return;
        }

        final int MAX_RETRIES = 3; // Define the maximum number of retries
        final long RETRY_DELAY_MS = 2000; // Define the delay between retries in milliseconds
        int attempt = 0;
        boolean success = false;

        while (attempt < MAX_RETRIES && !success) {
            attempt++;
            try {
                logger.info("Attempt {} to fetch data for device. ID: {}, IP: {}, Model: {}, Type: {}",
                        attempt, device.getDeviceID(), device.getDeviceIp(), device.getDeviceModel(), device.getDeviceType());

                List<SensorData> sensorDataList = sensorDataSocketService.fetchSensorData(device.getDeviceID());
                logger.debug("Fetched {} records for device ID: {}", sensorDataList.size(), device.getDeviceID());

                for (SensorData sensorData : sensorDataList) {
                    logger.debug("Saving sensor data for device ID: {}, Data: {}", device.getDeviceID(), sensorData);
                    sensorDataService.saveSensorData(sensorData);
                }

                if (!sensorDataList.isEmpty()) {
                    logger.info("Successfully fetched and saved {} sensor data records for device ID: {}.", sensorDataList.size(), device.getDeviceID());

                    if (!Device.DeviceStatus.ACTIVE.equals(device.getDeviceStatus())) {
                        logger.info("Updating device status to ACTIVE. Device ID: {}, Previous Status: {}", device.getDeviceID(), device.getDeviceStatus());
                        device.setDeviceStatus(Device.DeviceStatus.ACTIVE);
                        deviceService.updateDevice(device.getDeviceID(), device);
                    }
                    success = true; // Mark the fetch as successful to exit the loop
                } else {
                    logger.warn("No valid sensor data received for device ID: {}, IP: {}, Model: {}.", device.getDeviceID(), device.getDeviceIp(), device.getDeviceModel());
                    success = true; // Avoid retries for empty data
                }

            } catch (Exception e) {
                logger.error("Attempt {} failed. Error fetching sensor data for device ID: {}, IP: {}, Model: {}. Error: {}",
                        attempt, device.getDeviceID(), device.getDeviceIp(), device.getDeviceModel(), e.getMessage(), e);

                emailNotificationService.sendEmail("Sensor Data Fetch Error", String.format("Failed to fetch data for device ID: %d, IP: %s, Model: %s.\nError: %s",
                        device.getDeviceID(), device.getDeviceIp(), device.getDeviceModel(), e.getMessage()));

                if (attempt == MAX_RETRIES) {
                    logger.error("Exceeded maximum retries ({}) for device ID: {}. Marking device as INACTIVE.", MAX_RETRIES, device.getDeviceID());
                    if (!Device.DeviceStatus.INACTIVE.equals(device.getDeviceStatus())) {
                        device.setDeviceStatus(Device.DeviceStatus.INACTIVE);
                        deviceService.updateDevice(device.getDeviceID(), device);
                    }
                } else {
                    logger.info("Retrying fetch for device ID: {} (Attempt {}/{}). Waiting {} ms before retry.", device.getDeviceID(), attempt + 1, MAX_RETRIES, RETRY_DELAY_MS);
                    try {
                        Thread.sleep(RETRY_DELAY_MS); // Introduce a delay before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); // Restore interrupt status
                        logger.error("Retry interrupted for device ID: {}. Exiting retries.", device.getDeviceID());
                        break;
                    }
                }
            }
        }
    }



    public void cancelExistingTask(int deviceID) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(deviceID);
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
            logger.info("Cancelled existing task for device {}.", deviceID);
        }
    }

    public void rescheduleDeviceTask(int deviceID, FetchInterval newInterval) {
        Device device = deviceService.getDeviceById(deviceID);
        if (device == null || !device.isSensor()) {
            logger.warn("Attempted to reschedule task for non-sensor device or invalid device ID. Device ID: {}", deviceID);
            return;
        }

        device.setFetchInterval(newInterval);
        scheduleDeviceTask(device);
    }

}
