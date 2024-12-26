package io.mertkaniscan.automation_engine;


import static io.mertkaniscan.automation_engine.utils.calculators.DailyEToCalculator.calculateEToDaily;

public class CalcuatorTest {
    public static void main(String[] args) {

        double Tmax = 25; // Maximum temperature (°C)
        double Tmin = 23.2; // Minimum temperature (°C)
        double ghi = 600; // Global horizontal irradiance (Wh/m²/day)
        double windSpeed = 0; // Wind speed (m/s)
        double humidity = 55; // Relative humidity (%)
        double latitude = 40.64; // Latitude (degrees)
        double elevation = 0.0; // Elevation (meters)
        int dayOfYear = 349; // Day of year (Julian day)
        double pressure = 1033;

        //eto variables
        //8.15
        //5.19
        //2229.94
        //6.09
        //61
        //40.6454272
        //0.0
        //1033
        //Day of year: 349

        double eto = calculateEToDaily(Tmax, Tmin, ghi, windSpeed, humidity, latitude, elevation, pressure, dayOfYear);

        // Print the result
        System.out.println("Calculated ETo (mm/day): " + eto);
    }
}