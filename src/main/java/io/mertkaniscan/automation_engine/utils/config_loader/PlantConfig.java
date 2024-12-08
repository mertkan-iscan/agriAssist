package io.mertkaniscan.automation_engine.utils.config_loader;

import java.util.List;

public class PlantConfig {

    private String plantType;       // Plant type (e.g., LETTUCE, TOMATO)
    private double rootZoneDepth;   // Root zone depth (m)
    private double allowableDepletion; // Allowable depletion rate (0-1)
    private KcValues kcValues;      // Kc values for different growth stages
    private List<String> stages;    // List of plant-specific growth stages

    // Nested KcValues class
    public static class KcValues {
        private double kcInit;      // Kc value for initial stage
        private double kcMid;       // Kc value for mid stage
        private double kcLate;      // Kc value for late stage

        public double getKcInit() {
            return kcInit;
        }

        public void setKcInit(double kcInit) {
            this.kcInit = kcInit;
        }

        public double getKcMid() {
            return kcMid;
        }

        public void setKcMid(double kcMid) {
            this.kcMid = kcMid;
        }

        public double getKcLate() {
            return kcLate;
        }

        public void setKcLate(double kcLate) {
            this.kcLate = kcLate;
        }

        @Override
        public String toString() {
            return "KcValues{" +
                    "kcInit=" + kcInit +
                    ", kcMid=" + kcMid +
                    ", kcLate=" + kcLate +
                    '}';
        }
    }

    // Getters and Setters
    public String getPlantType() {
        return plantType;
    }

    public void setPlantType(String plantType) {
        this.plantType = plantType;
    }

    public double getRootZoneDepth() {
        return rootZoneDepth;
    }

    public void setRootZoneDepth(double rootZoneDepth) {
        this.rootZoneDepth = rootZoneDepth;
    }

    public double getAllowableDepletion() {
        return allowableDepletion;
    }

    public void setAllowableDepletion(double allowableDepletion) {
        this.allowableDepletion = allowableDepletion;
    }

    public KcValues getKcValues() {
        return kcValues;
    }

    public void setKcValues(KcValues kcValues) {
        this.kcValues = kcValues;
    }

    public List<String> getStages() {
        return stages;
    }

    public void setStages(List<String> stages) {
        this.stages = stages;
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
