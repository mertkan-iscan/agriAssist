package io.mertkaniscan.automation_engine.utils.calculators;

public class HourlyEToCalculator {
    private static final double ALBEDO = 0.23;
    // Stefan-Boltzmann constant in FAO-56 units [MJ K^-4 m^-2 day^-1]
    // Dividing by 24 converts day^-1 → hour^-1 for net longwave calculation.
    private static final double STEFAN_BOLTZMANN_DAILY = 4.903e-9;
    private static final double STEFAN_BOLTZMANN_HOURLY = STEFAN_BOLTZMANN_DAILY / 24.0;

    /**
     * Calculates hourly reference evapotranspiration (ETo) using the FAO-56
     * Penman–Monteith equation (hourly version).
     *
     * @param temp         Current air temperature (°C)
     * @param humidity     Current relative humidity (%)
     * @param ghi          Measured global horizontal irradiance (W/m²)
     * @param windSpeed    Wind speed at 2 m (m/s)
     * @param latitude     Latitude (+ north, − south) in decimal degrees
     * @param elevation    Elevation above mean sea level (m)
     * @param pressureHpa  Atmospheric pressure (hPa or mbar)
     * @param dayOfYear    Day of year (1–365 or 366)
     * @param hour         Hour of the day (0–23)
     * @param isDaytime    True if sun is above horizon
     * @return             Hourly reference evapotranspiration ETo (mm/hour)
     */
    public static double calculateEToHourly(double temp,
                                            double humidity,
                                            double ghi,
                                            double windSpeed,
                                            double latitude,
                                            double elevation,
                                            double pressureHpa,
                                            int dayOfYear,
                                            int hour,
                                            boolean isDaytime) {

        // 1. Convert solar irradiance from W/m² to MJ/m² per hour
        //    1 W/m² = 0.0036 MJ/m²/hour
        double radiationMJ = ghi * 0.0036;

        // 2. Convert pressure from hPa to kPa
        double pressureKpa = pressureHpa / 10.0;

        // 3. Psychrometric constant (kPa/°C)
        double gamma = 0.000665 * pressureKpa;  // gamma = 0.665e-3 * P (kPa)

        // 4. Saturation vapor pressure (es), actual vapor pressure (ea), VPD
        double es = saturationVaporPressure(temp);         // kPa
        double ea = es * (humidity / 100.0);               // kPa
        double vpd = es - ea;                              // kPa

        // 5. Slope of saturation vapor pressure curve (delta)
        double delta = slopeOfSaturationPressureCurve(temp);

        // 6. Compute clear-sky radiation Rso (MJ/m²/hour)
        //    using an hour-angle integration approach below
        double Rso = Math.max(calculateHourlyRso(latitude, elevation, dayOfYear, hour), 0.01);

        // 7. Net radiation [Rn = Rns - Rnl]
        double Rn = calculateHourlyNetRadiation(radiationMJ, Rso, temp, ea);

        // 8. Soil heat flux (G). For hourly steps, a common approximation:
        double G = isDaytime ? 0.1 * Rn : 0.5 * Rn;
        // If you prefer to ignore G at hourly scale, just set G=0.

        // 9. Penman–Monteith: ETo (mm/hour)
        //    Using "37" instead of "900" because we are in hourly timescale.
        double numerator   = 0.408 * delta * (Rn - G)
                + gamma * (37.0 / (temp + 273.0)) * windSpeed * vpd;
        double denominator = delta + gamma * (1.0 + 0.34 * windSpeed);

        return numerator / denominator;  // mm/hour
    }


    /**
     * Computes the hourly clear-sky solar radiation (Rso) in MJ/m²/hour
     * by integrating between the hour boundaries.  This is more
     * accurate than a single-point cos(θ) approximation.
     */
    public static double calculateHourlyRso(double latDeg,
                                            double altitude,
                                            int dayOfYear,
                                            int hour) {
        // Constants
        final double GSC = 0.0820; // MJ m^-2 min^-1 (solar constant)
        double phi  = Math.toRadians(latDeg);
        double delta = solarDeclination(dayOfYear);
        double dr    = inverseRelativeDistanceEarthSun(dayOfYear);

        // Hour boundaries (in radians).
        // For hour "hour" [e.g., 0–1, 1–2, ...], we treat midpoint as hour+0.5
        // But to integrate, define w1, w2 for the boundaries:
        //   w1 = omega at hour
        //   w2 = omega at hour+1
        double w1 = hourAngle(hour,     12.0);  // rad
        double w2 = hourAngle(hour + 1, 12.0);  // rad

        // If the sun is entirely below horizon, Ra = 0
        // If partially above horizon, integrate over the portion above horizon
        double Ra = calcHourlyRa(phi, delta, dr, w1, w2);

        // Clear-sky radiation
        // Rso = (0.75 + 2e-5 * altitude)*Ra   [FAO-56 eqn. 37]
        double Rso = (0.75 + 2.0e-5 * altitude) * Ra;
        return Math.max(Rso, 0.0);
    }


    /**
     * Net radiation [Rn = Rns - Rnl], in MJ/m²/hour.
     *
     * @param Rs   Measured or estimated incoming shortwave (MJ/m²/hour)
     * @param Rso  Clear-sky shortwave (MJ/m²/hour)
     * @param temp Air temperature (°C)
     * @param ea   Actual vapor pressure (kPa)
     * @return     Net radiation (MJ/m²/hour)
     */
    private static double calculateHourlyNetRadiation(double Rs,
                                                      double Rso,
                                                      double temp,
                                                      double ea) {
        // 1. Net shortwave radiation (Rns)
        double Rns = (1.0 - ALBEDO) * Rs;

        // 2. Net longwave radiation (Rnl)
        //    Using FAO-56 eqn. for hourly:
        //    sigma_hourly = 4.903e-9 / 24 = 2.04e-10 (MJ K^-4 m^-2 hour^-1)
        //    T in Kelvin
        double tKelvin = temp + 273.16;

        // Rs/Rso ratio.  Typically not clamped unless you want to
        // limit it to [0..1.0], but standard FAO-56 does not clamp it.
        double ratio = Rso < 1e-6 ? 0.0 : (Rs / Rso);

        // Rnl = sigma * T^4 * (0.34 - 0.14√ea) * [1.35 (Rs/Rso) - 0.35]
        double Rnl = STEFAN_BOLTZMANN_HOURLY
                * Math.pow(tKelvin, 4.0)
                * (0.34 - 0.14 * Math.sqrt(ea))
                * (1.35 * ratio - 0.35);

        // 3. Net radiation
        return Rns - Math.max(Rnl, 0.0);
    }


    /* ----------------------------------------------------------
     * HELPER FUNCTIONS FOR SATURATION VAPOR PRESSURE ETC.
     * ----------------------------------------------------------
     */

    /**
     * Saturation vapor pressure es(T). Returns kPa.
     */
    private static double saturationVaporPressure(double tempC) {
        return 0.6108 * Math.exp((17.27 * tempC) / (tempC + 237.3));
    }

    /**
     * Slope d(es)/dT at temperature T (°C). Returns kPa/°C.
     */
    private static double slopeOfSaturationPressureCurve(double tempC) {
        double es = saturationVaporPressure(tempC);
        return (4098.0 * es) / Math.pow(tempC + 237.3, 2.0);
    }

    /**
     * Solar declination (delta), radians.  Approx FAO-56 eqn. 24.
     */
    private static double solarDeclination(int dayOfYear) {
        return 0.409 * Math.sin((2.0 * Math.PI * dayOfYear / 365.0) - 1.39);
    }

    /**
     * Inverse relative distance Earth–Sun (dr).  FAO-56 eqn. 23.
     */
    private static double inverseRelativeDistanceEarthSun(int dayOfYear) {
        return 1.0 + 0.033 * Math.cos(2.0 * Math.PI * dayOfYear / 365.0);
    }

    /**
     * Hour angle (omega) for a given hour.
     * 'solar noon' ~ 12.
     * Return value in radians.
     */
    private static double hourAngle(double hour, double solarNoon) {
        // Each hour = 15 degrees = pi/12 radians from solar noon
        // If hour=12 => omega=0
        return (Math.PI / 12.0) * (hour - solarNoon);
    }

    /**
     * Computes the hourly extraterrestrial radiation Ra by integrating
     * from hour angle w1 to w2.  (FAO-56 uses eqn. 28–29 with modifications.)
     */
    private static double calcHourlyRa(double phi, double delta, double dr,
                                       double w1, double w2) {
        // Sunrise hour angle
        double ws = Math.acos(-Math.tan(phi) * Math.tan(delta));

        // If entire hour is dark, Ra=0.
        // If entire hour is light, integrate w1->w2.
        // If partially, clip boundaries to [-ws, +ws].
        double w1c = Math.max(-ws, Math.min(ws, w1));
        double w2c = Math.max(-ws, Math.min(ws, w2));
        if (w2c <= w1c) {
            return 0.0;
        }

        // Now do the integration for w1c..w2c
        // Ra = (12*60)/π * GSC * dr * [ (w2-w1)*sin(phi)sin(delta)
        //       + cos(phi)cos(delta)*(sin(w2)-sin(w1)) ]
        final double GSC = 0.0820;  // MJ m^-2 min^-1
        double term1 = (w2c - w1c) * Math.sin(phi) * Math.sin(delta);
        double term2 = Math.cos(phi) * Math.cos(delta)
                * (Math.sin(w2c) - Math.sin(w1c));
        double Ra = (12.0 * 60.0 / Math.PI) * GSC * dr * (term1 + term2);
        return Math.max(Ra, 0.0);
    }

    public static void main(String[] args) {

        // Test 1: Typical midday conditions
        double temp1         = 25.0;     // °C
        double humidity1     = 60.0;     // %
        double ghi1          = 0;    // W/m²
        double windSpeed1    = 0;      // m/s
        double latitude1     = 40.0;     // degrees
        double elevation1    = 200.0;    // m
        double pressureHpa1  = 1013.0;   // hPa
        int dayOfYear1       = 1;      // ~end of May
        int hour1            = 12;       // 1 PM local
        boolean isDaytime1   = true;     // Sun is up

        double eto1 = calculateEToHourly(
                temp1,
                humidity1,
                ghi1,
                windSpeed1,
                latitude1,
                elevation1,
                pressureHpa1,
                dayOfYear1,
                hour1,
                isDaytime1
        );

        System.out.println("=== Test 1: Typical Midday ===");
        System.out.println("Temperature (°C):          " + temp1);
        System.out.println("Relative Humidity (%):     " + humidity1);
        System.out.println("GHI (W/m²):                " + ghi1);
        System.out.println("Wind Speed (m/s):          " + windSpeed1);
        System.out.println("Latitude (°):              " + latitude1);
        System.out.println("Elevation (m):             " + elevation1);
        System.out.println("Pressure (hPa):            " + pressureHpa1);
        System.out.println("Day of Year:               " + dayOfYear1);
        System.out.println("Hour of Day:               " + hour1);
        System.out.println("Is Daytime:                " + isDaytime1);
        System.out.println("Calculated Hourly ETo:     " + eto1 + " mm/hour");
        System.out.println();

        // Test 2: Evening or nighttime conditions
        double temp2         = 18.0;     // °C
        double humidity2     = 80.0;     // %
        double ghi2          = 0.0;      // W/m² (night)
        double windSpeed2    = 0;      // m/s
        double latitude2     = 40.0;
        double elevation2    = 200.0;
        double pressureHpa2  = 1013.0;
        int dayOfYear2       = 150;
        int hour2            = 23;       // 11 PM
        boolean isDaytime2   = false;

        double eto2 = calculateEToHourly(
                temp2,
                humidity2,
                ghi2,
                windSpeed2,
                latitude2,
                elevation2,
                pressureHpa2,
                dayOfYear2,
                hour2,
                isDaytime2
        );

        System.out.println("=== Test 2: Nighttime ===");
        System.out.println("Temperature (°C):          " + temp2);
        System.out.println("Relative Humidity (%):     " + humidity2);
        System.out.println("GHI (W/m²):                " + ghi2);
        System.out.println("Wind Speed (m/s):          " + windSpeed2);
        System.out.println("Latitude (°):              " + latitude2);
        System.out.println("Elevation (m):             " + elevation2);
        System.out.println("Pressure (hPa):            " + pressureHpa2);
        System.out.println("Day of Year:               " + dayOfYear2);
        System.out.println("Hour of Day:               " + hour2);
        System.out.println("Is Daytime:                " + isDaytime2);
        System.out.println("Calculated Hourly ETo:     " + eto2 + " mm/hour");
        System.out.println();

        // Optional: Demonstrate direct testing of Rso or other helper methods
        double rsoMidday = calculateHourlyRso(latitude1, elevation1, dayOfYear1, hour1);
        System.out.println("Midday clear-sky Rso (MJ/m²/hr): " + rsoMidday);

        double rsoNight  = calculateHourlyRso(latitude2, elevation2, dayOfYear2, hour2);
        System.out.println("Nighttime clear-sky Rso (MJ/m²/hr): " + rsoNight);
    }
}
