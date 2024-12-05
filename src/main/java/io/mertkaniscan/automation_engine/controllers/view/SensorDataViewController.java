package io.mertkaniscan.automation_engine.controllers.view;

import io.mertkaniscan.automation_engine.models.SensorData;
import io.mertkaniscan.automation_engine.services.main_services.SensorDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/sensor-data")
public class SensorDataViewController {

    @Autowired
    private final SensorDataService sensorDataService;

    public SensorDataViewController(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    @GetMapping
    public String listSensorData(Model model) {
        List<SensorData> data = sensorDataService.getAllSensorData();
        model.addAttribute("sensorDataList", data);
        return "sensor_data_list";
    }

    @GetMapping("/search")
    public String getSensorDataByFieldIDAndType(@RequestParam int fieldID, @RequestParam String dataType, Model model) {
        List<SensorData> data = sensorDataService.getSensorDataByFieldIDAndTypeFromDb(fieldID, dataType);
        model.addAttribute("sensorDataList", data);
        return "sensor_data_list";
    }

    @GetMapping("/graph")
    public String getGraphByFieldIDAndType(@RequestParam int fieldID, @RequestParam String dataType, Model model) {
        List<SensorData> data = sensorDataService.getSensorDataByFieldIDAndTypeFromDb(fieldID, dataType);

        // Sort data by timestamp
        data.sort(Comparator.comparing(SensorData::getTimestamp));

        model.addAttribute("sensorDataList", data);
        model.addAttribute("fieldID", fieldID);
        model.addAttribute("dataType", dataType);
        return "sensor_data_graph";
    }
}
