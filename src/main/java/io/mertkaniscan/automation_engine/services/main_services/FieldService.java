package io.mertkaniscan.automation_engine.services.main_services;

import io.mertkaniscan.automation_engine.services.forecast_services.elevation_service.ElevationService;
import io.mertkaniscan.automation_engine.services.forecast_services.solar_forecast_service.SolarForecastService;
import io.mertkaniscan.automation_engine.services.forecast_services.solar_forecast_service.SolarResponse;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherForecastService;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherResponse;
import io.mertkaniscan.automation_engine.utils.config_loader.ConfigLoader;
import io.mertkaniscan.automation_engine.utils.config_loader.FieldConfig;
import io.mertkaniscan.automation_engine.models.*;
import io.mertkaniscan.automation_engine.services.device_services.SensorDataDTO;
import io.mertkaniscan.automation_engine.repositories.FieldRepository;
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
    private final ElevationService elevationService;
    private final ConfigLoader configLoader;
    private final ActuatorCommandSocketService actuatorCommandSocketService;
    private final SolarForecastService solarForecastService;

    public FieldService(FieldRepository fieldRepository,
                        DeviceService deviceService,
                        SensorDataSocketService sensorDataSocketService,
                        WeatherForecastService weatherForecastService,
                        ElevationService elevationService,
                        ConfigLoader configLoader,
                        ActuatorCommandSocketService actuatorCommandSocketService,
                        SolarForecastService solarForecastService) {
        this.fieldRepository = fieldRepository;
        this.deviceService = deviceService;
        this.sensorDataSocketService = sensorDataSocketService;
        this.weatherForecastService = weatherForecastService;
        this.elevationService = elevationService;
        this.configLoader = configLoader;
        this.actuatorCommandSocketService = actuatorCommandSocketService;
        this.solarForecastService = solarForecastService;
    }

    public Field saveField(Field field) {

        FieldConfig fieldConfig = configLoader.getFieldConfigs().stream()
                .filter(config -> config.getSoilType().equalsIgnoreCase(field.getFieldSoilType().toString()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Soil type not found in configuration: " + field.getFieldSoilType()));


        field.setElevation(
                elevationService.getElevation(
                        field.getLatitude(), field.getLongitude()
                ).getFirstElevation()
        );


        field.setEvaporationCoeff(fieldConfig.getEvaporationCoeff());
        field.setMaxEvaporationDepth(fieldConfig.getMaxEvaporationDepth());
        field.setFieldCapacity(fieldConfig.getFieldCapacity());
        field.setWiltingPoint(fieldConfig.getWiltingPoint());
        field.setBulkDensity(fieldConfig.getBulkDensity());
        field.setSaturation(fieldConfig.getSaturation());
        field.setInfiltrationRate(fieldConfig.getInfiltrationRate());

        FieldCurrentValues newCurrentValues = new FieldCurrentValues();
        newCurrentValues.setField(field);
        field.setCurrentValues(newCurrentValues);

        return fieldRepository.save(field);
    }

    public Field updateField(int fieldId, Field updatedField) {
        // Fetch the existing Field from the database
        Field existingField = getFieldById(fieldId);

        if (existingField == null) {
            throw new IllegalArgumentException("Field with ID " + fieldId + " not found.");
        }

        // Update basic attributes
        existingField.setFieldName(updatedField.getFieldName());
        existingField.setLatitude(updatedField.getLatitude());
        existingField.setLongitude(updatedField.getLongitude());
        existingField.setFieldSoilType(updatedField.getFieldSoilType());

        // Update Field-specific configurations if necessary
        FieldConfig fieldConfig = configLoader.getFieldConfigs().stream()
                .filter(config -> config.getSoilType().equalsIgnoreCase(updatedField.getFieldSoilType().toString()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Soil type not found in configuration: " + updatedField.getFieldSoilType()));

        existingField.setEvaporationCoeff(fieldConfig.getEvaporationCoeff());
        existingField.setMaxEvaporationDepth(fieldConfig.getMaxEvaporationDepth());
        existingField.setFieldCapacity(fieldConfig.getFieldCapacity());
        existingField.setWiltingPoint(fieldConfig.getWiltingPoint());
        existingField.setBulkDensity(fieldConfig.getBulkDensity());
        existingField.setSaturation(fieldConfig.getSaturation());
        existingField.setInfiltrationRate(fieldConfig.getInfiltrationRate());
        existingField.setElevation(elevationService.getElevation(updatedField.getLatitude(), updatedField.getLongitude()).getFirstElevation());

        // Update FieldCurrentValues if applicable
        if (existingField.getCurrentValues() != null) {
            FieldCurrentValues currentValues = existingField.getCurrentValues();
            currentValues.setKeValue(updatedField.getCurrentValues().getKeValue());
            currentValues.setTewValue(updatedField.getCurrentValues().getTewValue());
            currentValues.setRewValue(updatedField.getCurrentValues().getRewValue());
            // Update other fields in FieldCurrentValues as necessary
        }

        // Save and return the updated Field
        return fieldRepository.save(existingField);
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
        return weatherForecastService.getWeatherDataObj(field.getLatitude(), field.getLongitude());
    }

    public SolarResponse getSolarDataByFieldId(int fieldID, LocalDate date) {
        Field field = getFieldById(fieldID);
        return solarForecastService.getSolarData(field.getLatitude(), field.getLongitude(), date);
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
}
