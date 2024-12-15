package io.mertkaniscan.automation_engine.services.main_services;

import io.mertkaniscan.automation_engine.utils.config_loader.ConfigLoader;
import io.mertkaniscan.automation_engine.utils.config_loader.PlantConfig;
import io.mertkaniscan.automation_engine.models.Plant;
import io.mertkaniscan.automation_engine.repositories.PlantRepository;
import org.springframework.stereotype.Service;

@Service
public class PlantService {

    private final PlantRepository plantRepository;
    private final ConfigLoader configLoader;

    public PlantService(PlantRepository plantRepository, ConfigLoader configLoader) {
        this.plantRepository = plantRepository;
        this.configLoader = configLoader;
    }

    public Plant savePlant(Plant plant) {

        PlantConfig plantConfig = configLoader.getPlantConfigs().stream()
                .filter(config -> config.getPlantType().equalsIgnoreCase(plant.getPlantType().toString()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Plant type not found in configuration: " + plant.getPlantType()));

        plant.setCurrentCropCoefficient(plantConfig.getKcValues().getKcInit());
        plant.setCurrentRootZoneDepth(plantConfig.getRootZoneDepth());
        plant.setAllowableDepletion(plantConfig.getAllowableDepletion());

        return plantRepository.save(plant);
    }
}