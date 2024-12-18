package io.mertkaniscan.automation_engine.utils.config_loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class DeviceCommandConfigLoader {
    private static JsonNode configRoot;

    // Load JSON configuration file
    static {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            configRoot = objectMapper.readTree(new File("src/main/resources/configs/device_commands_config.json"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load device_commands_config.json: " + e.getMessage());
        }
    }

    // Retrieve "join response" message
    public static String getDeviceJoinResponse(String joinResponseType) {
        return createMessage(configRoot.path("deviceJoinResponse"), joinResponseType);
    }

    // Retrieve "pull sensor data" message
    public static String getPullSensorData() {
        return configRoot.path("pullSensorData").toString();
    }

    // Retrieve "pull weather data" message
    public static String getPullWeatherData() {
        return configRoot.path("pullWeatherData").toString();
    }

    // Retrieve "pull soil moisture data" message
    public static String getPullSoilMoistureData() {
        return configRoot.path("pullSoilMoistureData").toString();
    }

    // Retrieve "general actuator command" message
    public static String getActuatorCommand(String commandType) {
        String template = configRoot.path("actuatorCommands").path("general").toString();
        return template.replace("{commandType}", commandType);
    }

    // Retrieve "set valve actuator" command with degree
    public static String getValveActuatorCommand(int degree) {
        String template = configRoot.path("actuatorCommands").path("setValve").toString();
        return template.replace("{degree}", String.valueOf(degree));
    }

    // Helper method to replace messageType dynamically
    private static String createMessage(JsonNode node, String messageType) {
        String message = node.toString();
        return message.replace("join_accepted", messageType); // Replace placeholder
    }
}
