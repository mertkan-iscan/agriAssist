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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int plantID;

    @Column(nullable = false)
    private int fieldID;

    @Column(nullable = false)
    private String plantType;

    @Column(nullable = false)
    private Timestamp plantSowDate;

    @Column
    private String plantStage;

    @Column
    private Integer dayAfterSowDate;

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

    public Plant(int plantID, String plantType, Timestamp plantSowDate, String plantStage,
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
}
