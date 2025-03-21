package io.mertkaniscan.automation_engine.controllers.view;

import io.mertkaniscan.automation_engine.models.Device;
import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.services.main_services.DeviceService;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/fields")
public class FieldViewController {

    @Autowired
    private FieldService fieldService;

    @Autowired
    private DeviceService deviceService;

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

}
