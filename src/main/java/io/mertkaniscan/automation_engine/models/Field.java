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

    // Getters and Setters
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

    @Column(nullable = false)
    private Double elevation;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Timestamp fieldCreationDate;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "plant_id", referencedColumnName = "plantID")
    private Plant plantInField;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IrrigationStatus irrigationStatus = IrrigationStatus.NOT_IRRIGATING;

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("field-devices")
    private Set<Device> devices;


    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("field-sensorData")
    private Set<SensorData> sensorData;

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("field-irrigationRequest")
    private List<IrrigationRequest> irrigationRequests;

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("field-solarResponses")
    private Set<SolarResponse> solarResponses;

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("field-weatherResponses")
    private Set<WeatherResponse> weatherResponses ;

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
        //for jpa
    }
}
