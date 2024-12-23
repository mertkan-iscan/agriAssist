package io.mertkaniscan.automation_engine.components;

import io.mertkaniscan.automation_engine.models.Device;
import io.mertkaniscan.automation_engine.models.SensorData;

import io.mertkaniscan.automation_engine.services.main_services.DeviceService;
import io.mertkaniscan.automation_engine.services.main_services.SensorDataService;
import io.mertkaniscan.automation_engine.services.device_services.SensorDataSocketService;
import io.mertkaniscan.automation_engine.utils.FetchInterval;
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

    @Autowired
    public ScheduledSensorDataFetcher(DeviceService deviceService, SensorDataSocketService sensorDataSocketService, SensorDataService sensorDataService){
        this.deviceService = deviceService;
        this.sensorDataSocketService = sensorDataSocketService;
        this.sensorDataService = sensorDataService;
    }

    public void initializeDeviceTasks() {
        List<Device> devices = deviceService.getAllDevices();
        for (Device device : devices) {
            scheduleDeviceTask(device);
        }
    }

    public void scheduleDeviceTask(Device device) {
        FetchInterval interval = device.getFetchInterval() != null ? device.getFetchInterval() : FetchInterval.ONE_MINUTE;

        cancelExistingTask(device.getDeviceID());

        ScheduledFuture<?> scheduledTask = scheduler.scheduleAtFixedRate(() -> fetchSensorDataForDevice(device), 0, interval.toMilliseconds(), TimeUnit.MILLISECONDS);
        scheduledTasks.put(device.getDeviceID(), scheduledTask);
    }

    private void fetchSensorDataForDevice(Device device) {
        if (!device.isSensor()) {
            logger.warn("Attempted to fetch sensor data for a non-sensor device. Device ID: {}, Type: {}", device.getDeviceID(), device.getDeviceType());
            return;
        }

        try {
            logger.info("Starting data fetch for device. ID: {}, IP: {}, Model: {}, Type: {}",
                    device.getDeviceID(), device.getDeviceIp(), device.getDeviceModel(), device.getDeviceType());

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
            } else {
                logger.warn("No valid sensor data received for device ID: {}, IP: {}, Model: {}.", device.getDeviceID(), device.getDeviceIp(), device.getDeviceModel());
            }

        } catch (Exception e) {
            logger.error("Error fetching sensor data for device ID: {}, IP: {}, Model: {}. Error: {}",
                    device.getDeviceID(), device.getDeviceIp(), device.getDeviceModel(), e.getMessage(), e);

            if (!Device.DeviceStatus.INACTIVE.equals(device.getDeviceStatus())) {
                logger.warn("Marking device as INACTIVE due to error. Device ID: {}, Previous Status: {}", device.getDeviceID(), device.getDeviceStatus());
                device.setDeviceStatus(Device.DeviceStatus.INACTIVE);
                deviceService.updateDevice(device.getDeviceID(), device);
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
        // Fetch the device and update its interval
        Device device = deviceService.getDeviceById(deviceID);
        if (device != null) {
            device.setFetchInterval(newInterval);
            scheduleDeviceTask(device);
        }
    }
}
