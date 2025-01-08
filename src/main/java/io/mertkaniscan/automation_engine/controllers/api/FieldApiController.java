package io.mertkaniscan.automation_engine.controllers.api;

import io.mertkaniscan.automation_engine.models.*;
import io.mertkaniscan.automation_engine.services.device_services.SensorDataDTO;
import io.mertkaniscan.automation_engine.services.irrigation_services.IrrigationService;
import io.mertkaniscan.automation_engine.services.main_services.DeviceService;
import io.mertkaniscan.automation_engine.services.forecast_services.solar_forecast_service.SolarResponse;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherResponse;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.services.main_services.PlantService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @GetMapping("/types")
    public List<String> getFieldTypes() {
        return Stream.of(Field.FieldType.values())
                .map(Enum::name)
                .collect(Collectors.toList());
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

        fieldService.saveField(existingField);
        return ResponseEntity.ok(existingField);
    }

    @PostMapping("/{fieldID}/add-plant")
    public ResponseEntity<?> addPlantToField(@PathVariable int fieldID, @RequestBody @Valid Plant plant) {

        // Fetch the field by ID
        Field field = fieldService.getFieldById(fieldID);

        // Check if the field exists
        if (field == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Field not found.");
        }

        // Check if a plant is already assigned to the field
        if (field.getPlantInField() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("A plant is already assigned to this field.");
        }

        // Set bidirectional relationship
        plant.setField(field);
        field.setPlantInField(plant);

        // Save the plant, which cascades and persists the field if configured
        Plant savedPlant = plantService.savePlant(plant);

        // Return the saved plant in the response
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

            LocalDate date = LocalDate.now();

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
}