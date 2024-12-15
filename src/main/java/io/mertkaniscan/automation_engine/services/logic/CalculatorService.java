package io.mertkaniscan.automation_engine.services.logic;

import io.mertkaniscan.automation_engine.models.Day;
import io.mertkaniscan.automation_engine.models.DepletionData;
import io.mertkaniscan.automation_engine.utils.calculators.Calculators;
import io.mertkaniscan.automation_engine.models.Plant;
import io.mertkaniscan.automation_engine.utils.calculators.DailyEToCalculator;
import io.mertkaniscan.automation_engine.utils.calculators.HourlyEToCalculator;
import org.springframework.stereotype.Service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;

@Service
public class CalculatorService {

    private static final Logger logger = LogManager.getLogger(CalculatorService.class);

    private DepletionData depletionData;

    public CalculatorService() {
        this.depletionData = new DepletionData(0.0, LocalDateTime.now());
    }

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
     * @return            Reference evapotranspiration ETo (mm/day)
     */
    public double calculateEToDaily(double Tmax, double Tmin, double ghi, double windSpeed,
                                    double humidity, double latitude, double elevation, double pressureHpa) {

        int dayOfYear = LocalDateTime.now().getDayOfYear();

        // Log all input parameters
        logger.info("Input - Tmax: " + Tmax);
        logger.info("Input - Tmin: " + Tmin);
        logger.info("Input - GHI: " + ghi);
        logger.info("Input - Wind Speed: " + windSpeed);
        logger.info("Input - Humidity: " + humidity);
        logger.info("Input - Latitude: " + latitude);
        logger.info("Input - Elevation: " + elevation);
        logger.info("Input - Pressure (hPa): " + pressureHpa);
        logger.info("Internal - Calculated Day of Year: " + dayOfYear);

        return DailyEToCalculator.calculateEToDaily(Tmax, Tmin, ghi, windSpeed,
                humidity, latitude, elevation, pressureHpa, dayOfYear);
    }

    /**
     * Calculates hourly reference evapotranspiration (ETo) using the FAO-56 Penman-Monteith equation.
     *
     * @param temp         Current air temperature (°C)
     * @param humidity     Current relative humidity (%)
     * @param windSpeed    Current wind speed at 2m height (m/s)
     * @param latitude     Geographical latitude (decimal degrees)
     * @param elevation     Elevation above sea level (meters)
     * @param radiation    Solar radiation (W/m²)
     * @param pressureHpa  Atmospheric pressure (hPa or mbar)
     * @return            Reference evapotranspiration ETo (mm/hour)
     */
    public double calculateEToHourly(double temp, double humidity, double windSpeed,
                                     double latitude, double elevation, double radiation,
                                     double pressureHpa) {

        int dayOfYear = LocalDateTime.now().get(ChronoField.DAY_OF_YEAR);
        int hour = LocalDateTime.now().getHour();

        boolean isDaytime = radiation > 0;

        return HourlyEToCalculator.calculateEToHourly(temp, humidity, windSpeed,
                latitude, elevation, dayOfYear, hour, radiation, pressureHpa, isDaytime);
    }

    public synchronized void calculateDepletion(double evapotranspiration, double rainfall) {

        double previousD = depletionData.getDepletion();

        // FAO Depletion Formula: D = D_prev - Rainfall + ET
        double newDepletion = Math.max(0, previousD - rainfall + evapotranspiration);

        depletionData.setDepletion(newDepletion);
        depletionData.setLastUpdated(LocalDateTime.now());

        System.out.println("Depletion updated: D = " + newDepletion);
    }

    public Double calculateVPD(double temperature, double humidity) {
        double vpd = Calculators.calculateVPD(temperature, humidity);
        return vpd;
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
