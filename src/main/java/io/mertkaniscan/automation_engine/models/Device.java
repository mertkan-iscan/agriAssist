package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mertkaniscan.automation_engine.utils.FetchInterval;
import io.mertkaniscan.automation_engine.utils.NonReentrantLock;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "devices", uniqueConstraints = @UniqueConstraint(columnNames = "device_ip"))
public class Device {

    @Transient
    private final NonReentrantLock lock = new NonReentrantLock();

    public enum DeviceStatus {
        WAITING,
        ACTIVE,
        INACTIVE,
        RUNNING,
        STOPPED,
        ERROR,
    }

    @Setter
    @Id
    private int deviceID;

    @Setter
    @Column(nullable = false, unique = true)
    private String deviceIp;

    @Column
    private Integer devicePort;

    @Column(nullable = false)
    private String deviceType;

    @Setter
    @Column(nullable = false)
    private String deviceModel;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus deviceStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "fetch_interval")
    private FetchInterval fetchInterval = FetchInterval.ONE_MINUTE;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String calibrationData;

    @ManyToOne
    @JoinColumn(name = "fieldID", nullable = false)
    @JsonBackReference("field-devices")
    private Field field;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Timestamp installationDate;

    @Column(nullable = false)
    private Timestamp updatedAt;

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("device-sensorDatas")
    private List<SensorData> sensorDatas;

    public Device() {
        // No-argument constructor for JPA
    }

    public Device(int deviceID, DeviceStatus deviceStatus, String deviceModel, String deviceIp, String deviceType) {
        this.deviceID = deviceID;
        this.deviceStatus = deviceStatus;
        this.deviceModel = deviceModel;
        this.deviceIp = deviceIp;
        this.deviceType = deviceType;
        setDefaultCalibrationPolynomial();
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

    public boolean isSensor() {
        return this.deviceType.equalsIgnoreCase("sensor");
    }

    public boolean isActuator() {
        return this.deviceType.equalsIgnoreCase("actuator");
    }

    @Transient
    public Map<Double, Integer> getCalibrationMap() {
        if (calibrationData == null || calibrationData.isEmpty()) {
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

    @Transient
    public Map<Double, Integer> getCalibrationPolynomial() {
        if (calibrationData == null || calibrationData.isEmpty()) {
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
    public void setCalibrationPolynomial(Map<Double, Integer> polynomial) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.calibrationData = objectMapper.writeValueAsString(polynomial);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize calibration data for device ID " + this.deviceID, e);
        }
    }

    /**
     * Set default calibration polynomial.
     */
    public void setDefaultCalibrationPolynomial() {
        if (this.calibrationData == null || this.calibrationData.isEmpty()) {
            Map<Double, Integer> defaultPolynomial = new HashMap<>();
            defaultPolynomial.put(0.024114825980221664, 1);
            defaultPolynomial.put(2.6691733715467696, 0);
            setCalibrationPolynomial(defaultPolynomial);
        }
    }

    public void lock() throws InterruptedException {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }
}
