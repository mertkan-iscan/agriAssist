package io.mertkaniscan.automation_engine;

import static io.mertkaniscan.automation_engine.utils.Calculators.calculateFullETcDual;

public class CalcuatorTest {
    public static void main(String[] args) {
        double Kcb = 0.5;
        double T = 25.0;
        double humidity = 60.0;
        double pressure = 101.3;
        double ghi = 20.0;
        double dni = 15.0;
        double dhi = 5.0;
        double windSpeed = 2.0;
        double wettedArea = 50.0;
        double totalArea = 100.0;
        double FieldCapacity = 0.30;
        double WiltingPoint = 0.15;
        double Ze = 0.10;
        double DePrev = 5.0;
        double dailyRainfall = 2.0;
        double irrigation = 3.0;
        double albedo = 0.23;

        double ETcDual = calculateFullETcDual(Kcb, T, humidity, pressure, ghi, dni, dhi, windSpeed, wettedArea, totalArea,
                FieldCapacity, WiltingPoint, Ze, DePrev, dailyRainfall, irrigation, albedo);

        System.out.println("ETc Dual: " + ETcDual + " mm/day");
    }

}
