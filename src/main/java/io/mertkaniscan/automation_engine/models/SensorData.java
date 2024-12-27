package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "sensor_datas")
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int sensorDataID;

    @ElementCollection()
    @CollectionTable(name = "sensor_data_values", joinColumns = @JoinColumn(name = "sensor_data_id"))
    @MapKeyColumn(name = "data_type")
    @Column(name = "data_value")
    private Map<String, Double> dataValues = new HashMap<>();

    @Column(nullable = false, updatable = false)
    private Timestamp timestamp;

    @ManyToOne
    @JsonBackReference("device-sensorDatas")
    @JoinColumn(name = "deviceID", nullable = false)
    private Device device;

    @ManyToOne
    @JsonBackReference("field-sensorData")
    @JoinColumn(name = "fieldID", nullable = false)
    private Field field;

    // New field for sensor data type
    @Column(name = "sensor_data_type", nullable = false)
    private String sensorDataType;

    public SensorData() {
        // No-argument constructor for JPA
    }

    @PrePersist
    protected void onCreate() {
        timestamp = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SensorData that = (SensorData) o;
        return sensorDataID == that.sensorDataID;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(sensorDataID);
    }
}
