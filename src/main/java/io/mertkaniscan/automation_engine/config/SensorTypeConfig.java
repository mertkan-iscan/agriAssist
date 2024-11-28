package io.mertkaniscan.automation_engine.config;

import java.util.List;
import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class SensorTypeConfig {

    @XmlElement(name = "type")
    private String type;

    @XmlElementWrapper(name = "expectedDataTypes")
    @XmlElement(name = "dataType")
    private List<String> expectedDataTypes;

    // Getters and setters
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
