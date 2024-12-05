package io.mertkaniscan.automation_engine.utils.config_loader;

import java.util.List;

public class SensorConfig {

    private String type;
    private List<String> expectedDataTypes;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getExpectedDataTypes() {
        return expectedDataTypes;
    }

    public void setExpectedDataTypes(List<String> expectedDataTypes) {
        this.expectedDataTypes = expectedDataTypes;
    }
}