package io.mertkaniscan.automation_engine.utils.calculators;

public class HourlyEToCalculator {
    private static final double ALBEDO = 0.23;
    private static final double STEFAN_BOLTZMANN = 4.903e-9; // MJ/K⁴/m²/day

    /**
     * Calculates hourly reference evapotranspiration (ETo) using the FAO-56 Penman-Monteith equation.
     *
     * @param temp         Current air temperature (°C)
     * @param humidity     Current relative humidity (%)
     * @param windSpeed    Current wind speed at 2m height (m/s)
     * @param latitude     Geographical latitude (decimal degrees)
     * @param elevation     Elevation above sea level (meters)
     * @param dayOfYear    Day of year (1-365)
     * @param hour        Hour of the day (0-23)
     * @param radiation    Solar radiation (W/m²)
     * @param pressureHpa  Atmospheric pressure (hPa or mbar)
     * @param isDaytime    Boolean indicating whether it is daytime (true) or nighttime (false)
     * @return            Reference evapotranspiration ETo (mm/hour)
     */
    public static double calculateEToHourly(double temp, double humidity, double windSpeed,
                                            double latitude, double elevation, int dayOfYear, int hour,
                                            double radiation, double pressureHpa, boolean isDaytime) {

        // Convert radiation from W/m² to MJ/m²/hour
        double radiationMJ = radiation * 0.0036;

        // Convert pressure from hPa to kPa
        double pressureKpa = pressureHpa / 10.0;

        // Psychrometric constant (kPa/°C)
        double gamma = 0.665e-3 * pressureKpa;

        // Vapor pressure calculations
        double es = 0.6108 * Math.exp((17.27 * temp) / (temp + 237.3));
        double ea = es * (humidity / 100.0);
        double vpd = es - ea;

        // Slope of saturation vapor pressure curve (kPa/°C)
        double delta = (4098 * (0.6108 * Math.exp((17.27 * temp) / (temp + 237.3))))
                / Math.pow((temp + 237.3), 2);

        // Calculate hourly clear sky radiation
        double Rso = calculateHourlyRso(latitude, elevation, dayOfYear, hour);

        // Net radiation
        double Rn = calculateHourlyNetRadiation(radiationMJ, temp, ea, Rso);

        // Soil heat flux (MJ/m²/hour)
        double G = calculateHourlySoilHeatFlux(Rn, isDaytime);

        // Temperature term for hourly ETo
        double tempTerm = (37 / (temp + 273)) * windSpeed * vpd;

        // Final hourly ETo calculation (mm/hour)
        return (0.408 * delta * (Rn - G) + gamma * tempTerm) /
                (delta + gamma * (1 + 0.34 * windSpeed));
    }

    public static double calculateHourlyRso(double latitude, double altitude,
                                            int dayOfYear, int hour) {
        final double GSC = 0.0820; // Solar constant (MJ/m²/min)

        // Convert latitude to radians
        double phi = Math.toRadians(latitude);

        // Solar declination
        double delta = 0.409 * Math.sin(2 * Math.PI * dayOfYear / 365 - 1.39);

        // Inverse relative distance Earth-Sun
        double dr = 1 + 0.033 * Math.cos(2 * Math.PI * dayOfYear / 365);

        // Solar time angle at midpoint of hourly period
        double t = hour + 0.5; // Midpoint of hour
        double omega = (Math.PI/12) * (t - 12);

        // Solar zenith angle
        double cosZ = Math.sin(phi) * Math.sin(delta) +
                Math.cos(phi) * Math.cos(delta) * Math.cos(omega);

        if (cosZ <= 0) {
            return 0.0; // Night time
        }

        // Hourly extraterrestrial radiation
        double Ra = (12 * 60 / Math.PI) * GSC * dr * cosZ;

        // Clear sky radiation
        return (0.75 + 2 * Math.pow(10, -5) * altitude) * Ra;
    }

    private static double calculateHourlyNetRadiation(double radiation, double temp,
                                                      double ea, double Rso) {
        // Net shortwave radiation (MJ/m²/hour)
        double Rns = radiation * (1 - ALBEDO);

        // Net longwave radiation (MJ/m²/hour)
        double TK = temp + 273.16;
        double radiationRatio = Math.min(radiation / Math.max(Rso, 0.0001), 1.0);

        double Rnl = (STEFAN_BOLTZMANN / 24) * Math.pow(TK, 4) *
                (0.34 - 0.14 * Math.sqrt(ea)) *
                (1.35 * radiationRatio - 0.35);

        return Rns - Rnl;
    }

    private static double calculateHourlySoilHeatFlux(double Rn, boolean isDaytime) {
        return isDaytime ? 0.1 * Rn : 0.5 * Rn;
    }

    public static double convertPressureFromAltitude(double altitude) {
        return 101.3 * Math.pow((293 - 0.0065 * altitude) / 293, 5.26);
    }
}