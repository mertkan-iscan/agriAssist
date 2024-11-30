package io.mertkaniscan.automation_engine.components;

public class Calculators {

    public static double calculateFullETcDual(
            double Kcb, double T, double humidity, double pressure, double ghi, double dni, double dhi, double windSpeed,
            double wettedArea, double totalArea, double FieldCapacity, double WiltingPoint, double Ze, double DePrev,
            double dailyRainfall, double irrigation, double albedo) {

        // Step 1: Calculate Net Radiation (Rn)
        double Rn = calculateRn(ghi, dni, dhi, albedo);

        // Step 2: Calculate Saturation Vapor Pressure (es)
        double es = calculateSaturationVaporPressure(T);

        // Step 3: Calculate Delta (Slope of saturation vapor pressure curve)
        double delta = calculateDelta(es, T);

        // Step 4: Calculate Psychrometric constant (gamma)
        double gamma = calculateGamma(pressure);

        // Step 5: Calculate Reference ETo
        double ETo = calculateETo(T, humidity, pressure, ghi, dni, dhi, windSpeed, Rn, 0, es, es * (humidity / 100.0), delta, gamma);

        // Step 6: Calculate Total Evaporable Water (TEW)
        double TEW = calculateTEW(FieldCapacity, WiltingPoint, Ze);

        // Step 7: Calculate Readily Evaporable Water (REW)
        double REW = calculateREW(TEW);

        // Step 8: Calculate KcMax
        double KcMax = calculateKcMax(Kcb, humidity, windSpeed);

        // Step 9: Calculate Actual Evaporation (Eact)
        double fw = calculateFw(wettedArea, totalArea);
        double De = calculateDe(DePrev, dailyRainfall, irrigation, 0, TEW); // Eact initially set to 0 for preliminary De
        double Kr = calculateKr(De, TEW, REW);
        double Ke = calculateKe(Kcb, Kr, fw, KcMax, De, TEW, REW);
        double Eact = calculateEact(Ke, ETo);

        // Step 10: Recalculate De with Actual Evaporation (Eact)
        De = calculateDe(DePrev, dailyRainfall, irrigation, Eact, TEW);

        // Step 11: Calculate ETc dual
        return calculateETcDual(Kcb, Ke, ETo);
    }


    // Calculate ETc with dual crop coefficient
    public static double calculateETcDual(double Kcb, double Ke, double ETo) {
        return ETo * (Kcb + Ke); // ETc in mm/day
    }

    // Calculate Reference ETo
    public static double calculateETo(double T, double humidity, double pressure, double ghi, double dni, double dhi, double windSpeed,
                                      double Rn, double G, double es, double ea, double delta, double gamma) {
        double numerator = 0.408 * delta * (Rn - G) + gamma * (900 / (T + 273)) * windSpeed * (es - ea);
        double denominator = delta + gamma * (1 + 0.34 * windSpeed);
        return numerator / denominator; // ETo in mm/day
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
        return RAW / ETc; // Interval in days
    }

    // Calculate Net Radiation (Rn)
    public static double calculateRn(double ghi, double dni, double dhi, double albedo) {
        double shortwaveIncoming = ghi;
        double shortwaveReflected = albedo * shortwaveIncoming;
        return (shortwaveIncoming - shortwaveReflected);
    }

    // Calculate Saturation Vapor Pressure (es)
    public static double calculateSaturationVaporPressure(double T) {
        return 0.6108 * Math.exp((17.27 * T) / (T + 237.3));
    }

    // Calculate Delta (Slope of saturation vapor pressure curve)
    public static double calculateDelta(double es, double T) {
        return (4098 * es) / Math.pow((T + 237.3), 2);
    }

    // Calculate Psychrometric constant (gamma)
    public static double calculateGamma(double pressure) {
        return 0.000665 * pressure;
    }
}