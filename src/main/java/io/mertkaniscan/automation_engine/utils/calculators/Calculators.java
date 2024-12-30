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

    public static double calculateKe(double Kr, double fw, double KcMax, double Kcb) {
        return Kr * (KcMax - Kcb) * fw;
    }

    public static double calculateKr(double evaporationDeficit, double TEW, double REW) {
        if (evaporationDeficit > TEW) {
            return 0.0;
        } else if (evaporationDeficit <= REW) {
            return 1.0;
        } else {
            return (TEW - evaporationDeficit) / (TEW - REW);
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
        double KcMaxBase = Kcb + 0.05;

        if (humidity < 40 || windSpeed > 5) {

            double windEffect = 0.04 * (windSpeed - 2);
            double humidityEffect = -0.004 * (humidity - 45);
            return KcMaxBase + windEffect + humidityEffect;
        }

        return KcMaxBase;
    }

    public static double calculateTEW(double FieldCapacity, double WiltingPoint, double Ze) {
        return (FieldCapacity - WiltingPoint) * Ze;
    }

    public static double calculateSensorTEW(double soilWaterPercentage, double wiltingPoint, double maxEvaporationDepth) {
        return Math.max(0, (soilWaterPercentage - wiltingPoint) * maxEvaporationDepth);
    }

    public static double calculateREW(double TEW, double p) {
        return TEW * p;
    }

    public static double calculateTAW(double SoilMoisture, double WiltingPoint, double RZD) {
        return (SoilMoisture - WiltingPoint) * RZD;
    }

    public static double calculateSensorTAW(double SensorMoisture, double WiltingPoint, double RZD) {
        return (SensorMoisture - WiltingPoint) * RZD;
    }

    public static double calculateRAW(double TAW, double AD) {
        return TAW * AD;
    }

    public static double calculateIrrigationInterval(double RAW, double ETc) {
        return RAW / ETc;
    }

}