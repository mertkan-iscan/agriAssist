package io.mertkaniscan.automation_engine.services.logic;

import io.mertkaniscan.automation_engine.models.Day;
import io.mertkaniscan.automation_engine.models.DepletionData;
import io.mertkaniscan.automation_engine.components.Calculators;
import io.mertkaniscan.automation_engine.models.Plant;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CalculatorService {

    private DepletionData depletionData;

    // Constructor initializes D to a default value
    public CalculatorService() {
        this.depletionData = new DepletionData(0, LocalDateTime.now());
    }

    // Method to calculate depletion based on FAO formula
    public synchronized void calculateDepletion(double evapotranspiration, double rainfall) {

        double previousD = depletionData.getDepletion();

        // FAO Depletion Formula: D = D_prev - Rainfall + ET
        double newDepletion = Math.max(0, previousD - rainfall + evapotranspiration);

        depletionData.setDepletion(newDepletion);
        depletionData.setLastUpdated(LocalDateTime.now());

        System.out.println("Depletion updated: D = " + newDepletion);
    }

    // Method to calculate Reference ETo
    public double calculateETo(double temperature, double humidity, double pressure, double ghi, double dni, double dhi, double windSpeed) {

        double Rn = Calculators.calculateRn(ghi, dni, dhi, 0.23); // Example albedo = 0.23
        double es = Calculators.calculateSaturationVaporPressure(temperature);
        double delta = Calculators.calculateDelta(es, temperature);
        double gamma = Calculators.calculateGamma(pressure);
        double ea = es * (humidity / 100.0); // Actual vapor pressure

        return Calculators.calculateETo(temperature, humidity, pressure, ghi, dni, dhi, windSpeed, Rn, 0, es, ea, delta, gamma);
    }

    // Method to retrieve the latest depletion value
    public DepletionData getDepletionData() {
        return this.depletionData;
    }

    // Method to calculate Ke (Evaporation Coefficient)
    public double calculateKe(double Kcb, double humidity, double windSpeed, double De, double TEW, double REW) {
        double KcMax = Calculators.calculateKcMax(Kcb, humidity, windSpeed);
        double Kr = Calculators.calculateKr(De, TEW, REW);
        double fw = 0.5; // Example wetted fraction, adjust as needed

        return Calculators.calculateKe(Kcb, Kr, fw, KcMax, De, TEW, REW);
    }


    public double updateDailyKc(Plant.PlantStage plantStage, Day today) {
        return 0;
    }

    public double updateDailyKc(String string, double v) {
        return 0;
    }
}
