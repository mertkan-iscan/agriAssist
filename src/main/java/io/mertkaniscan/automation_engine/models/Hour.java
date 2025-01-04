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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hourid")
    private int hourID;

    @Column(name = "hour_index", nullable = false)
    private int hourIndex;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // evaporation
    @Column(name = "ke_value")
    private Double KeValue;

    @Column(name = "kr_value")
    private Double KrValue;

    // water
    @Column(name = "taw_value_hourly")
    private Double TAWValueHourly;

    @Column(name = "raw_value_hourly")
    private Double RAWValueHourly;

    @Column(name = "tew_value_hourly")
    private Double TEWValueHourly;

    @Column(name = "rew_value_hourly")
    private Double REWValueHourly;

    // ETo
    @Column(name = "forecast_eto_hourly")
    private Double forecastEToHourly;

    @Column(name = "sensor_eto_hourly")
    private Double sensorEToHourly;

    @Column(name = "guessed_eto_hourly")
    private Double guessedEtoHourly;

    // sensor
    @Column(name = "sensor_temperature")
    private Double sensorTemperature;

    @Column(name = "sensor_humidity")
    private Double sensorHumidity;

    // forecast
    @Column(name = "forecast_temperature")
    private Double forecastTemperature;

    @Column(name = "forecast_humidity")
    private Double forecastHumidity;

    @Column(name = "forecast_wind_speed")
    private Double forecastWindSpeed;

    @Column(name = "forecast_precipitation")
    private Double forecastPrecipitation;

    @Column(name = "happened_precipitation")
    private Double happenedPrecipitation;

    @Column(name = "solar_radiation")
    private Double solarRadiation;

    @Column(name = "de_value")
    private Double DeValue;

    @Column(name = "hourly_depletion")
    private Double hourlyDepletion;

    @Column(name = "calculated_kcb_adjusted")
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
