package io.mertkaniscan.automation_engine.components.config_loader;

public class PlantConfig {

    private String plantType;       // Bitki türü (ör. LETTUCE, TOMATO)
    private double rootZoneDepth;   // Kök bölgesi derinliği (m)
    private double allowableDepletion; // Kullanılabilir su kaybı oranı (0-1 arasında)
    private KcValues kcValues;      // Bitki büyüme evrelerine göre Kc değerleri

    // İç içe geçmiş KcValues sınıfı
    public static class KcValues {
        private double kcInit;      // Başlangıç evresi için Kc
        private double kcMid;       // Orta evre için Kc
        private double kcLate;      // Geç evre için Kc

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

    @Override
    public String toString() {
        return "PlantConfig{" +
                "plantType='" + plantType + '\'' +
                ", rootZoneDepth=" + rootZoneDepth +
                ", allowableDepletion=" + allowableDepletion +
                ", kcValues=" + kcValues +
                '}';
    }
}
