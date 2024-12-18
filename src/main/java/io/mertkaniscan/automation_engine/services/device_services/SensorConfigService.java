package io.mertkaniscan.automation_engine.services;

import io.mertkaniscan.automation_engine.utils.config_loader.ConfigLoader;
import io.mertkaniscan.automation_engine.utils.config_loader.SensorConfig;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SensorConfigService {

    private final ConfigLoader configLoader;

    public SensorConfigService(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    public List<String> getExpectedDataTypesForSensorType(String sensorType) {

        Optional<SensorConfig> sensorConfigOptional = configLoader.getSensorConfigs().stream()
                .filter(sensor -> sensor.getType().equalsIgnoreCase(sensorType))
                .findFirst();

        return sensorConfigOptional.map(SensorConfig::getExpectedDataTypes).orElse(null);
    }
}
