package io.mertkaniscan.automation_engine.services.main_services;

import io.mertkaniscan.automation_engine.utils.config_loader.ConfigLoader;
import io.mertkaniscan.automation_engine.utils.config_loader.FieldConfig;
import io.mertkaniscan.automation_engine.models.*;
import io.mertkaniscan.automation_engine.services.device_services.SensorDataDTO;
import io.mertkaniscan.automation_engine.services.weather_forecast_services.SolarResponse;
import io.mertkaniscan.automation_engine.services.weather_forecast_services.weather_response_obj.WeatherResponse;
import io.mertkaniscan.automation_engine.repositories.FieldRepository;
import io.mertkaniscan.automation_engine.services.weather_forecast_services.WeatherForecastService;
import io.mertkaniscan.automation_engine.services.device_services.SensorDataSocketService;
import io.mertkaniscan.automation_engine.services.device_services.ActuatorCommandSocketService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FieldService {

    private final FieldRepository fieldRepository;
    private final DeviceService deviceService;
    private final SensorDataSocketService sensorDataSocketService;
    private final WeatherForecastService weatherForecastService;
    private final ConfigLoader configLoader;
    private final ActuatorCommandSocketService actuatorCommandSocketService;

    public FieldService(FieldRepository fieldRepository, DeviceService deviceService, SensorDataSocketService sensorDataSocketService, WeatherForecastService weatherForecastService, ConfigLoader configLoader, ActuatorCommandSocketService actuatorCommandSocketService) {
        this.fieldRepository = fieldRepository;
        this.deviceService = deviceService;
        this.sensorDataSocketService = sensorDataSocketService;
        this.weatherForecastService = weatherForecastService;
        this.configLoader = configLoader;
        this.actuatorCommandSocketService = actuatorCommandSocketService;
    }

    public Field saveField(Field field) {

        FieldConfig fieldConfig = configLoader.getFieldConfigs().stream()
                .filter(config -> config.getSoilType().equalsIgnoreCase(field.getFieldSoilType().toString()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Soil type not found in configuration: " + field.getFieldSoilType()));

        field.setFieldCapacity(fieldConfig.getFieldCapacity());
        field.setWiltingPoint(fieldConfig.getWiltingPoint());
        field.setBulkDensity(fieldConfig.getBulkDensity());
        field.setSaturation(fieldConfig.getSaturation());
        field.setInfiltrationRate(fieldConfig.getInfiltrationRate());

        return fieldRepository.save(field);
    }

    public List<Field> getAllFields() {
        return fieldRepository.findAll();
    }

    public Field getFieldById(int fieldID) {
        return fieldRepository.findById(fieldID).orElse(null);
    }

    public List<Device> getDevicesByFieldId(int fieldID) {
        return deviceService.getDevicesByFieldID(fieldID);
    }

    public void deleteFieldById(int fieldId) {
        fieldRepository.deleteById(fieldId);
    }

    public WeatherResponse getWeatherDataByFieldId(int fieldID) {
        Field field = getFieldById(fieldID);
        return weatherForecastService.getAndParseWeatherData(field.getLatitude(), field.getLongitude());
    }

    public SolarResponse getSolarDataByFieldId(int fieldID, LocalDate date) {
        Field field = getFieldById(fieldID);
        return weatherForecastService.getAndParseSolarData(field.getLatitude(), field.getLongitude(), date);
    }

    public Field updateFieldWithPlant(int fieldID, Plant plant) {
        Field field = getFieldById(fieldID);
        field.setPlantInField(plant);
        return fieldRepository.save(field);
    }

    public List<SensorDataDTO> getSensorDataValueByModel(int fieldId, String sensorModel) {

        List<Device> devices = getDevicesByFieldId(fieldId);

        return devices.stream()
                .filter(device -> device.getDeviceModel().equalsIgnoreCase(sensorModel))
                .map(device -> {
                    try {
                        return sensorDataSocketService.fetchSensorDataValue(device.getDeviceID());
                    } catch (Exception e) {
                        throw new RuntimeException("Error fetching sensor data for device ID: " + device.getDeviceID(), e);
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public String controlActuator(int fieldId, int deviceId, int degree) {
        Field field = getFieldById(fieldId);

        if (field == null) {
            throw new IllegalArgumentException("Field with ID " + fieldId + " not found.");
        }

        Device device = deviceService.getDeviceById(deviceId);

        if (device == null || !field.getDevices().contains(device)) {
            throw new IllegalArgumentException("Device with ID " + deviceId + " not found in field with ID " + fieldId);
        }

        try {
            return actuatorCommandSocketService.sendActuatorCommand(deviceId, degree);
        } catch (Exception e) {
            throw new RuntimeException("Failed to control actuator with ID " + deviceId + ": " + e.getMessage(), e);
        }
    }

    public String controlActuatorByFlowRate(int fieldId, int deviceId, double flowRate) {
        Field field = getFieldById(fieldId);

        if (field == null) {
            throw new IllegalArgumentException("Field with ID " + fieldId + " not found.");
        }

        Device device = deviceService.getDeviceById(deviceId);

        if (device == null || !field.getDevices().contains(device)) {
            throw new IllegalArgumentException("Device with ID " + deviceId + " not found in field with ID " + fieldId);
        }

        // Kalibrasyon verilerini al
        Map<Double, Integer> calibrationMap = device.getCalibrationMap();
        Integer degree = calibrationMap.get(flowRate);

        if (degree == null) {
            throw new IllegalArgumentException("Flow rate " + flowRate + " not found in device calibration data.");
        }

        try {
            return actuatorCommandSocketService.sendActuatorCommand(deviceId, degree);
        } catch (Exception e) {
            throw new RuntimeException("Failed to control actuator with ID " + deviceId + ": " + e.getMessage(), e);
        }
    }

    public void calibrateDevice(int fieldID, int deviceID, int degree, double flowRate) {
        Field field = getFieldById(fieldID);
        if (field == null) {
            throw new IllegalArgumentException("Field with ID " + fieldID + " not found.");
        }

        Device device = deviceService.getDeviceById(deviceID);
        if (device == null || !field.getDevices().contains(device)) {
            throw new IllegalArgumentException("Device with ID " + deviceID + " not found in field with ID " + fieldID);
        }

        Map<Double, Integer> calibrationMap = device.getCalibrationMap();
        calibrationMap.put(flowRate, degree);
        device.setCalibrationMap(calibrationMap);

        deviceService.saveDevice(device);
    }
}
