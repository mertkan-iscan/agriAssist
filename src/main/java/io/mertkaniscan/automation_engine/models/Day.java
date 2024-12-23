package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.ArrayList;
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

    @Column
    private Timestamp sunrise;

    @Column
    private Timestamp sunset;

    @Column
    private Double vpd;

    @Column
    private Double guessedEtoDaily;

    @Column
    private Double dailyDepletion;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("day-hours")
    private List<Hour> hours = new ArrayList<>();

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
}