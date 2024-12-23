package io.mertkaniscan.automation_engine.utils.config_loader;

import java.util.List;
import java.util.Map;

public class SensorConfig {
    private String type; // Sensor type
    private Map<String, CommandDataType> commandDataTypes; // Command details with group and types

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, CommandDataType> getCommandDataTypes() {
        return commandDataTypes;
    }

    public void setCommandDataTypes(Map<String, CommandDataType> commandDataTypes) {
        this.commandDataTypes = commandDataTypes;
    }

    /**
     * Get the list of expected data types for a specific command.
     */
    public List<String> getExpectedDataTypesForCommand(String command) {
        CommandDataType commandDataType = commandDataTypes.get(command);
        return commandDataType != null ? commandDataType.getTypes() : null;
    }

    /**
     * Get the group for a specific command.
     */
    public String getGroupForCommand(String command) {
        CommandDataType commandDataType = commandDataTypes.get(command);
        return commandDataType != null ? commandDataType.getGroup() : null;
    }

    // Inner class representing the details of a command
    public static class CommandDataType {
        private String group; // Logical group (e.g., soil_moisture, weather)
        private List<String> types; // List of data types (e.g., depth_1_1, weather_temp)

        // Getters and Setters
        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public List<String> getTypes() {
            return types;
        }

        public void setTypes(List<String> types) {
            this.types = types;
        }
    }
}
