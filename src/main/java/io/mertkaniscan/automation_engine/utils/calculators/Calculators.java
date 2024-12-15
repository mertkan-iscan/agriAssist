package io.mertkaniscan.automation_engine.utils.calculators;

import org.springframework.stereotype.Component;

@Component
public class Calculators {

    private static final double ALBEDO = 0.23; // Surface albedo value
    private static final double STEFAN_BOLTZMANN = 5.67e-8; // Stefan-Boltzmann constant (W/m^2/K^4)
    private static final double SOIL_EMISSIVITY = 0.95;  // Typical soil emissivity
    private static final double AIR_EMISSIVITY = 0.7;

    // Calculate ETc with dual crop coefficient
    public static double calculateETcDual(double Kcb, double Ke, double ETo) {
        return ETo * (Kcb + Ke); // ETc in mm/day
    }

    public static double calculateVPD(double temperature, double humidity) {

        double saturationVaporPressure = 0.6108 * Math.exp((17.27 * temperature) / (temperature + 237.3));
        double actualVaporPressure = saturationVaporPressure * (humidity / 100.0);

        return saturationVaporPressure - actualVaporPressure;
    }

    // Calculate evaporation reduction coefficient Ke
    public static double calculateKe(double Kcb, double Kr, double fw, double KcMax, double De, double TEW, double REW) {
        return Math.min(Kr * KcMax, fw * KcMax);
    }

    // Calculate evaporation reduction coefficient Kr
    public static double calculateKr(double De, double TEW, double REW) {
        if (De > TEW) {
            return 0.0;
        } else if (De <= REW) {
            return 1.0;
        } else {
            return (TEW - De) / (TEW - REW);
        }
    }

    // Calculate wetted area fraction (fw)
    public static double calculateFw(double wettedArea, double totalArea) {
        if (totalArea == 0) {
            throw new IllegalArgumentException("Total area cannot be zero.");
        }
        return Math.min(Math.max(wettedArea / totalArea, 0.0), 1.0);
    }

    // Calculate depletion (De)
    public static double calculateDe(double DePrev, double dailyRainfall, double irrigation, double Eact, double TEW) {
        double De = DePrev + Eact - dailyRainfall - irrigation;
        return Math.min(De, TEW);
    }

    // Calculate actual evaporation (Eact)
    public static double calculateEact(double Ke, double ETo) {
        if (Ke < 0 || ETo < 0) {
            throw new IllegalArgumentException("Ke and ETo must be non-negative values.");
        }
        return Ke * ETo;
    }

    // Calculate Kc max
    public static double calculateKcMax(double Kcb, double humidity, double windSpeed) {
        double KcMaxBase = Kcb + 0.05;
        if (humidity < 40 || windSpeed > 5) {
            return KcMaxBase + (0.04 * (windSpeed - 2)) - (0.004 * (humidity - 45));
        }
        return KcMaxBase;
    }

    // Calculate Total Evaporable Water (TEW)
    public static double calculateTEW(double FieldCapacity, double WiltingPoint, double Ze) {
        return (FieldCapacity - WiltingPoint) * Ze;
    }

    // Calculate Readily Evaporable Water (REW)
    public static double calculateREW(double TEW) {
        return Math.min(TEW / 2, 20); // Capped at 20 mm
    }

    // Calculate Total Available Water (TAW)
    public static double calculateTAW(double FieldCapacity, double WiltingPoint, double RZD) {
        return (FieldCapacity - WiltingPoint) * RZD;
    }

    // Calculate Readily Available Water (RAW)
    public static double calculateRAW(double TAW, double AD) {
        return TAW * AD;
    }

    // Calculate Irrigation Interval
    public static double calculateIrrigationInterval(double RAW, double ETc) {
        return RAW / ETc;
    }

}