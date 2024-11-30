package io.mertkaniscan.automation_engine.services.main_services;

import io.mertkaniscan.automation_engine.components.config_loader.ConfigLoader;
import io.mertkaniscan.automation_engine.components.config_loader.PlantConfig;
import io.mertkaniscan.automation_engine.models.Plant;
import io.mertkaniscan.automation_engine.repositories.PlantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PlantService {

    @Autowired
    private PlantRepository plantRepository;

    @Autowired
    private ConfigLoader configLoader;

    public Plant savePlant(Plant plant) {

        PlantConfig plantConfig = configLoader.getPlantConfigs().stream()
                .filter(config -> config.getPlantType().equalsIgnoreCase(plant.getPlantType().toString()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Plant type not found in configuration: " + plant.getPlantType()));

        plant.setCurrentCropCoefficient(BigDecimal.valueOf(plantConfig.getKcValues().getKcInit()));
        plant.setCurrentRootZoneDepth(plantConfig.getRootZoneDepth());
        plant.setAllowableDepletion(plantConfig.getAllowableDepletion());

        return plantRepository.save(plant);
    }
}