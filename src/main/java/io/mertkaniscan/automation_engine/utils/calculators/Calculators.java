package io.mertkaniscan.automation_engine.utils.calculators;

import org.springframework.stereotype.Component;

@Component
public class Calculators {

    public static double calculateETcDual(double Kcb, double Ke, double ETo) {
        return ETo * (Kcb + Ke);
    }

    public static double calculateVPD(double temperature, double humidity) {

        double saturationVaporPressure = 0.6108 * Math.exp((17.27 * temperature) / (temperature + 237.3));
        double actualVaporPressure = saturationVaporPressure * (humidity / 100.0);

        return saturationVaporPressure - actualVaporPressure;
    }

    public static double calculateKe(double Kr,
                                     double fw,
                                     double KcMax,
                                     double Kcb) {

        return Kr * (KcMax - Kcb) * fw;
    }

//    public static double calculateKeFao(double Kr,
//                                     double fw,
//                                     double KcMax,
//                                     double Kcb) {
//
//        return Math.min(Kr * (KcMax - Kcb), fe * KcMax);
//    }

    public static double calculateKr(double evaporationDeficit, double TEW, double REW) {
        if (evaporationDeficit > TEW) {
            return 0.0;
        } else if (evaporationDeficit <= REW) {
            return 1.0;
        } else {
            return (TEW - evaporationDeficit) / (TEW - REW);
        }
    }

    public static double calculateSensorKr(double currentREW, double soilMoistPercent, double fieldCapacity, double wiltingPoint) {
        if (currentREW > 0) {
            return 1.0; // Kr is 1 if REW > 0
        } else {

            return soilMoistPercent / (fieldCapacity - wiltingPoint);
        }
    }

    public static double calculateFw(double wettedArea, double totalArea) {
        if (totalArea == 0) {
            throw new IllegalArgumentException("Total area cannot be zero.");
        }
        return Math.min(Math.max(wettedArea / totalArea, 0.0), 1.0);
    }

    public static double calculateDe(double DePrev, double dailyRainfall, double irrigation, double Eact, double TEW) {
        double De = DePrev + Eact - dailyRainfall - irrigation;
        return Math.min(De, TEW);
    }

    public static double calculateETc(double Ke, double ETo) {
        if (Ke < 0 || ETo < 0) {
            throw new IllegalArgumentException("Ke and ETo must be non-negative values.");
        }
        return Ke * ETo;
    }

    public static double calculateKcMax(double Kcb, double humidity, double windSpeed) {
        // Başlangıç değeri
        double KcMaxBase = Kcb + 0.05;

        // Rüzgar ve nem etkileri
        double windEffect = 0.0;
        double humidityEffect = 0.0;

        if (windSpeed > 2) { // Rüzgar hızı etkisi 2 m/s üzerindeyse
            windEffect = 0.04 * (windSpeed - 2);
        }

        if (humidity < 45) { // Nem etkisi 45%'in altındaysa
            humidityEffect = -0.004 * (humidity - 45);
        }

        // KcMax hesaplama
        double KcMax = KcMaxBase + windEffect + humidityEffect;

        // Mantıksal sınırlar
        KcMax = Math.max(0.8, Math.min(1.4, KcMax)); // KcMax değeri [0.8, 1.4] arasında sınırlandırılır

        return KcMax;
    }

    public static double calculateKcbAdjusted(double Kcb, double humidity, double windSpeed, double plantHeight) {

        double windEffect = 0.04 * (windSpeed - 2);
        double humidityEffect = -0.004 * (humidity - 45);
        double heightEffect = Math.pow(plantHeight/3,0.3);

        return Kcb + (windEffect + humidityEffect) * heightEffect;
    }

    public static double calculateTEW(double FieldCapacity, double WiltingPoint, double Ze) {
        return (FieldCapacity - WiltingPoint) * Ze * 1000;
    }

    public static double calculateSensorTEW(double SensorMoisture, double wiltingPoint, double maxEvaporationDepth) {
        return Math.max(0, ((SensorMoisture / 100) - (0.5 * wiltingPoint)) * maxEvaporationDepth * 1000);
    }

    public static double calculateSensorREW(double TEW, double evaporationCoeff) {
        return TEW * evaporationCoeff;
    }

    public static double calculateTAW(double FieldCapacity, double WiltingPoint, double RZD) {
        return (FieldCapacity - WiltingPoint) * RZD * 1000;
    }

    public static double calculateSensorTAW(double SensorMoisture, double WiltingPoint, double RZD) {
        System.out.println("RZD: " + RZD);
        return ((SensorMoisture / 100) - WiltingPoint) * RZD * 1000;
    }

    public static double calculateSensorRAW(double TAW, double allowableDepletion) {
        return TAW * allowableDepletion;
    }

    public static double calculateAdjustedAllowableDepletion(double standardP, double etc) {
        // Validate input
        if (standardP < 0 || standardP > 1) {
            throw new IllegalArgumentException("Standard p must be between 0 and 1.");
        }
        if (etc < 0) {
            throw new IllegalArgumentException("ETc cannot be negative.");
        }

        // Adjust p value using the formula
        double adjustedP = standardP + 0.04 * (5 - etc);

        // Ensure p is within valid range [0, 1]
        return Math.max(0, Math.min(1, adjustedP));
    }

    public static double calculateIrrigationInterval(double RAW, double ETc) {
        return RAW / ETc;
    }
}