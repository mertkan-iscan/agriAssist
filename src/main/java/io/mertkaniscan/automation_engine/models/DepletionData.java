package io.mertkaniscan.automation_engine.models;

import java.time.LocalDateTime;

public class DepletionData {
    private double depletion; // D value
    private LocalDateTime lastUpdated;

    // Constructor
    public DepletionData(double depletion, LocalDateTime lastUpdated) {
        this.depletion = depletion;
        this.lastUpdated = lastUpdated;
    }

    // Getters and Setters
    public double getDepletion() {
        return depletion;
    }

    public void setDepletion(double depletion) {
        this.depletion = depletion;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
