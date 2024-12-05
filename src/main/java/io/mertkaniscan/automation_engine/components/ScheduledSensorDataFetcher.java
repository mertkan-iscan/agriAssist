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

    private final DeviceService deviceService;
    private final SensorDataSocketService sensorDataSocketService;
    private final SensorDataService sensorDataService;

    @Autowired
    public ScheduledSensorDataFetcher(DeviceService deviceService, SensorDataSocketService sensorDataSocketService, SensorDataService sensorDataService){
        this.deviceService = deviceService;
        this.sensorDataSocketService = sensorDataSocketService;
        this.sensorDataService = sensorDataService;
    }

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();


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
        if (device.isSensor()) {
            try {
                List<SensorData> sensorDataList = sensorDataSocketService.fetchSensorData(device.getDeviceID());

                for (SensorData sensorData : sensorDataList) {
                    sensorDataService.saveSensorData(sensorData);
                }

                if (!sensorDataList.isEmpty()) {
                    logger.info("Sensor data fetched and saved for device {}.", device.getDeviceID());

                    if (!"ACTIVE".equals(device.getDeviceStatus())) {
                        device.setDeviceStatus(Device.DeviceStatus.ACTIVE);
                        deviceService.updateDevice(device.getDeviceID(), device);
                    }
                } else {
                    logger.warn("No valid sensor data received from device {}.", device.getDeviceID());
                }

            } catch (Exception e) {
                logger.error("Error fetching sensor data for device {}: {}", device.getDeviceID(), e.getMessage());

                // If the sensor data can't be fetched, mark the device as inactive
                if (!"INACTIVE".equals(device.getDeviceStatus())) {
                    device.setDeviceStatus(Device.DeviceStatus.INACTIVE);
                    deviceService.updateDevice(device.getDeviceID(), device);
                }
            }
        }
    }

    private void cancelExistingTask(int deviceID) {
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
