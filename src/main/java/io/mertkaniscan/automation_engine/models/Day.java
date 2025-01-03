package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mertkaniscan.automation_engine.services.forecast_services.solar_forecast_service.SolarResponse;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherResponse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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
    private Double evaporationWaterLoss;
    @Column
    private Double plantWaterLoss;

    @Column
    private Double guessedEtoDaily;

    @Column
    private Double TAWValueDaily;
    @Column
    private Double RAWValueDaily;
    @Column
    private Double TEWValueDaily;
    @Column
    private Double REWValueDaily;

    @Column
    private Double dailyDepletion;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference("day-hours")
    private List<Hour> hours = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "plantID", nullable = false)
    @JsonBackReference("plant-days")
    private Plant plant;

    // JSON field for SolarResponse
    @Column(columnDefinition = "json")
    private String solarResponseJson;

    // JSON field for WeatherResponse
    @Column(columnDefinition = "json")
    private String weatherResponseJson;

    public Day() {
    }

    // Utility methods for SolarResponse
    public SolarResponse getSolarResponse() {
        if (this.solarResponseJson != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(this.solarResponseJson, SolarResponse.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize SolarResponse JSON", e);
            }
        }
        return null;
    }

    public void setSolarResponse(SolarResponse solarResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.solarResponseJson = objectMapper.writeValueAsString(solarResponse);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize SolarResponse to JSON", e);
        }
    }

    // Utility methods for WeatherResponse
    public WeatherResponse getWeatherResponse() {
        if (this.weatherResponseJson != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(this.weatherResponseJson, WeatherResponse.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize WeatherResponse JSON", e);
            }
        }
        return null;
    }

    public void setWeatherResponse(WeatherResponse weatherResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.weatherResponseJson = objectMapper.writeValueAsString(weatherResponse);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize WeatherResponse to JSON", e);
        }
    }

    // Enhanced getter for SolarResponse
    public SolarResponse getSolarResponseObject() {
        if (this.solarResponseJson == null || this.solarResponseJson.isEmpty()) {
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(this.solarResponseJson, SolarResponse.class);
        } catch (IOException e) {
            log.error("Error deserializing SolarResponse JSON for Day ID {}: {}", this.dayID, e.getMessage());
            throw new RuntimeException("Failed to deserialize SolarResponse JSON", e);
        }
    }

    // Enhanced getter for WeatherResponse
    public WeatherResponse getWeatherResponseObject() {
        if (this.weatherResponseJson == null || this.weatherResponseJson.isEmpty()) {
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(this.weatherResponseJson, WeatherResponse.class);
        } catch (IOException e) {
            log.error("Error deserializing WeatherResponse JSON for Day ID {}: {}", this.dayID, e.getMessage());
            throw new RuntimeException("Failed to deserialize WeatherResponse JSON", e);
        }
    }
}
