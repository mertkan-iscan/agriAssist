package io.mertkaniscan.automation_engine.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

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
    private BigDecimal vpd; // vapour pressure deficit

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Hour> hours;

    // Many-to-One relationship, many days belong to one plant
    @ManyToOne
    @JoinColumn(name = "plantID", nullable = false)
    private Plant plant;

    public Day() {
    }

    public Day(Timestamp date, Timestamp sunrise, Timestamp sunset, BigDecimal vpd, Plant plant) {
        this.date = date;
        this.sunrise = sunrise;
        this.sunset = sunset;
        this.vpd = vpd;
        this.plant = plant;
    }

    // Getters and Setters
    public int getDayID() {
        return dayID;
    }

    public void setDayID(int dayID) {
        this.dayID = dayID;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public Timestamp getSunrise() {
        return sunrise;
    }

    public void setSunrise(Timestamp sunrise) {
        this.sunrise = sunrise;
    }

    public Timestamp getSunset() {
        return sunset;
    }

    public void setSunset(Timestamp sunset) {
        this.sunset = sunset;
    }

    public BigDecimal getVpd() {
        return vpd;
    }

    public void setVpd(BigDecimal vpd) {
        this.vpd = vpd;
    }

    public List<Hour> getHours() {
        return hours;
    }

    public void setHours(List<Hour> hours) {
        this.hours = hours;
    }

    public Plant getPlant() {
        return plant;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
    }
}