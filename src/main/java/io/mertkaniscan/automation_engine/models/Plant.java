package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "plants")
public class Plant {

    public enum PlantType {
        TOMATO,
        LETTUCE,
        CUCUMBER
    }

    public enum PlantStage {
        EARLY_GROWTH,
        VEGETATIVE,
        FLOWERING,
        POLLINATION,
        FRUIT_FORMATION,
        FRUIT_RIPENING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int plantID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlantType plantType;

    @Column(nullable = false)
    private Timestamp plantSowDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private PlantStage plantStage;

    @Column(nullable = false)
    private Double currentRootZoneDepth;

    @Column(nullable = false)
    private Double allowableDepletion;

    @Column(nullable = false)
    private Double currentCropCoefficient; // Kc

    @Column(nullable = false)
    private int fieldID;

    @OneToMany(mappedBy = "plant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("plant-days")
    private List<Day> days;

    public Plant() {
    }

    public Plant(int plantID, PlantType plantType, Timestamp plantSowDate, PlantStage plantStage,
                 Double currentRootZoneDepth, Double allowableDepletion, Double currentCropCoefficient, int fieldID) {

        this.plantID = plantID;
        this.plantType = plantType;
        this.plantSowDate = plantSowDate;
        this.plantStage = plantStage;
        this.currentRootZoneDepth = currentRootZoneDepth;
        this.allowableDepletion = allowableDepletion;
        this.currentCropCoefficient = currentCropCoefficient;
        this.fieldID = fieldID;
    }
}
