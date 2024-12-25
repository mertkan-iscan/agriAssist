package io.mertkaniscan.automation_engine.services.forecast_services.elevation_service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ElevationResponse {
    @JsonProperty("elevation")
    private double[] elevation;

    public double[] getElevation() {
        return elevation;
    }

    public void setElevation(double[] elevation) {
        this.elevation = elevation;
    }

    public double getFirstElevation() {
        if (elevation != null && elevation.length > 0) {
            return elevation[0];
        }
        throw new IllegalStateException("Elevation data is empty");
    }
}