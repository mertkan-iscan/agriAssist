package io.mertkaniscan.automation_engine.services.logic;


import io.mertkaniscan.automation_engine.models.DepletionData;
import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.models.Plant;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.services.python_module_services.PythonTaskService;
import io.mertkaniscan.automation_engine.utils.calculators.Calculators;

import io.mertkaniscan.automation_engine.utils.calculators.DailyEToCalculator;
import io.mertkaniscan.automation_engine.utils.calculators.HourlyEToCalculator;
import lombok.Getter;
import org.springframework.stereotype.Service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;

@Getter
@Service
public class CalculatorService {

    private static final Logger logger = LogManager.getLogger(CalculatorService.class);

    private final FieldService fieldService;
    private final PythonTaskService pythonTaskService;

    public CalculatorService(FieldService fieldService, PythonTaskService pythonTaskService) {
        this.fieldService = fieldService;
        this.pythonTaskService = pythonTaskService;
    }

    //public double calculateRew(int fieldID){
//
    //    pythonTaskService.sendInterpolationSoilWaterPercentage();
    //}

    public double calculateTEW(double fieldCapacity, double fieldWiltingPoint, double plantCurrentRootDepth, double fieldMaxEvoporationdepth){

        double Ze = Math.min(plantCurrentRootDepth, fieldMaxEvoporationdepth);

        return Calculators.calculateTEW(fieldCapacity, fieldWiltingPoint, Ze);
    }

    public double calculateTAW(int fieldID){
        Field field = fieldService.getFieldById(fieldID);
        Plant plant = field.getPlantInField();

        return Calculators.calculateTAW(field.getFieldCapacity(), field.getWiltingPoint(), plant.getCurrentRootZoneDepth());
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
        logger.info("Input - Tmax: {}", Tmax);
        logger.info("Input - Tmin: {}", Tmin);
        logger.info("Input - GHI: {}", ghi);
        logger.info("Input - Wind Speed: {}", windSpeed);
        logger.info("Input - Humidity: {}", humidity);
        logger.info("Input - Latitude: {}", latitude);
        logger.info("Input - Elevation: {}", elevation);
        logger.info("Input - Pressure (hPa): {}", pressureHpa);
        logger.info("Internal - Calculated Day of Year: {}", dayOfYear);

        double eto = DailyEToCalculator.calculateEToDaily(
                Tmax, Tmin, ghi, windSpeed, humidity, latitude, elevation, pressureHpa, dayOfYear);

        if (eto < 0) {
            logger.warn("Calculated ETo (Daily) is negative! Setting to 0. Value: " + eto);
            eto = 0.0;
        }

        return eto;
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

        int dayOfYear = LocalDateTime.now().getDayOfYear();
        int hour = LocalDateTime.now().getHour();

        boolean isDaytime = radiation > 0;

        double eto = HourlyEToCalculator.calculateEToHourly(
                temp, humidity, windSpeed, latitude, elevation, dayOfYear, hour, radiation, pressureHpa, isDaytime);

        // Ensure value is non-negative
        if (eto < 0) {
            logger.warn("Calculated ETo (Hourly) is negative! Setting to 0. Value: {}", eto);
            eto = 0.0;
        }

        return eto;
    }

    /**
     * Calculates the wetted area fraction (fw) for drip irrigation.
     *
     * @param irrigationDuration The duration of irrigation in hours
     * @param emitterRate        The emitter rate of drip irrigation in L/hour
     * @param totalArea          The total area of the field in square meters
     * @return The calculated wetted area fraction (fw)
     */
    private double calculateFwForDripIrrigation(double irrigationDuration, double emitterRate, double totalArea) {
        // Estimate wetted area using emitter rate and irrigation time
        // Example calculation: Assume a standard radius of wetting per emitter
        double waterVolume = irrigationDuration * emitterRate; // Total water volume in liters
        double wettedArea = waterVolume / 10.0; // Convert to approximate m² (assumption: 1 liter covers ~10m²).

        // Return fraction of wetted area
        return Calculators.calculateFw(wettedArea, totalArea); // Use provided method to ensure fw is capped between 0 and 1
    }

    //public double calculateKe(int fieldID) {
//
    //    //double Kcb, double humidity, double windSpeed, double De, double TEW, double REW
//
    //    Field field = fieldService.getFieldById(fieldID);
//
    //    double Kcb = field.getPlantInField().getCurrentKcValue();
//
    //    double humidity = field.getPlantInField().getCurrentHour().getSensorHumidity();
    //    double windSpeed = field.getCurrentWindSpeed();
    //    double De = field.getCurrentDeValue();
//
//
    //    double KcMax = Calculators.calculateKcMax(Kcb, humidity, windSpeed);
    //    double Kr = Calculators.calculateKr(De, TEW, REW);
    //    double fw = calculateFw(fieldID);
//
    //    return Calculators.calculateKe(Kr, fw, KcMax);
    //}

    //public double calculateFw(int fieldID) {
//
    //    Field field = fieldService.getFieldById(fieldID);
//
    //    //boolean isRained, double rainAmount, double timeSinceRain,
    //    //boolean isIrrigated, double irrigationAmount, double timeSinceIrrigation,
    //    //double totalFieldArea
//
    //    double wettedFieldArea = field.getCurrentWetArea();
    //    boolean isRaining = field.getIsRaining();
//
//
    //    if (isRained && rainAmount > 0) {
    //        wettedFieldArea += (rainAmount * 0.5) / Math.max(1, timeSinceRain);
    //    }
//
    //    if (isIrrigated && irrigationAmount > 0) {
    //        wettedFieldArea += (irrigationAmount * 0.8) / Math.max(1, timeSinceIrrigation);
    //    }
//
    //    wettedFieldArea = Math.min(wettedFieldArea, totalFieldArea);
//
//
    //    return Calculators.calculateFw(wettedFieldArea, totalFieldArea);
    //}
}
