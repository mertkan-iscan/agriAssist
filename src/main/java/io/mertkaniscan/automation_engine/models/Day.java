package io.mertkaniscan.automation_engine.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

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
    private BigDecimal vpd; // vapour preassure deficit

    // Many-to-One relationship, many days belong to one plant
    @ManyToOne
    @JoinColumn(name = "plantID", nullable = false)
    private Plant plant;

    public Day() {
    }

    public Day(Timestamp date, Timestamp sunrise, Timestamp sunset, BigDecimal meanSoilHumidity, BigDecimal meanAirTemperature, BigDecimal meanAirHumidity, BigDecimal meanWindSpeed, Plant plant) {
        this.date = date;
        this.sunrise = sunrise;
        this.sunset = sunset;
        this.plant = plant;
    }

}
