package io.mertkaniscan.automation_engine.models;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "hours")
public class Hour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int hourID;

    @Column(nullable = false)
    private int hour; // Hour of the day (0-23)

    @Column(nullable = false)
    private BigDecimal ke; // Evaporation coefficient (Ke)

    @Column(nullable = false)
    private BigDecimal eto; // Reference evapotranspiration (ETo)

    @ManyToOne
    @JoinColumn(name = "dayid", nullable = false)
    private Day day;

    public Hour() {
    }

    public Hour(int hour, BigDecimal ke, BigDecimal eto, Day day) {
        this.hour = hour;
        this.ke = ke;
        this.eto = eto;
        this.day = day;
    }

    // Getters and Setters
    public int getHourID() {
        return hourID;
    }

    public void setHourID(int hourID) {
        this.hourID = hourID;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public BigDecimal getKe() {
        return ke;
    }

    public void setKe(BigDecimal ke) {
        this.ke = ke;
    }

    public BigDecimal getEto() {
        return eto;
    }

    public void setEto(BigDecimal eto) {
        this.eto = eto;
    }

    public Day getDay() {
        return day;
    }

    public void setDay(Day day) {
        this.day = day;
    }
}
