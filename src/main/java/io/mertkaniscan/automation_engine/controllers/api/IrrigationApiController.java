package io.mertkaniscan.automation_engine.controllers.api;

import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.models.IrrigationRequest;
import io.mertkaniscan.automation_engine.services.irrigation_services.IrrigationService;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/irrigation")
public class IrrigationApiController {

    @Autowired
    private IrrigationService irrigationService;

    @Autowired
    private FieldService fieldService;

    @PostMapping("/{fieldId}/schedule")
    public ResponseEntity<String> scheduleIrrigation(@PathVariable int fieldId, @RequestBody IrrigationRequest request) {
        try {
            // Retrieve the field by ID
            Field field = fieldService.getFieldById(fieldId);

            if (field == null) {
                return ResponseEntity.badRequest().body("Field not found with ID: " + fieldId);
            }

            // Attach the field to the irrigation request
            request.setField(field);

            // Schedule the irrigation
            irrigationService.scheduleIrrigation(request);
            return ResponseEntity.ok("Irrigation scheduled successfully for field ID: " + fieldId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error scheduling irrigation: " + e.getMessage());
        }
    }

    @GetMapping("/{fieldId}/history")
    public ResponseEntity<List<IrrigationRequest>> getIrrigationHistory(@PathVariable int fieldId) {
        try {
            List<IrrigationRequest> history = irrigationService.getIrrigationHistory(fieldId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<String> editIrrigationRequest(@PathVariable int id, @RequestBody IrrigationRequest updatedRequest) {
        try {
            irrigationService.editIrrigationRequest(id, updatedRequest);
            return ResponseEntity.ok("Irrigation request updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating irrigation request: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> cancelIrrigationRequest(@PathVariable int id) {
        try {
            irrigationService.cancelIrrigation(id);
            return ResponseEntity.ok("Irrigation request canceled successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error canceling irrigation request: " + e.getMessage());
        }
    }

    @PostMapping("/start-now/{id}")
    public ResponseEntity<String> startIrrigationNow(@PathVariable int id) {
        try {
            IrrigationRequest request = irrigationService.getIrrigationRequestById(id);
            if (request == null) {
                return ResponseEntity.badRequest().body("Irrigation request not found.");
            }
            irrigationService.startIrrigation(request);
            return ResponseEntity.ok("Irrigation started immediately.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error starting irrigation: " + e.getMessage());
        }
    }
}
