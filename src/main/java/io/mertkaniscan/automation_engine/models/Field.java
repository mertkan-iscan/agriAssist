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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fieldid")
    private int fieldID;

    @Column(nullable = false, unique = true)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType fieldType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SoilType fieldSoilType;

    @Column(nullable = false)
    private double fieldCapacity; // FC (Field Capacity)

    @Column(nullable = false)
    private double wiltingPoint; // WP (Wilting Point)

    @Column(nullable = false)
    private double bulkDensity; // soil density (g/cm³)

    @Column(nullable = false)
    private double saturation; // Toprağın maksimum su kapasitesi

    @Column(nullable = false)
    private double infiltrationRate; // İnfiltrasyon hızı (mm/saat)

    @Column(nullable = false)
    private double totalArea; // Toplam alan (m²)

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Timestamp fieldCreationDate;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "plant_id", referencedColumnName = "plantID")
    private Plant plantInField;

    //@OneToOne(cascade = CascadeType.ALL)
    //@JoinColumn(name = "hoseID")
    //private Hose hose;

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Device> devices;

    @JsonManagedReference("field-sensorData")
    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<SensorData> sensorData;

    @JsonManagedReference("field-irrigationRequest")
    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<IrrigationRequest> irrigationRequests;

    public Field(int fieldID, String fieldName, FieldType fieldType, SoilType fieldSoilType, Plant plantInField,
                 double fieldCapacity, double wiltingPoint, double bulkDensity, double saturation,
                 double infiltrationRate, double totalArea, double latitude, double longitude, Timestamp fieldCreationDate) {
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

    public int getFieldID() {
        return fieldID;
    }

    public void setFieldID(int fieldID) {
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

    public double getFieldCapacity() {
        return fieldCapacity;
    }

    public void setFieldCapacity(double fieldCapacity) {
        this.fieldCapacity = fieldCapacity;
    }

    public double getWiltingPoint() {
        return wiltingPoint;
    }

    public void setWiltingPoint(double wiltingPoint) {
        this.wiltingPoint = wiltingPoint;
    }

    public double getBulkDensity() {
        return bulkDensity;
    }

    public void setBulkDensity(double bulkDensity) {
        this.bulkDensity = bulkDensity;
    }

    public double getSaturation() {
        return saturation;
    }

    public void setSaturation(double saturation) {
        this.saturation = saturation;
    }

    public double getInfiltrationRate() {
        return infiltrationRate;
    }

    public void setInfiltrationRate(double infiltrationRate) {
        this.infiltrationRate = infiltrationRate;
    }

    public double getTotalArea() {
        return totalArea;
    }

    public void setTotalArea(double totalArea) {
        this.totalArea = totalArea;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
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
}