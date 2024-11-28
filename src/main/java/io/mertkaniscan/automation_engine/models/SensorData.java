package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

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
    private BigDecimal dataValue;

    @Column(nullable = false, updatable = false)
    private Timestamp timestamp;

    @JsonBackReference // Prevents infinite recursion in Device serialization
    @ManyToOne
    @JoinColumn(name = "deviceID", nullable = false)
    private Device device;

    @JsonBackReference("field-sensorData")
    @ManyToOne
    @JoinColumn(name = "fieldID", nullable = false)
    private Field field;

    public SensorData() {
        // No-argument constructor for JPA
    }

    public SensorData(String dataType, BigDecimal dataValue, Timestamp timestamp, Device device) {
        this.dataType = dataType;
        this.dataValue = dataValue;
        this.timestamp = timestamp;
        this.device = device;
    }

    @PrePersist
    protected void onCreate() {
        timestamp = new Timestamp(System.currentTimeMillis());
    }

    // Getters and Setters
    public int getSensorDataID() {
        return sensorDataID;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public BigDecimal getDataValue() {
        return dataValue;
    }

    public void setDataValue(BigDecimal dataValue) {
        this.dataValue = dataValue;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
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
        return Objects.hash(sensorDataID);
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
