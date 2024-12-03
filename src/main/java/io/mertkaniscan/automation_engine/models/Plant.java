package io.mertkaniscan.automation_engine.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

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
    private double currentRootZoneDepth;

    @Column(nullable = false)
    private double allowableDepletion;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal currentCropCoefficient; // Kc

    @Column(nullable = false)
    private int fieldID;

    @OneToMany(mappedBy = "plant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Day> days;

    public Plant() {
    }

    public Plant(int plantID, PlantType plantType, Timestamp plantSowDate, PlantStage plantStage,
                 double currentRootZoneDepth, double allowableDepletion, BigDecimal currentCropCoefficient, int fieldID) {
        this.plantID = plantID;
        this.plantType = plantType;
        this.plantSowDate = plantSowDate;
        this.plantStage = plantStage;
        this.currentRootZoneDepth = currentRootZoneDepth;
        this.allowableDepletion = allowableDepletion;
        this.currentCropCoefficient = currentCropCoefficient;
        this.fieldID = fieldID;
    }

    // Getters and Setters

    public int getPlantID() {
        return plantID;
    }

    public void setPlantID(int plantID) {
        this.plantID = plantID;
    }

    public PlantType getPlantType() {
        return plantType;
    }

    public void setPlantType(PlantType plantType) {
        this.plantType = plantType;
    }

    public Timestamp getPlantSowDate() {
        return plantSowDate;
    }

    public void setPlantSowDate(Timestamp plantSowDate) {
        this.plantSowDate = plantSowDate;
    }

    public PlantStage getPlantStage() {
        return plantStage;
    }

    public void setPlantStage(PlantStage plantStage) {
        this.plantStage = plantStage;
    }

    public double getCurrentRootZoneDepth() {
        return currentRootZoneDepth;
    }

    public void setCurrentRootZoneDepth(double currentRootZoneDepth) {
        this.currentRootZoneDepth = currentRootZoneDepth;
    }

    public double getAllowableDepletion() {
        return allowableDepletion;
    }

    public void setAllowableDepletion(double allowableDepletion) {
        this.allowableDepletion = allowableDepletion;
    }

    public BigDecimal getCurrentCropCoefficient() {
        return currentCropCoefficient;
    }

    public void setCurrentCropCoefficient(BigDecimal currentCropCoefficient) {
        this.currentCropCoefficient = currentCropCoefficient;
    }

    public int getFieldID() {
        return fieldID;
    }

    public void setFieldID(int fieldID) {
        this.fieldID = fieldID;
    }

    public List<Day> getDays() {
        return days;
    }

    public void setDays(List<Day> days) {
        this.days = days;
    }
}
