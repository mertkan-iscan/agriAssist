package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "fields")
public class Field {

    @OneToOne(mappedBy = "field", cascade = CascadeType.ALL, orphanRemoval = true)
    private FieldCurrentValues currentValues;

    public enum FieldType {
        OUTDOOR,
        GREENHOUSE,
    }

    public enum FieldIrrigationStatus {
        NOT_IRRIGATING,
        IRRIGATING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fieldid")
    private Integer fieldID;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Timestamp fieldCreationDate;
    @Column(nullable = false, unique = true)
    private String fieldName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType fieldType;

    @Column(nullable = false)
    private Double totalArea; //m2
    @Column(nullable = false)
    private Double latitude;
    @Column(nullable = false)
    private Double longitude;
    @Column(nullable = false)
    private Double elevation;

    // soil variables
    @Column(nullable = false)
    private String fieldSoilType;
    @Column(nullable = false)
    private Double fieldCapacity;
    @Column(nullable = false)
    private Double wiltingPoint;
    @Column(nullable = false)
    private Double bulkDensity; // soil density (g/cm³)
    @Column(nullable = false)
    private Double saturation; // Toprağın maksimum su kapasitesi
    @Column(nullable = false)
    private Double infiltrationRate;//(mm/hour)
    @Column(nullable = false)
    private Double evaporationCoeff;
    @Column(nullable = false)
    private Double maxEvaporationDepth;

    // relations
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "plant_id", referencedColumnName = "plantID")
    private Plant plantInField;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldIrrigationStatus fieldIrrigationStatus = FieldIrrigationStatus.NOT_IRRIGATING;

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("field-irrigationRequest")
    private List<IrrigationRequest> irrigationRequests;

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("field-devices")
    private Set<Device> devices;

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("field-sensorData")
    private Set<SensorData> sensorData;


    public Field() {}

    public Field(Integer fieldID, String fieldName, FieldType fieldType, String fieldSoilType, Plant plantInField,
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
}
