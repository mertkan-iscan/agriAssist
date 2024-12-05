package io.mertkaniscan.automation_engine.controllers.api;

import io.mertkaniscan.automation_engine.models.SensorData;
import io.mertkaniscan.automation_engine.services.main_services.SensorDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensor-data")
public class SensorDataApiController {

    private final SensorDataService sensorDataService;

    public SensorDataApiController(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    @GetMapping
    public ResponseEntity<List<SensorData>> getAllSensorData() {
        List<SensorData> data = sensorDataService.getAllSensorData();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SensorData> getSensorDataById(@PathVariable int id) {
        SensorData sensorData = sensorDataService.getSensorDataById(id);
        return sensorData != null ? ResponseEntity.ok(sensorData) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<SensorData> createSensorData(@RequestBody SensorData sensorData) {
        SensorData savedData = sensorDataService.saveSensorData(sensorData);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedData);
    }

    @DeleteMapping("/delete/{sensorDataId}")
    public ResponseEntity<?> deleteSensorData(@PathVariable int sensorDataId) {
        sensorDataService.deleteSensorData(sensorDataId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{fieldID}/{dataType}")
    public ResponseEntity<List<SensorData>> getSensorDataByFieldIDAndTypeFromDb(
            @PathVariable int fieldID,
            @PathVariable String dataType,
            @RequestParam(required = false) String timeRange) {

        List<SensorData> data;

        // Determine the time range filter
        if (timeRange != null) {
            switch (timeRange.toLowerCase()) {
                case "day":
                    data = sensorDataService.getSensorDataByFieldIDAndTypeWithinLastDaysFromDb(fieldID, dataType, 1);
                    break;
                case "week":
                    data = sensorDataService.getSensorDataByFieldIDAndTypeWithinLastDaysFromDb(fieldID, dataType, 7);
                    break;
                case "month":
                    data = sensorDataService.getSensorDataByFieldIDAndTypeWithinLastDaysFromDb(fieldID, dataType, 30);
                    break;
                case "year":
                    data = sensorDataService.getSensorDataByFieldIDAndTypeWithinLastDaysFromDb(fieldID, dataType, 365);
                    break;
                default:
                    data = sensorDataService.getSensorDataByFieldIDAndTypeFromDb(fieldID, dataType);
            }
        } else {
            // Default behavior if no time range is specified
            data = sensorDataService.getSensorDataByFieldIDAndTypeFromDb(fieldID, dataType);
        }

        if (data != null && !data.isEmpty()) {
            return ResponseEntity.ok(data);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}