package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "fields")
public class Field {

    public enum FieldType {
        SOILFIELD,
        GREENHOUSE
    }

    public enum SoilType {
        SANDYLOAM,
        SILTLOAM,
        CLAY,
        LOAM
    }

    public enum IrrigationStatus {
        NOT_IRRIGATING,
        IRRIGATING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fieldid")
    private Integer fieldID;

    @Column(nullable = false, unique = true)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType fieldType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SoilType fieldSoilType;

    @Column(nullable = false)
    private Double fieldCapacity; // FC (Field Capacity)

    @Column(nullable = false)
    private Double wiltingPoint; // WP (Wilting Point)

    @Column(nullable = false)
    private Double bulkDensity; // soil density (g/cm³)

    @Column(nullable = false)
    private Double saturation; // Toprağın maksimum su kapasitesi

    @Column(nullable = false)
    private Double infiltrationRate; // İnfiltrasyon hızı (mm/saat)

    @Column(nullable = false)
    private Double totalArea; // Toplam alan (m²)

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Timestamp fieldCreationDate;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "plant_id", referencedColumnName = "plantID")
    private Plant plantInField;

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Device> devices;

    @JsonManagedReference("field-sensorData")
    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<SensorData> sensorData;

    @JsonManagedReference("field-irrigationRequest")
    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<IrrigationRequest> irrigationRequests;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IrrigationStatus irrigationStatus = IrrigationStatus.NOT_IRRIGATING;

    public Field(Integer fieldID, String fieldName, FieldType fieldType, SoilType fieldSoilType, Plant plantInField,
                 Double fieldCapacity, Double wiltingPoint, Double bulkDensity, Double saturation,
                 Double infiltrationRate, Double totalArea, Double latitude, Double longitude, Timestamp fieldCreationDate) {
        this.fieldID = fieldID;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.fieldSoilType = fieldSoilType;
        this.plantInField = plantInField;
        this.fieldCapacity = fieldCapacity;
        this.wiltingPoint = wiltingPoint;
        this.bulkDensity = bulkDensity;
        this.saturation = saturation;
        this.infiltrationRate = infiltrationRate;
        this.totalArea = totalArea;
        this.latitude = latitude;
        this.longitude = longitude;
        this.fieldCreationDate = fieldCreationDate;
    }

    public Field() {
    }

    // Getters and Setters
    public Integer getFieldID() {
        return fieldID;
    }

    public void setFieldID(Integer fieldID) {
        this.fieldID = fieldID;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public SoilType getFieldSoilType() {
        return fieldSoilType;
    }

    public void setFieldSoilType(SoilType fieldSoilType) {
        this.fieldSoilType = fieldSoilType;
    }

    public Double getFieldCapacity() {
        return fieldCapacity;
    }

    public void setFieldCapacity(Double fieldCapacity) {
        this.fieldCapacity = fieldCapacity;
    }

    public Double getWiltingPoint() {
        return wiltingPoint;
    }

    public void setWiltingPoint(Double wiltingPoint) {
        this.wiltingPoint = wiltingPoint;
    }

    public Double getBulkDensity() {
        return bulkDensity;
    }

    public void setBulkDensity(Double bulkDensity) {
        this.bulkDensity = bulkDensity;
    }

    public Double getSaturation() {
        return saturation;
    }

    public void setSaturation(Double saturation) {
        this.saturation = saturation;
    }

    public Double getInfiltrationRate() {
        return infiltrationRate;
    }

    public void setInfiltrationRate(Double infiltrationRate) {
        this.infiltrationRate = infiltrationRate;
    }

    public Double getTotalArea() {
        return totalArea;
    }

    public void setTotalArea(Double totalArea) {
        this.totalArea = totalArea;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Plant getPlantInField() {
        return plantInField;
    }

    public void setPlantInField(Plant plantInField) {
        this.plantInField = plantInField;
    }

    public Timestamp getFieldCreationDate() {
        return fieldCreationDate;
    }

    public void setFieldCreationDate(Timestamp fieldCreationDate) {
        this.fieldCreationDate = fieldCreationDate;
    }

    public Set<Device> getDevices() {
        return devices;
    }

    public void setDevices(Set<Device> devices) {
        this.devices = devices;
    }

    public List<IrrigationRequest> getIrrigationRequests() {
        return irrigationRequests;
    }

    public void setIrrigationRequests(List<IrrigationRequest> irrigationRequests) {
        this.irrigationRequests = irrigationRequests;
    }

    public Set<SensorData> getSensorData() {
        return sensorData;
    }

    public void setSensorData(Set<SensorData> sensorData) {
        this.sensorData = sensorData;
    }

    public IrrigationStatus getIrrigationStatus() {
        return irrigationStatus;
    }

    public void setIrrigationStatus(IrrigationStatus irrigationStatus) {
        this.irrigationStatus = irrigationStatus;
    }
}
