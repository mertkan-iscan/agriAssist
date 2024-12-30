package io.mertkaniscan.automation_engine.services.main_services;

import io.mertkaniscan.automation_engine.utils.config_loader.ConfigLoader;
import io.mertkaniscan.automation_engine.utils.config_loader.PlantConfig;
import io.mertkaniscan.automation_engine.models.Plant;
import io.mertkaniscan.automation_engine.repositories.PlantRepository;
import io.mertkaniscan.automation_engine.utils.config_loader.PlantConfigNew;
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

        // Retrieve the plant configuration based on the plant type
        PlantConfigNew plantConfig = configLoader.getNewPlantConfigs().stream()
                .filter(config -> config.getPlantType().equalsIgnoreCase(plant.getPlantType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Plant type not found in configuration: " + plant.getPlantType()));

        // Retrieve the current stage configuration
        PlantConfigNew.StageConfig stageConfig = plantConfig.getStages().stream()
                .filter(stage -> stage.getStageName().equalsIgnoreCase(plant.getPlantStage()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Plant stage not found in configuration: " + plant.getPlantStage()));

        // Set values based on the current stage configuration
        plant.setCurrentKcValue(stageConfig.getKcValue());
        plant.setCurrentRootZoneDepth(stageConfig.getRootZoneDepth());
        plant.setAllowableDepletion(stageConfig.getAllowableDepletion());

        // Save the plant to the repository
        return plantRepository.save(plant);
    }
}