package io.mertkaniscan.automation_engine.services.logic;

import io.mertkaniscan.automation_engine.models.DepletionData;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class DepletionService {

    private DepletionData depletionData;

    // Constructor initializes D to a default value
    public DepletionService() {
        this.depletionData = new DepletionData(0, LocalDateTime.now());
    }

    // Method to calculate D based on FAO formula
    public synchronized void calculateDepletion() {
        // Fetch current values for soil moisture, etc. (Mocked here)
        double previousD = depletionData.getDepletion();
        double evapotranspiration = 5.0; // Example ET value, replace with real inputs
        double rainfall = 3.0; // Example Rainfall value, replace with real inputs

        // FAO Depletion Formula (mock example: D = D_prev - Rainfall + ET)
        double newDepletion = Math.max(0, previousD - rainfall + evapotranspiration);

        // Update depletion data
        depletionData.setDepletion(newDepletion);
        depletionData.setLastUpdated(LocalDateTime.now());

        System.out.println("Depletion updated: D = " + newDepletion);
    }

    // Method to retrieve the latest D value
    public DepletionData getDepletionData() {
        return this.depletionData;
    }
}
