package io.mertkaniscan.automation_engine.config;

import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "sensors")
@XmlAccessorType(XmlAccessType.FIELD)
public class SensorConfig {

    @XmlElement(name = "sensor")
    private List<SensorTypeConfig> sensors;

    // Getters and setters
    public List<SensorTypeConfig> getSensors() {
        return sensors;
    }

    public void setSensors(List<SensorTypeConfig> sensors) {
        this.sensors = sensors;
    }
}
