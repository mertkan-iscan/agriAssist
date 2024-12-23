package io.mertkaniscan.automation_engine.utils.config_loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Component
public class ConfigLoader {

    private List<FieldConfig> fieldConfigs;
    private List<PlantConfig> plantConfigs;
    private List<PlantConfigNew> plantConfigsNew;
    private List<SensorConfig> sensorConfigs;

    @PostConstruct
    public void loadConfigs() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();

        fieldConfigs = objectMapper.readValue(
                new File("src/main/resources/configs/field-conf.json"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, FieldConfig.class)
        );

        plantConfigs = objectMapper.readValue(
                new File("src/main/resources/configs/plant-conf.json"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, PlantConfig.class)
        );

        try {
            plantConfigsNew = objectMapper.readValue(
                    new File("src/main/resources/configs/plant-config-new.json"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, PlantConfigNew.class)
            );
            System.out.println("New Plant Configurations Loaded: " + plantConfigsNew.size());
        } catch (Exception e) {
            System.err.println("Error loading plant-config-new.json: " + e.getMessage());
            throw e;
        }

        sensorConfigs = objectMapper.readValue(
                new File("src/main/resources/configs/sensor-conf.json"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, SensorConfig.class)
        );
    }

    public List<FieldConfig> getFieldConfigs() {
        return fieldConfigs;
    }

    public List<PlantConfig> getPlantConfigs() {
        return plantConfigs;
    }

    public List<PlantConfigNew> getNewPlantConfigs() {
        return plantConfigsNew;
    }

    public List<SensorConfig> getSensorConfigs() {
        return sensorConfigs;
    }
}