package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hours")
public class Hour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int hourID;

    @Column(nullable = false)
    private int hour; // Hour of the day (0-23)

    @Column(nullable = false)
    private Double ke; // Evaporation coefficient (Ke)

    @Column(name = "eto")
    private Double sensorEToHourly; // Reference evapotranspiration (ETo)

    @Column(nullable = true)
    private Double guessedEtoHourly;

    @ManyToOne
    @JoinColumn(name = "dayid", nullable = false)
    @JsonBackReference("day-hours")
    private Day day;

    public Hour() {
    }

    public Hour(int hour, Double ke, Double sensorEToHourly, Double guessedEtoHourly, Day day) {
        this.hour = hour;
        this.ke = ke;
        this.sensorEToHourly = sensorEToHourly;
        this.guessedEtoHourly = guessedEtoHourly;
        this.day = day;
    }
}
