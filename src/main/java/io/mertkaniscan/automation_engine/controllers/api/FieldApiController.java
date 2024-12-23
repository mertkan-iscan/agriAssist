package io.mertkaniscan.automation_engine.controllers.api;

import io.mertkaniscan.automation_engine.models.*;
import io.mertkaniscan.automation_engine.services.device_services.SensorDataDTO;
import io.mertkaniscan.automation_engine.services.irrigation_services.IrrigationService;
import io.mertkaniscan.automation_engine.services.main_services.DeviceService;
import io.mertkaniscan.automation_engine.services.weather_forecast_services.SolarResponse;
import io.mertkaniscan.automation_engine.services.weather_forecast_services.WeatherResponse;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.services.main_services.PlantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/fields")
@Slf4j
public class FieldApiController {

    private final PlantService plantService;
    private final FieldService fieldService;
    private final DeviceService deviceService;
    private final IrrigationService irrigationService;

    public FieldApiController(PlantService plantService, FieldService fieldService, DeviceService deviceService, IrrigationService irrigationService) {
        this.plantService = plantService;
        this.fieldService = fieldService;
        this.deviceService = deviceService;
        this.irrigationService = irrigationService;
    }

    @PostMapping("/add")
    public ResponseEntity<Field> createField(@RequestBody Field field) {
        Field savedField = fieldService.saveField(field);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedField);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Field>> getAllFields() {
        List<Field> fields = fieldService.getAllFields();
        return ResponseEntity.ok(fields);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Field> getFieldById(@PathVariable int id) {
        Field field = fieldService.getFieldById(id);
        return field != null ? ResponseEntity.ok(field) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/delete/{fieldId}")
    public ResponseEntity<?> deleteField(@PathVariable int fieldId) {
        fieldService.deleteFieldById(fieldId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{fieldID}")
    public ResponseEntity<?> updateField(@PathVariable int fieldID, @RequestBody Field updatedField) {
        Field existingField = fieldService.getFieldById(fieldID);

        if (existingField == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Field not found with ID: " + fieldID);
        }

        existingField.setFieldName(updatedField.getFieldName());
        existingField.setFieldType(updatedField.getFieldType());
        existingField.setFieldSoilType(updatedField.getFieldSoilType());
        // add more

        fieldService.saveField(existingField);
        return ResponseEntity.ok(existingField);
    }

    @PostMapping("/{fieldID}/add-plant")
    public ResponseEntity<?> addPlantToField(@PathVariable int fieldID, @RequestBody Plant plant) {
        // Retrieve the field by ID
        Field field = fieldService.getFieldById(fieldID);

        if (field == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Field not found.");
        }

        // Check if a plant is already assigned to this field
        if (field.getPlantInField() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("A plant is already assigned to this field.");
        }

        // Save the plant
        Plant savedPlant = plantService.savePlant(plant);

        // Update the field with the new plant
        field.setPlantInField(savedPlant);
        fieldService.saveField(field);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedPlant);
    }

    @GetMapping("/{fieldId}/weather-data-daily")
    public ResponseEntity<WeatherResponse> pullDailyWeatherDataByFieldId(@PathVariable int fieldId) {
        try {

            WeatherResponse weatherResponse = fieldService.getWeatherDataByFieldId(fieldId);
            log.info(weatherResponse.toString());
            return ResponseEntity.ok(weatherResponse);

        } catch (Exception e) {
            return ResponseEntity.status(404).body(null);
        }
    }

    @GetMapping("/{fieldId}/solar-data-daily")
    public ResponseEntity<SolarResponse> pullDailySolarDataByFieldId(@PathVariable int fieldId) {
        try {
            // Set the date to the current date in the format YYYY-MM-DD
            LocalDate date = LocalDate.now();

            // Fetch the solar data for the current date
            SolarResponse solarResponse = fieldService.getSolarDataByFieldId(fieldId, date);

            return ResponseEntity.ok(solarResponse);

        } catch (Exception e) {
            return ResponseEntity.status(404).body(null);
        }
    }

    @GetMapping("/{fieldId}/sensor-data")
    public ResponseEntity<?> pullSensorDataValueByModel(@PathVariable int fieldId, @RequestParam String sensorModel) {
        try {
            List<SensorDataDTO> sensorDataList = fieldService.getSensorDataValueByModel(fieldId, sensorModel);

            return ResponseEntity.ok(sensorDataList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No sensor data found for the given model.");
        }
    }

    @PostMapping("/{fieldID}/control-actuator")
    public ResponseEntity<String> controlActuator(
            @PathVariable int fieldID,
            @RequestParam int deviceID,
            @RequestParam double flowRate) {

        try {
            String response = fieldService.controlActuatorByFlowRate(fieldID, deviceID, flowRate);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to control actuator: " + e.getMessage());
        }
    }

    @PostMapping("/{fieldID}/control-actuator-test")
    public ResponseEntity<String> controlActuatorTest(
            @PathVariable int fieldID,
            @RequestParam int deviceID,
            @RequestParam int degree) {

        try {
            String response = fieldService.controlActuator(fieldID, deviceID, degree);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to control actuator: " + e.getMessage());
        }
    }

    @PostMapping("/{fieldID}/calibrate-device")
    public ResponseEntity<String> addCalibration(
            @PathVariable int fieldID,
            @RequestParam int deviceID,
            @RequestParam int degree,
            @RequestParam double flowRate) {
        try {
            deviceService.addCalibration(deviceID, degree, flowRate);
            return ResponseEntity.ok("Calibration data added successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add calibration data: " + e.getMessage());
        }
    }
}