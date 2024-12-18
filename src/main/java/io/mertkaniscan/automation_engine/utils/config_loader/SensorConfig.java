package io.mertkaniscan.automation_engine.utils.config_loader;

import java.util.List;
import java.util.Map;

public class SensorConfig {
    private String type;
    private Map<String, List<String>> commandDataTypes;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, List<String>> getCommandDataTypes() {
        return commandDataTypes;
    }

    public void setCommandDataTypes(Map<String, List<String>> commandDataTypes) {
        this.commandDataTypes = commandDataTypes;
    }

    public List<String> getExpectedDataTypesForCommand(String command) {
        return commandDataTypes.get(command);
    }
}