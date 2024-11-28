package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mertkaniscan.automation_engine.components.FetchInterval;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "devices", uniqueConstraints = @UniqueConstraint(columnNames = "device_ip"))
public class Device {

    public enum DeviceStatus {
        WAITING,

        ACTIVE,
        INACTIVE,

        RUNNING,
        STOPPED,

        ERROR,
    }
    @Id
    private int deviceID;

    @Column(nullable = false, unique = true)
    private String deviceIp;

    @Column(nullable = false)
    private String deviceType;

    @Column(nullable = false)
    private String deviceModel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus deviceStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "fetch_interval")
    private FetchInterval fetchInterval = FetchInterval.ONE_MINUTE;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String calibrationData; // Flow rate - degree  data

    @ManyToOne
    @JoinColumn(name = "fieldID", nullable = false)
    @JsonBackReference
    private Field field;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Timestamp installationDate;

    @Column(nullable = false)
    private Timestamp updatedAt;

    public Device() {
        // No-argument constructor for JPA
    }

    public Device(int deviceID, DeviceStatus deviceStatus, String deviceModel, String deviceIp, String deviceType) {
        this.deviceID = deviceID;
        this.deviceStatus = deviceStatus;
        this.deviceModel = deviceModel;
        this.deviceIp = deviceIp;
        this.deviceType = deviceType;
    }

    @PrePersist
    protected void onCreate() {
        installationDate = new Timestamp(System.currentTimeMillis());
        updatedAt = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public int getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(int deviceID) {
        this.deviceID = deviceID;
    }

    public String getDeviceIp() {
        return deviceIp;
    }

    public void setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(DeviceStatus deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public FetchInterval getFetchInterval() {
        return fetchInterval;
    }

    public void setFetchInterval(FetchInterval fetchInterval) {
        this.fetchInterval = fetchInterval;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Timestamp getInstallationDate() {
        return installationDate;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public boolean isSensor() {
        return this.deviceType.equalsIgnoreCase("sensor");
    }

    public boolean isActuator() {
        return this.deviceType.equalsIgnoreCase("actuator");
    }
    public String getCalibrationData() {
        return calibrationData;
    }

    public void setCalibrationData(String calibrationData) {
        this.calibrationData = calibrationData;
    }

    @Transient
    public Map<Double, Integer> getCalibrationMap() {

        if (calibrationData == null || calibrationData.isEmpty()) {
            // Eğer calibrationData null veya boş ise, boş bir Map döndür
            return new HashMap<>();
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(calibrationData, new TypeReference<Map<Double, Integer>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse calibration data for device ID " + this.deviceID, e);
        }
    }

    @Transient
    public void setCalibrationMap(Map<Double, Integer> calibrationMap) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.calibrationData = objectMapper.writeValueAsString(calibrationMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize calibration data for device ID " + this.deviceID, e);
        }
    }
}