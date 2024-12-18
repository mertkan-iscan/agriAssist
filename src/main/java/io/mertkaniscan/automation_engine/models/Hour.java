package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "hours")
public class Hour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int hourID;

    @Column(nullable = false, unique = true)
    private int minuteOfDay; // Gün içindeki toplam dakika (0-1439)

    @Column
    private Double KeValue;

    @Column
    private Double TEWValue;

    @Column
    private Double REWValue;

    //@Column
    //private Double soilMoisture;

    @Column
    private Double sensorEToHourly;

    @Column
    private Double guessedEtoHourly;

    @Column
    private Double sensorTemperature;

    @Column
    private Double forecastTemperature;

    @Column
    private Double forecastWindSpeed;

    @Column
    private Double sensorWindSpeed;

    @Column
    private Double sensorHumidity;

    @Column
    private Double forecastHumidity;

    @Column
    private Double precipitation;

    @Column
    private Double solarRadiation;

    @Column
    private Double DeValue; // Buharlaşabilir su açığı (De)

    @Column
    private LocalDateTime lastUpdated;

    @Column
    private Double hourlyDepletion;

    @ManyToOne
    @JoinColumn(name = "dayid", nullable = false)
    @JsonBackReference("day-hours")
    private Day day;

    public Hour() {}

    public Hour(int minuteOfDay, Double guessedEtoHourly, Day day) {
        this.minuteOfDay = minuteOfDay;
        this.guessedEtoHourly = guessedEtoHourly;
        this.day = day;
    }

    public int getHour() {
        return this.minuteOfDay / 60;
    }

    public int getMinute() {
        return this.minuteOfDay % 60;
    }

    public Double getWindSpeed(Field.FieldType fieldType) {

        if (fieldType == Field.FieldType.GREENHOUSE) {
            return 0.0;
        }

        if (this.sensorWindSpeed != null) {
            return this.sensorWindSpeed;
        }

        return this.forecastWindSpeed;
    }

}
