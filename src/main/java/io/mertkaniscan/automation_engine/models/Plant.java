package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
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

    @Column(nullable = false)
    private int fieldID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlantType plantType;

    @Column(nullable = false)
    private Timestamp plantSowDate;

    @Enumerated(EnumType.STRING)
    @Column
    private PlantStage plantStage;

    @Column(nullable = false)
    private Double currentRootZoneDepth;

    @Column(nullable = false)
    private Double allowableDepletion;

    @Column(nullable = false)
    private Double currentKcValue;

    @OneToMany(mappedBy = "plant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("plant-days")
    private List<Day> days;

    public Plant() {
    }

    public Plant(int plantID, PlantType plantType, Timestamp plantSowDate, PlantStage plantStage,
                 Double currentRootZoneDepth, Double allowableDepletion, Double currentKcValue, int fieldID) {

        this.plantID = plantID;
        this.plantType = plantType;
        this.plantSowDate = plantSowDate;
        this.plantStage = plantStage;
        this.currentRootZoneDepth = currentRootZoneDepth;
        this.allowableDepletion = allowableDepletion;
        this.currentKcValue = currentKcValue;
        this.fieldID = fieldID;
    }

    public Day getToday() {

        if (days == null || days.isEmpty()) {
            return null;
        }

        LocalDate today = LocalDate.now();

        return days.stream()
                .filter(day -> {
                    LocalDate dayDate = day.getDate().toLocalDateTime().toLocalDate();
                    return dayDate.equals(today);
                })
                .findFirst()
                .orElse(null);
    }

    public Hour getCurrentHour() {
        Day today = getToday();
        if (today == null || today.getHours() == null || today.getHours().isEmpty()) {
            return null;
        }

        // Get current time in minutes since midnight
        LocalTime now = LocalTime.now();
        int currentMinuteOfDay = now.getHour() * 60 + now.getMinute();

        // Find the closest hour record
        return today.getHours().stream()
                .min((h1, h2) -> {
                    int diff1 = Math.abs(h1.getMinuteOfDay() - currentMinuteOfDay);
                    int diff2 = Math.abs(h2.getMinuteOfDay() - currentMinuteOfDay);
                    return Integer.compare(diff1, diff2);
                })
                .orElse(null);
    }
}
