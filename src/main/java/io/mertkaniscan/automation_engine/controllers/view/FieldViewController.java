package io.mertkaniscan.automation_engine.controllers.view;

import io.mertkaniscan.automation_engine.models.Device;
import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.models.IrrigationRequest;
import io.mertkaniscan.automation_engine.models.Plant;
import io.mertkaniscan.automation_engine.services.device_services.DeviceJoinService;
import io.mertkaniscan.automation_engine.services.irrigation_services.IrrigationService;
import io.mertkaniscan.automation_engine.services.main_services.DeviceService;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/fields")
public class FieldViewController {

    private final FieldService fieldService;
    private final DeviceService deviceService;
    private final IrrigationService irrigationService;
    private final DeviceJoinService deviceJoinService;

    public FieldViewController(FieldService fieldService, DeviceService deviceService, IrrigationService irrigationService, DeviceJoinService deviceJoinService) {
        this.fieldService = fieldService;
        this.deviceService = deviceService;
        this.irrigationService = irrigationService;
        this.deviceJoinService = deviceJoinService;
    }

//    @GetMapping("/join-requests")
//    public String joinRequests(Model model) {
//        // Assume `deviceJoinService` is an available service for fetching join requests
//        List<Device> joinRequests = deviceJoinService.getPendingJoinRequests(); // Add this method to `DeviceJoinService` if needed
//        model.addAttribute("joinRequests", joinRequests);
//        return "join_requests";
//    }

    @GetMapping
    public String listFields(Model model) {
        List<Field> fields = fieldService.getAllFields();
        model.addAttribute("fields", fields);
        return "field_list";
    }

    @GetMapping("/add")
    public String showAddFieldForm(Model model) {
        model.addAttribute("field", new Field());
        return "add_field";
    }

    @PostMapping("/add")
    public String addField(@ModelAttribute Field field) {
        fieldService.saveField(field);
        return "redirect:/fields";
    }

    @GetMapping("/{fieldID}/edit")
    public String showEditFieldForm(@PathVariable int fieldID, Model model) {
        Field field = fieldService.getFieldById(fieldID);
        if (field == null) {
            model.addAttribute("error", "Field not found with ID: " + fieldID);
            return "error";
        }
        model.addAttribute("field", field);
        return "edit_field";
    }

    @GetMapping("/{fieldID}/edit-plant")
    public String showEditPlantForm(@PathVariable int fieldID, Model model) {
        Field field = fieldService.getFieldById(fieldID);
        if (field == null) {
            model.addAttribute("error", "Field not found with ID: " + fieldID);
            return "error";
        }

        Plant plant = field.getPlantInField();
        if (plant == null) {
            model.addAttribute("error", "No plant assigned to this field.");
            return "error";
        }

        model.addAttribute("plant", plant);
        model.addAttribute("fieldID", fieldID);
        return "edit_plant";
    }

    @GetMapping("/{fieldID}/add-plant")
    public String addPlantForm(@PathVariable int fieldID, Model model) {
        model.addAttribute("fieldID", fieldID);
        return "add_plant";
    }

    @GetMapping("/{fieldID}/devices")
    public String showDevicesByField(@PathVariable int fieldID, Model model) {
        Field field = fieldService.getFieldById(fieldID);

        if (field == null) {
            model.addAttribute("error", "Field not found with ID: " + fieldID);
            return "field_devices";
        }

        Set<Device> devices = field.getDevices();

        if (devices == null || devices.isEmpty()) {
            model.addAttribute("error", "No devices found for Field ID: " + fieldID);
            return "field_devices";
        }

        // Separate devices into sensors and actuators
        List<Device> sensorDevices = devices.stream()
                .filter(Device::isSensor)
                .collect(Collectors.toList());

        List<Device> actuatorDevices = devices.stream()
                .filter(Device::isActuator)
                .collect(Collectors.toList());

        model.addAttribute("sensorDevices", sensorDevices);
        model.addAttribute("actuatorDevices", actuatorDevices);

        return "field_devices";
    }

    @GetMapping("/{fieldID}/devices/{deviceID}/calibration")
    public String showCalibrationPage(@PathVariable int fieldID, @PathVariable int deviceID, Model model) {

        Device device = deviceService.getDeviceById(deviceID);

        if (device == null) {
            model.addAttribute("error", "Device not found with ID: " + deviceID);
            return "error";
        }

        model.addAttribute("fieldID", fieldID);
        model.addAttribute("device", device);
        return "calibration";
    }

    @GetMapping("/{fieldID}/schedule-irrigation")
    public String scheduleIrrigationPage(@PathVariable int fieldID, Model model) {
        // Retrieve the field information using the fieldID
        Field field = fieldService.getFieldById(fieldID);

        if (field == null) {
            model.addAttribute("error", "field not found with ID: " + fieldID);
        }

        // Add the field information to the model
        model.addAttribute("field", field);

        // Return the Thymeleaf template name for scheduling irrigation
        return "schedule_irrigation";
    }

    @GetMapping("/{fieldID}/scheduled-irrigations")
    public String showScheduledIrrigations(@PathVariable int fieldID, Model model) {
        // Retrieve the field
        Field field = fieldService.getFieldById(fieldID);

        if (field == null) {
            model.addAttribute("error", "Field not found with ID: " + fieldID);
            return "error";
        }

        // Get the list of scheduled irrigations for the field
        List<IrrigationRequest> scheduledIrrigations = irrigationService.getScheduledIrrigationsByField(fieldID);

        if (scheduledIrrigations == null || scheduledIrrigations.isEmpty()) {
            model.addAttribute("message", "No scheduled irrigations found for this field.");
        } else {
            model.addAttribute("scheduledIrrigations", scheduledIrrigations);
        }

        model.addAttribute("field", field);
        return "scheduled_irrigations";
    }

}
