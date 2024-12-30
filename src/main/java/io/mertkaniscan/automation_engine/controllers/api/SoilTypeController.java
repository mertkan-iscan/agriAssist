package io.mertkaniscan.automation_engine.controllers.api;

import io.mertkaniscan.automation_engine.utils.config_loader.ConfigLoader;
import io.mertkaniscan.automation_engine.utils.config_loader.FieldConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/soil-types")
public class SoilTypeController {
    private final ConfigLoader configLoader;

    public SoilTypeController(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    @GetMapping
    public List<String> getSoilTypes() {
        return configLoader.getFieldConfigs()
                .stream()
                .map(FieldConfig::getSoilType)
                .toList();
    }
}
