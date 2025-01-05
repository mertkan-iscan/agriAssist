package io.mertkaniscan.automation_engine.services.main_services;

import io.mertkaniscan.automation_engine.components.ScheduledSensorDataFetcher;
import io.mertkaniscan.automation_engine.models.Device;
import io.mertkaniscan.automation_engine.utils.FetchInterval;
import io.mertkaniscan.automation_engine.repositories.DeviceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final ScheduledSensorDataFetcher scheduledSensorDataFetcher;

    @Autowired
    public DeviceService(DeviceRepository deviceRepository, @Lazy ScheduledSensorDataFetcher scheduledSensorDataFetcher) {
        this.deviceRepository = deviceRepository;
        this.scheduledSensorDataFetcher = scheduledSensorDataFetcher;
    }

    public Device saveDevice(Device device) {
        Device savedDevice = deviceRepository.save(device);
        return savedDevice;
    }

    public Device updateDevice(int id, Device device) {

        Device existingDevice = getDeviceById(id);

        if (existingDevice != null) {

            existingDevice.setDeviceModel(device.getDeviceModel());
            existingDevice.setDeviceIp(device.getDeviceIp());
            existingDevice.setDeviceStatus(device.getDeviceStatus());
            existingDevice.setField(device.getField());

            return deviceRepository.save(existingDevice);
        }
        return null;
    }

    public boolean deleteDevice(int id) {
        if (deviceRepository.existsById(id)) {
            scheduledSensorDataFetcher.cancelExistingTask(id);
            deviceRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Device getDeviceById(int deviceID) {
        return deviceRepository.findById(deviceID).orElse(null);
    }

    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    public List<Device> getDevicesByFieldID(int fieldID) {
        return deviceRepository.findByFieldFieldID(fieldID);
    }

    public void updateDeviceFetchInterval(int deviceID, FetchInterval fetchInterval) {

        Device device = deviceRepository.findById(deviceID).orElse(null);

        if (device != null) {
            device.setFetchInterval(fetchInterval);
            deviceRepository.save(device);
        }
    }

    public void addCalibration(int deviceID, int degree, double flowRate) {
        Device device = getDeviceById(deviceID);
        if (device == null) {
            throw new IllegalArgumentException("Device not found with ID: " + deviceID);
        }

        Map<Double, Integer> calibrationMap = device.getCalibrationMap();

        if (calibrationMap == null) {
            calibrationMap = new HashMap<>();
        }

        calibrationMap.put(flowRate, degree);
        device.setCalibrationMap(calibrationMap);

        deviceRepository.save(device);
    }
}
