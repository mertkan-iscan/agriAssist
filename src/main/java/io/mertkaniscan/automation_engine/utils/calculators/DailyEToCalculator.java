package io.mertkaniscan.automation_engine.utils.calculators;

import org.springframework.stereotype.Component;

@Component
public class DailyEToCalculator {

    private static final double ALBEDO = 0.23;
    private static final double STEFAN_BOLTZMANN = 4.903e-9; // MJ/K⁴/m²/day

    /**
     * Calculates daily reference evapotranspiration (ETo) using the FAO-56 Penman-Monteith equation.
     *
     * @param Tmax         Maximum daily temperature (°C)
     * @param Tmin         Minimum daily temperature (°C)
     * @param ghi          Global horizontal irradiance (Wh/m²/day)
     * @param windSpeed    Average daily wind speed at 2m height (m/s)
     * @param humidity     Relative humidity (%)
     * @param latitude     Geographical latitude (decimal degrees)
     * @param elevation     Elevation above sea level (meters)
     * @param pressureHpa  Atmospheric pressure (hPa or mbar)
     * @param dayOfYear    Day of year (1-365)
     * @return            Reference evapotranspiration ETo (mm/day)
     */
    public static double calculateEToDaily(double Tmax,
                                           double Tmin,
                                           double ghi,
                                           double windSpeed,
                                           double humidity,
                                           double latitude,
                                           double elevation,
                                           double pressureHpa,
                                           int dayOfYear) {

        // Convert GHI from Wh/m²/day to MJ/m²/day
        double ghiMJ = ghi * 0.0036;

        // Convert pressure from hPa to kPa
        double pressureKpa = pressureHpa / 10.0;

        final double gamma = 0.665e-3 * pressureKpa;

        final double delta = (4098 * (0.6108 * Math.exp((17.27 * ((Tmax + Tmin) / 2)) /
                ((Tmax + Tmin) / 2 + 237.3)))) / Math.pow(((Tmax + Tmin) / 2 + 237.3), 2);

        double es = 0.6108 * Math.exp((17.27 * Tmax) / (Tmax + 237.3));
        double ea = es * (humidity / 100.0);
        double vpd = es - ea;

        double Rso = calculateRso(latitude, elevation, dayOfYear);
        double netRadiation = calculateDetailedNetRadiation(Tmax, Tmin, ea, ghiMJ, Rso);

        double tempTerm = (900 / ((Tmax + Tmin) / 2 + 273)) * windSpeed * vpd;

        return (0.408 * delta * netRadiation + gamma * tempTerm) / (delta + gamma * (1 + 0.34 * windSpeed));
    }

    public static double calculateDetailedNetRadiation(double Tmax, double Tmin, double ea,
                                                       double ghiMJ, double Rso) {

        double netShortwave = calculateNetShortwave(ghiMJ); // ghi already in MJ/m²/day

        double netLongwave = calculateNetLongwaveRadiation(Tmax, Tmin, ea, ghiMJ, Rso);

        return netShortwave - netLongwave;
    }

    public static double calculateNetShortwave(double ghi) {
        // ghi should already be in MJ/m²/day
        return ghi * (1 - ALBEDO);
    }

    public static double calculateNetLongwaveRadiation(double Tmax, double Tmin, double ea,
                                                       double Rs, double Rso) {
        double TmaxK = Tmax + 273.16;
        double TminK = Tmin + 273.16;
        double tempAvgPower4 = (Math.pow(TmaxK, 4) + Math.pow(TminK, 4)) / 2;

        double radiationRatio = Math.min(Rs / Rso, 1.0);

        return STEFAN_BOLTZMANN * tempAvgPower4 *
                (0.34 - 0.14 * Math.sqrt(ea)) *
                (1.35 * radiationRatio - 0.35);
    }

    public static double calculateRso(double latitude, double altitude, int dayOfYear) {
        final double GSC = 0.0820;
        double phi = Math.toRadians(latitude);
        double delta = 0.409 * Math.sin((2 * Math.PI * dayOfYear / 365.0) - 1.39);
        double dr = 1 + 0.033 * Math.cos(2 * Math.PI * dayOfYear / 365.0);
        double omega_s = Math.acos(-Math.tan(phi) * Math.tan(delta));

        double Ra = (24 * 60 / Math.PI) * GSC * dr *
                (omega_s * Math.sin(phi) * Math.sin(delta) +
                        Math.cos(phi) * Math.cos(delta) * Math.sin(omega_s));

        return (0.75 + 2 * Math.pow(10, -5) * altitude) * Ra;
    }
}