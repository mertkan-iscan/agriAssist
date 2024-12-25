package io.mertkaniscan.automation_engine.services;


import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.models.Plant;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.services.python_module_services.PythonTaskService;
import io.mertkaniscan.automation_engine.services.forecast_services.solar_forecast_service.SolarResponse;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherResponse;
import io.mertkaniscan.automation_engine.utils.calculators.Calculators;

import io.mertkaniscan.automation_engine.utils.calculators.DailyEToCalculator;
import io.mertkaniscan.automation_engine.utils.calculators.HourlyEToCalculator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Getter
@Service
public class CalculatorService {

    private final FieldService fieldService;
    private final PythonTaskService pythonTaskService;

    public CalculatorService(FieldService fieldService, PythonTaskService pythonTaskService) {
        this.fieldService = fieldService;
        this.pythonTaskService = pythonTaskService;
    }

    public double calculateRew(int fieldID){
        pythonTaskService.sendSoilWaterVolumeFromCalibratedMoisture();
        return 0.0;
    }

    public double calculateTEW(double fieldCapacity, double fieldWiltingPoint, double plantCurrentRootDepth, double fieldMaxEvoporationdepth){

        double Ze = Math.min(plantCurrentRootDepth, fieldMaxEvoporationdepth);

        return Calculators.calculateTEW(fieldCapacity, fieldWiltingPoint, Ze);
    }

    public double calculateTAW(int fieldID){
        Field field = fieldService.getFieldById(fieldID);
        Plant plant = field.getPlantInField();

        return Calculators.calculateTAW(field.getFieldCapacity(), field.getWiltingPoint(), plant.getCurrentRootZoneDepth());
    }

    public double calculateEToDaily(WeatherResponse weatherResponse, SolarResponse solarResponse, Field field) {

        double Tmax = weatherResponse.getDaily().get(0).getTemp().getMax(); // Maximum temperature
        double Tmin = weatherResponse.getDaily().get(0).getTemp().getMin(); // Minimum temperature
        double windSpeed = weatherResponse.getDaily().get(0).getWindSpeed(); // Daily wind speed
        double humidity = weatherResponse.getDaily().get(0).getHumidity(); // Daily humidity
        double latitude = weatherResponse.getLat(); // Latitude
        double pressureHpa = weatherResponse.getDaily().get(0).getPressure(); // Atmospheric pressure

        double ghi = solarResponse.getIrradiance().getDaily().get(0).getClearSky().getGhi();

        double elevation = field.getElevation();

        int dayOfYear = LocalDateTime.now().getDayOfYear();


        log.info("Input - Tmax: {}", Tmax);
        log.info("Input - Tmin: {}", Tmin);
        log.info("Input - GHI: {}", ghi);
        log.info("Input - Wind Speed: {}", windSpeed);
        log.info("Input - Humidity: {}", humidity);
        log.info("Input - Latitude: {}", latitude);
        log.info("Input - Elevation: {}", elevation);
        log.info("Input - Pressure (hPa): {}", pressureHpa);
        log.info("Internal - Calculated Day of Year: {}", dayOfYear);

        double eto = DailyEToCalculator.calculateEToDaily(
                Tmax, Tmin, ghi, windSpeed, humidity, latitude, elevation, pressureHpa, dayOfYear);

        if (eto < 0) {
            log.warn("Calculated ETo (Daily) is negative! Setting to 0. Value: " + eto);
            eto = 0.0;
        }

        return eto;
    }

    public double calculateEToHourly(WeatherResponse weatherResponse, SolarResponse solarResponse, Field field, int hourIndex) {


        double Tmax = weatherResponse.
        double Tmin = weatherResponse.
        double windSpeed = weatherResponse.
        double humidity = weatherResponse.
        double latitude = weatherResponse.
        double pressureHpa = weatherResponse.

        double ghi = solarResponse.getIrradiance().getHourly().get(hourIndex).getClearSky().getGhi();

        double elevation = field.getElevation();

        int dayOfYear = LocalDateTime.now().getDayOfYear();

        int hour = LocalDateTime.now().getHour();

        boolean isDaytime = radiation > 0;

        double eto = HourlyEToCalculator.calculateEToHourly(
                temp, humidity, windSpeed, latitude, elevation, dayOfYear, hour, radiation, pressureHpa, isDaytime);

        // Ensure value is non-negative
        if (eto < 0) {
            log.warn("Calculated ETo (Hourly) is negative! Setting to 0. Value: {}", eto);
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

    public double calculateKe(int fieldID) {

        Field field = fieldService.getFieldById(fieldID);

        field.getPlantInField().getDays

        double Kcb = field.getPlantInField().getCurrentKcValue();

        double humidity = field.getPlantInField().getCurrentHour().getSensorHumidity();
        double windSpeed = field.getCurrentWindSpeed();
        double De = field.getCurrentDeValue();


        double KcMax = Calculators.calculateKcMax(Kcb, humidity, windSpeed);
        double Kr = Calculators.calculateKr(De, TEW, REW);
        double fw = calculateFw(fieldID);

        return Calculators.calculateKe(Kr, fw, KcMax);
    }

    public double calculateFw(int fieldID) {

        Field field = fieldService.getFieldById(fieldID);

        return 0.5;
    }
}
