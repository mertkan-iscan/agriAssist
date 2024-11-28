package io.mertkaniscan.automation_engine.services;

import io.mertkaniscan.automation_engine.components.config_loader.ConfigLoader;
import io.mertkaniscan.automation_engine.components.config_loader.SensorConfig;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SensorConfigService {

    private final ConfigLoader configLoader;

    // Inject ConfigLoader via constructor
    public SensorConfigService(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    public List<String> getExpectedDataTypesForSensorType(String sensorType) {
        // Retrieve the sensor configuration for the given sensorType
        Optional<SensorConfig> sensorConfigOptional = configLoader.getSensorConfigs().stream()
                .filter(sensor -> sensor.getType().equalsIgnoreCase(sensorType))
                .findFirst();

        // Return the expected data types if the configuration is found, otherwise return null
        return sensorConfigOptional.map(SensorConfig::getExpectedDataTypes).orElse(null);
    }
}
