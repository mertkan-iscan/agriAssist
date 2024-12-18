package io.mertkaniscan.automation_engine.utils.config_loader;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class PlantConfig {

    // Getters and Setters
    private String plantType;       // Plant type (e.g., LETTUCE, TOMATO)
    private double rootZoneDepth;   // Root zone depth (m)
    private double allowableDepletion; // Allowable depletion rate (0-1)
    private KcValues kcValues;      // Kc values for different growth stages
    private List<String> stages;    // List of plant-specific growth stages

    // Nested KcValues class
    @Setter
    @Getter
    public static class KcValues {
        private double kcInit;      // Kc value for initial stage
        private double kcMid;       // Kc value for mid stage
        private double kcLate;      // Kc value for late stage

        @Override
        public String toString() {
            return "KcValues{" +
                    "kcInit=" + kcInit +
                    ", kcMid=" + kcMid +
                    ", kcLate=" + kcLate +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "PlantConfig{" +
                "plantType='" + plantType + '\'' +
                ", rootZoneDepth=" + rootZoneDepth +
                ", allowableDepletion=" + allowableDepletion +
                ", kcValues=" + kcValues +
                ", stages=" + stages +
                '}';
    }
}
