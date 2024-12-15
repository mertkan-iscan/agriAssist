package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "sensor_datas")
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int sensorDataID;

    @Column(nullable = false)
    @NotNull(message = "Data type cannot be null")
    @Size(min = 1, max = 255, message = "Data type must be between 1 and 255 characters")
    private String dataType;

    @Column(nullable = false)
    @NotNull(message = "Data value cannot be null")
    @Min(value = 0, message = "Data value must be non-negative")
    private Double dataValue;

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

    public SensorData() {
        // No-argument constructor for JPA
    }

    public SensorData(String dataType, Double dataValue, Timestamp timestamp, Device device) {
        this.dataType = dataType;
        this.dataValue = dataValue;
        this.timestamp = timestamp;
        this.device = device;
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
}
