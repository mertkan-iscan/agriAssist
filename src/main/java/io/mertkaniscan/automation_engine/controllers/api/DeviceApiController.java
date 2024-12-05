package io.mertkaniscan.automation_engine.controllers.api;


import io.mertkaniscan.automation_engine.components.FetchInterval;
import io.mertkaniscan.automation_engine.components.ScheduledSensorDataFetcher;
import io.mertkaniscan.automation_engine.services.device_services.DeviceJoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.mertkaniscan.automation_engine.models.Device;
import io.mertkaniscan.automation_engine.services.main_services.DeviceService;

import java.util.List;

@CrossOrigin(origins = "http://192.168.31.191:443")
@RestController
@RequestMapping("/api/devices")
public class DeviceApiController {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceJoinService deviceJoinService;

    @Autowired
    private ScheduledSensorDataFetcher scheduledSensorDataFetcher;


    @PostMapping("/{deviceID}/join-request")
    public ResponseEntity<String> handleJoinRequest(
            @PathVariable int deviceID,
            @RequestParam(required = false) Integer fieldID,
            @RequestParam String action) {

        if ("accept".equalsIgnoreCase(action)) {
            if (fieldID == null) {
                return ResponseEntity.badRequest().body("Field ID is required for accepting a device.");
            }
            String result = deviceJoinService.acceptDevice(deviceID, fieldID);
            return ResponseEntity.ok(result);

        } else if ("reject".equalsIgnoreCase(action)) {
            String result = deviceJoinService.refuseDevice(deviceID);
            return ResponseEntity.ok(result);

        } else {
            return ResponseEntity.badRequest().body("Invalid action.");
        }
    }

    @GetMapping
    public ResponseEntity<List<Device>> getAllDevices() {
        List<Device> devices = deviceService.getAllDevices();
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Device> getDeviceById(@PathVariable int id) {
        Device device = deviceService.getDeviceById(id);
        return device != null ? ResponseEntity.ok(device) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Device> createDevice(@RequestBody Device device) {
        Device savedDevice = deviceService.saveDevice(device);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDevice);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Device> updateDevice(@PathVariable int id, @RequestBody Device device) {
        Device updatedDevice = deviceService.updateDevice(id, device);
        return updatedDevice != null ? ResponseEntity.ok(updatedDevice) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable int id) {
        boolean isDeleted = deviceService.deleteDevice(id);
        return isDeleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("{deviceID}/update-interval")
    public ResponseEntity<String> updateDeviceFetchInterval(@PathVariable int deviceID, @RequestParam String interval) {
        try {
            FetchInterval newInterval = FetchInterval.valueOf(interval);

            deviceService.updateDeviceFetchInterval(deviceID, newInterval);

            scheduledSensorDataFetcher.rescheduleDeviceTask(deviceID, newInterval);

            return ResponseEntity.ok("Interval updated successfully.");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid interval value.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update interval.");
        }
    }
}
