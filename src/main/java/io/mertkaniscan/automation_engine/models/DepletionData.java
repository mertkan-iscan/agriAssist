package io.mertkaniscan.automation_engine.models;

import java.time.LocalDateTime;

public class DepletionData {

    private Double depletion; // D value
    private LocalDateTime lastUpdated;

    public DepletionData(Double depletion, LocalDateTime lastUpdated) {
        this.depletion = depletion;
        this.lastUpdated = lastUpdated;
    }

    public Double getDepletion() {
        return depletion;
    }

    public void setDepletion(Double depletion) {
        this.depletion = depletion;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
