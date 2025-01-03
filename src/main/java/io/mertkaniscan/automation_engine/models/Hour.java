package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "hours")
public class Hour {

    //@Column
    //private Double guessedWaterVolume;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int hourID;
    @Column(nullable = false)
    private int hourIndex;
    @Column
    private LocalDateTime lastUpdated;

    // evaporation
    @Column
    private Double KeValue;
    @Column
    private Double KrValue;

    // water
    @Column
    private Double TAWValueHourly;
    @Column
    private Double RAWValueHourly;
    @Column
    private Double TEWValueHourly;
    @Column
    private Double REWValueHourly;


    // ETo
    @Column
    private Double forecastEToHourly;
    @Column
    private Double sensorEToHourly;
    @Column
    private Double guessedEtoHourly;

    //sensor
    @Column
    private Double sensorTemperature;
    @Column
    private Double sensorHumidity;

    //forecast
    @Column
    private Double forecastTemperature;
    @Column
    private Double forecastHumidity;
    @Column
    private Double forecastWindSpeed;
    @Column
    private Double forecastPrecipitation;


    @Column
    private Double happenedPrecipitation;

    @Column
    private Double solarRadiation;

    @Column
    private Double DeValue;

    @Column
    private Double hourlyDepletion;

    @Column
    private Double calculatedKcbAdjusted;



    @ManyToOne
    @JoinColumn(name = "dayid", nullable = false)
    @JsonBackReference("day-hours")
    private Day day;

    public Hour() {}

    public Hour(int hourIndex, Day day) {
        this.hourIndex = hourIndex;
        this.day = day;
    }

    public Double getWindSpeed(Field.FieldType fieldType) {

        if (fieldType == Field.FieldType.GREENHOUSE) {
            return 0.0;
        }

        return this.forecastWindSpeed;
    }

}
