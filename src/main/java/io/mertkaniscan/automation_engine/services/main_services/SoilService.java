package io.mertkaniscan.automation_engine.services.main_services;

import io.mertkaniscan.automation_engine.models.Field;
import org.springframework.stereotype.Service;

@Service
public class SoilService {

    public double getSoilMoisture(Field field) {
        // Fetch soil moisture from sensors or database
        return 0.15; // Example: 15% volumetric water content
    }

    public double getFieldCapacity(Field field) {
        // Return field capacity for the soil type
        return 0.25; // Example: 25%
    }

    public double getWiltingPoint(Field field) {
        // Return wilting point for the soil type
        return 0.12; // Example: 12%
    }
}