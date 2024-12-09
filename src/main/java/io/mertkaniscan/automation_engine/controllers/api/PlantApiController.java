package io.mertkaniscan.automation_engine.controllers.api;

import io.mertkaniscan.automation_engine.utils.config_loader.ConfigLoader;
import io.mertkaniscan.automation_engine.utils.config_loader.PlantConfig;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/plants")
public class PlantApiController {

    private final ConfigLoader configLoader;

    public PlantApiController(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    @GetMapping("/types")
    public List<String> getPlantTypes() {
        return configLoader.getPlantConfigs()
                .stream()
                .map(PlantConfig::getPlantType)
                .collect(Collectors.toList());
    }

    @GetMapping("/{plantType}/stages")
    public List<String> getPlantStages(@PathVariable String plantType) {
        return configLoader.getPlantConfigs().stream()
                .filter(config -> config.getPlantType().equalsIgnoreCase(plantType))
                .findFirst()
                .map(PlantConfig::getStages)
                .orElseThrow(() -> new RuntimeException("Plant type not found"));
    }
}