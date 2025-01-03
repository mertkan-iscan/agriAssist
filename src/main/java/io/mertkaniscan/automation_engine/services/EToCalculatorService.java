package io.mertkaniscan.automation_engine.services;

import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.models.Hour;
import io.mertkaniscan.automation_engine.services.forecast_services.solar_forecast_service.SolarResponse;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherResponse;
import io.mertkaniscan.automation_engine.utils.calculators.Calculators;
import io.mertkaniscan.automation_engine.utils.calculators.DailyEToCalculator;
import io.mertkaniscan.automation_engine.utils.calculators.HourlyEToCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class EToCalculatorService {

    public double calculateEToDaily(WeatherResponse weatherResponse, SolarResponse solarResponse, Field field) {

        double Tmax = weatherResponse.getDaily().get(0).getTemp().getMax();
        double Tmin = weatherResponse.getDaily().get(0).getTemp().getMin();

        double humidity = weatherResponse.getDaily().get(0).getHumidity();
        double latitude = weatherResponse.getLat();
        double pressureHpa = weatherResponse.getDaily().get(0).getPressure();

        int cloudCoverage = weatherResponse.getDaily().get(0).getClouds();
        double clearSkyGHI = solarResponse.getIrradiance().getDaily().get(0).getClearSky().getGhi();
        double cloudySkyGHI = solarResponse.getIrradiance().getDaily().get(0).getCloudySky().getGhi();
        double ghi = calculateGHI(clearSkyGHI, cloudySkyGHI, cloudCoverage);

        double windSpeed = getDailyWindSpeed(field, weatherResponse);

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
            log.warn("Calculated ETo (Daily) is negative! Setting to 0. Value: {}", eto);
            eto = 0.0;
        }

        return eto;
    }

    public double calculateForecastEToHourly(WeatherResponse weatherResponse, SolarResponse solarResponse, Field field, int hourIndex) {


        double temp = weatherResponse.getHourly().get(hourIndex).getTemp();
        double humidity = weatherResponse.getHourly().get(hourIndex).getHumidity();
        double latitude = field.getLatitude();
        double pressureHpa = weatherResponse.getHourly().get(hourIndex).getPressure();

        int cloudCoverage = weatherResponse.getHourly().get(hourIndex).getClouds();
        double clearSkyGHI = solarResponse.getIrradiance().getHourly().get(hourIndex).getClearSky().getGhi();
        double cloudySkyGHI = solarResponse.getIrradiance().getHourly().get(hourIndex).getCloudySky().getGhi();
        double ghi = calculateGHI(clearSkyGHI, cloudySkyGHI, cloudCoverage);

        double windSpeed = getHourlyWindSpeed(field, weatherResponse, hourIndex);

        double elevation = field.getElevation();

        // Calculate day of the year and the current hour
        int dayOfYear = LocalDateTime.now().getDayOfYear();
        int hour = LocalDateTime.now().getHour();

        // Determine if it's daytime based on solar radiation
        boolean isDaytime = ghi > 0;

        // Use the HourlyEToCalculator to calculate ETo
        double eto = HourlyEToCalculator.calculateEToHourly(
                temp, humidity, ghi, windSpeed, latitude, elevation, pressureHpa, dayOfYear, hour, isDaytime);

        // Ensure value is non-negative
        if (eto < 0) {
            log.warn("Calculated ETo (Hourly) is negative! Setting to 0. Value: {}", eto);
            eto = 0.0;
        }

        return eto;
    }

    public double calculateCurrentEToHourly(WeatherResponse weatherResponse, SolarResponse solarResponse, Field field, int hourIndex) {


        double temp = weatherResponse.getCurrent().getTemp();
        double humidity = weatherResponse.getCurrent().getHumidity();

        double latitude = field.getLatitude();
        double pressureHpa = weatherResponse.getCurrent().getPressure();

        int cloudCoverage = weatherResponse.getCurrent().getClouds();

        double clearSkyGHI = solarResponse.getIrradiance().getHourly().get(hourIndex).getClearSky().getGhi();
        double cloudySkyGHI = solarResponse.getIrradiance().getHourly().get(hourIndex).getCloudySky().getGhi();

        double ghi = calculateGHI(clearSkyGHI, cloudySkyGHI, cloudCoverage);

        double windSpeed = getHourlyWindSpeed(field, weatherResponse, hourIndex);

        double elevation = field.getElevation();

        // Calculate day of the year and the current hour
        int dayOfYear = LocalDateTime.now().getDayOfYear();
        int hour = LocalDateTime.now().getHour();

        // Determine if it's daytime based on solar radiation
        boolean isDaytime = ghi > 0;

        // Use the HourlyEToCalculator to calculate ETo
        double eto = HourlyEToCalculator.calculateEToHourly(
                temp, humidity, ghi, windSpeed, latitude, elevation, pressureHpa, dayOfYear, hour, isDaytime);

        // Ensure value is non-negative
        if (eto < 0) {
            log.warn("Calculated ETo (Hourly) is negative! Setting to 0. Value: {}", eto);
            eto = 0.0;
        }

        return eto;
    }

    public double calculateEtcDualhourly(Field field, Hour hour){

        double currentKc = field.getPlantInField().getCurrentKcValue();
        double currentKe = field.getCurrentValues().getTewValue();

        double ETo;
        if(field.getFieldType() == Field.FieldType.GREENHOUSE){

            ETo = hour.getSensorEToHourly();
        }else{

            ETo = hour.getForecastEToHourly();
        }

        return Calculators.calculateETcDual(currentKc, currentKe, ETo);
    }

    public double calculateSensorEToHourly(Double sensorTemp, Double sensorHumidity, WeatherResponse weatherResponse, SolarResponse solarResponse, Field field, int hourIndex) {


        double temp = sensorTemp;
        double humidity = sensorHumidity;

        double latitude = field.getLatitude();
        double pressureHpa = weatherResponse.getCurrent().getPressure();

        double ghi = calculateSolarRadiationHourly(weatherResponse, solarResponse, hourIndex);

        double windSpeed = getHourlyWindSpeed(field, weatherResponse, hourIndex);

        double elevation = field.getElevation();

        // Calculate day of the year and the current hour
        int dayOfYear = LocalDateTime.now().getDayOfYear();
        int hour = LocalDateTime.now().getHour();

        // Determine if it's daytime based on solar radiation
        boolean isDaytime = ghi > 0;

        // Use the HourlyEToCalculator to calculate ETo
        double eto = HourlyEToCalculator.calculateEToHourly(
                temp, humidity, ghi, windSpeed, latitude, elevation, pressureHpa, dayOfYear, hour, isDaytime);

        // Ensure value is non-negative
        if (eto < 0) {
            log.warn("Calculated ETo (Hourly) is negative! Setting to 0. Value: {}", eto);
            eto = 0.0;
        }

        return eto;
    }

    public double calculateSolarRadiationHourly(WeatherResponse weatherResponse, SolarResponse solarResponse, int hourIndex){

        int cloudCoverage = weatherResponse.getCurrent().getClouds();

        double clearSkyGHI = solarResponse.getIrradiance().getHourly().get(hourIndex).getClearSky().getGhi();
        double cloudySkyGHI = solarResponse.getIrradiance().getHourly().get(hourIndex).getCloudySky().getGhi();

        return calculateGHI(clearSkyGHI, cloudySkyGHI, cloudCoverage);
    }

    public double getHourlyWindSpeed(Field field, WeatherResponse weatherResponse, int hourIndex) {
        if (field.getFieldType() == Field.FieldType.GREENHOUSE) {
            return 0.0;
        }
        return weatherResponse.getHourly().get(hourIndex).getWindSpeed();
    }

    public double getDailyWindSpeed(Field field, WeatherResponse weatherResponse) {
        if (field.getFieldType() == Field.FieldType.GREENHOUSE) {
            return 0.0;
        }
        return weatherResponse.getDaily().get(0).getWindSpeed();
    }

    public double calculateGHI(double clearSkyGHI, double cloudySkyGHI, int cloudCoverage) {
        if (cloudCoverage < 0 || cloudCoverage > 100) {
            throw new IllegalArgumentException("Cloud coverage must be between 0 and 100.");
        }

        double ghi = (1 - (cloudCoverage / 100.0)) * clearSkyGHI + (cloudCoverage / 100.0) * cloudySkyGHI;

        log.info("Calculated GHI: {} (Clear Sky GHI: {}, Cloudy Sky GHI: {}, Cloud Coverage: {}%)",
                ghi, clearSkyGHI, cloudySkyGHI, cloudCoverage);

        return ghi;
    }
}
