package io.mertkaniscan.automation_engine.services.device_services;

import io.mertkaniscan.automation_engine.utils.config_loader.ConfigLoader;
import io.mertkaniscan.automation_engine.utils.config_loader.DeviceCommandConfigLoader;
import io.mertkaniscan.automation_engine.utils.config_loader.SensorConfig;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SensorConfigService {
    private final ConfigLoader configLoader;
    private final DeviceCommandConfigLoader deviceCommandConfigLoader;

    public SensorConfigService(ConfigLoader configLoader, DeviceCommandConfigLoader deviceCommandConfigLoader) {
        this.configLoader = configLoader;
        this.deviceCommandConfigLoader = deviceCommandConfigLoader;
    }

    /**
     * Get expected data types for a given sensor type and command.
     */
    public List<String> getExpectedDataTypesForSensorTypeAndCommand(String sensorType, String command) {
        Optional<SensorConfig> sensorConfigOptional = configLoader.getSensorConfigs().stream()
                .filter(sensor -> sensor.getType().equalsIgnoreCase(sensorType))
                .findFirst();

        return sensorConfigOptional.map(config -> config.getExpectedDataTypesForCommand(command))
                .orElse(null);
    }

    /**
     * Get available commands for a given sensor type.
     */
    public List<String> getAvailableCommandsForSensorType(String sensorType) {
        Optional<SensorConfig> sensorConfigOptional = configLoader.getSensorConfigs().stream()
                .filter(sensor -> sensor.getType().equalsIgnoreCase(sensorType))
                .findFirst();

        return sensorConfigOptional.map(config -> List.copyOf(config.getCommandDataTypes().keySet()))
                .orElse(null);
    }

    /**
     * Get group name for a given sensor type and command.
     */
    public String getGroupForSensorTypeAndCommand(String sensorType, String command) {
        Optional<SensorConfig> sensorConfigOptional = configLoader.getSensorConfigs().stream()
                .filter(sensor -> sensor.getType().equalsIgnoreCase(sensorType))
                .findFirst();

        return sensorConfigOptional
                .flatMap(config -> Optional.ofNullable(config.getCommandDataTypes().get(command)))
                .map(commandDataType -> commandDataType.getGroup())
                .orElse(null);
    }
}