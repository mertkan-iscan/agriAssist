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
@Table(name = "days")
public class Day {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int dayID;

    @Column(nullable = false)
    private Timestamp date;

    @Column(nullable = false)
    private Timestamp sunrise;

    @Column(nullable = false)
    private Timestamp sunset;

    @Column(nullable = false)
    private Double vpd;

    @Column(nullable = false)
    private Double guessedEtoDaily = 0.0;

    @Column
    private Double dailyDepletion; // Günlük toplam eksilme (mm)

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("day-hours")
    private List<Hour> hours;

    // Many-to-One relationship, many days belong to one plant
    @ManyToOne
    @JoinColumn(name = "plantID", nullable = false)
    @JsonBackReference("plant-days")
    private Plant plant;

    @OneToOne(mappedBy = "day", cascade = CascadeType.ALL)
    private SolarResponse solarResponse;

    @OneToOne(mappedBy = "day", cascade = CascadeType.ALL)
    private WeatherResponse weatherResponse;


    public Day() {
    }

    public Day(Timestamp date, Timestamp sunrise, Timestamp sunset, Double vpd, Plant plant) {
        this.date = date;
        this.sunrise = sunrise;
        this.sunset = sunset;
        this.vpd = vpd;
        this.plant = plant;
    }
}