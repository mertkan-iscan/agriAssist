package io.mertkaniscan.automation_engine.services.task_services;

import io.mertkaniscan.automation_engine.models.*;
import io.mertkaniscan.automation_engine.models.WeatherResponse.Hourly;

import io.mertkaniscan.automation_engine.repositories.DayRepository;
import io.mertkaniscan.automation_engine.repositories.FieldRepository;
import io.mertkaniscan.automation_engine.repositories.SolarResponseRepository;

import io.mertkaniscan.automation_engine.services.logic.CalculatorService;
import io.mertkaniscan.automation_engine.services.main_services.SensorDataService;
import io.mertkaniscan.automation_engine.services.weather_forecast_services.WeatherForecastService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class HourlyTaskService {

    private static final Logger logger = LogManager.getLogger(HourlyTaskService.class);
    private static final int CLOUD_COVER_THRESHOLD = 50;

    private final FieldRepository fieldRepository;
    private final DayRepository dayRepository;
    private final SensorDataService sensorDataService;
    private final WeatherForecastService weatherForecastService;
    private final CalculatorService calculatorService;
    private final SolarResponseRepository solarResponseRepository;

    public HourlyTaskService(FieldRepository fieldRepository,
                             DayRepository dayRepository,
                             SensorDataService sensorDataService,
                             WeatherForecastService weatherForecastService,
                             CalculatorService calculatorService,
                             SolarResponseRepository solarResponseRepository) {
        this.fieldRepository = fieldRepository;
        this.dayRepository = dayRepository;
        this.sensorDataService = sensorDataService;
        this.weatherForecastService = weatherForecastService;
        this.calculatorService = calculatorService;
        this.solarResponseRepository = solarResponseRepository;
    }

    public void recordHourlyData() {
        logger.info("Starting hourly data recording task.");
        fieldRepository.findAll().parallelStream().forEach(this::processField);
        logger.info("Hourly data recording task completed.");
    }

    private void processField(Field field) {
        logger.debug("Processing field: {}", field.getFieldName());
        try {
            LocalDateTime now = LocalDateTime.now();
            int currentHour = now.getHour();

            Optional<Hour> hourRecord = getHourRecord(field, currentHour);
            if (hourRecord.isEmpty()) {
                logger.info("No hour record available for field '{}', hour={}. Skipping update.",
                        field.getFieldName(), currentHour);
                return;
            }

            WeatherData weatherData = collectWeatherData(field, now);
            SolarData solarData = collectSolarData(field, currentHour, weatherData.getCloudCover());
            updateHourRecord(hourRecord.get(), weatherData, solarData, field);

            logger.info("Hour data updated for field '{}', hour={}.", field.getFieldName(), currentHour);
        } catch (Exception e) {
            logger.error("Error processing field '{}'. Error: {}", field.getFieldName(), e.getMessage(), e);
        }
    }

    private Optional<Hour> getHourRecord(Field field, int currentHour) {
        Day today = getExistingDay(field.getPlantInField().getPlantID());
        if (today == null) {
            logger.info("No existing Day record found for plantID={}", field.getPlantInField().getPlantID());
            return Optional.empty();
        }

        return today.getHours().stream()
                .filter(h -> h.getHour() == currentHour)
                .findFirst();
    }

    private WeatherData collectWeatherData(Field field, LocalDateTime now) {
        Timestamp startOfHour = Timestamp.valueOf(now.withMinute(0).withSecond(0).withNano(0));

        Double temperature = getWeightedMean(field.getFieldID(), "weather_temp", startOfHour);
        Double humidity = getWeightedMean(field.getFieldID(), "weather_hum", startOfHour);

        if(temperature == null && humidity == null) {
            logger.error("Temperature and humidity data is missing in DB for field {}", field.getFieldName());
        }

        Hourly hourlyWeather = weatherForecastService.getAndParseWeatherData(
                field.getLatitude(), field.getLongitude()
        ).getHourly().get(0);

        return new WeatherData(
                temperature != null ? temperature : hourlyWeather.getTemp(),
                humidity != null ? humidity : hourlyWeather.getHumidity(),
                hourlyWeather.getWind_speed(),
                hourlyWeather.getClouds(),
                hourlyWeather.getPressure()
        );
    }

    private SolarData collectSolarData(Field field, int currentHour, int cloudCover) {
        Optional<SolarResponse> solarDataOpt = solarResponseRepository.findByFieldAndDate(
                field, LocalDate.now().toString()
        );

        if (solarDataOpt.isEmpty()) {
            throw new RuntimeException("Solar data is missing in DB for field " + field.getFieldName());
        }

        SolarResponse.Irradiance.HourlyIrradiance irradiance =
                solarDataOpt.get().getIrradiance().getHourly().get(currentHour);

        boolean isSunny = cloudCover < CLOUD_COVER_THRESHOLD;
        double ghi = isSunny ? irradiance.getClearSky().getGhi() : irradiance.getCloudySky().getGhi();

        return new SolarData(ghi, isSunny);
    }

    private void updateHourRecord(Hour hour, WeatherData weatherData, SolarData solarData, Field field) {
        double eto = calculatorService.calculateEToHourly(
                weatherData.getTemperature(),
                weatherData.getHumidity(),
                weatherData.getWindSpeed(),
                field.getLatitude(),
                field.getElevation(),
                solarData.getGhi(),
                weatherData.getPressure()
        );

        double ke = calculatorService.calculateKe(
                field.getPlantInField().getCurrentCropCoefficient().doubleValue(),
                weatherData.getHumidity(),
                weatherData.getWindSpeed(),
                0, 0, 0
        );

        hour.setSensorEToHourly(eto);
        hour.setKe(ke);

        dayRepository.save(hour.getDay());
    }

    private Double getWeightedMean(int fieldID, String dataType, Timestamp startOfHour) {
        logger.debug("Fetching sensor data for field ID={}, dataType='{}', startOfHour={}",
                fieldID, dataType, startOfHour);

        List<SensorData> data = sensorDataService.findByFieldIdAndTypeAndTimestampAfter(
                fieldID, dataType, startOfHour
        );

        if (data.isEmpty()) {
            logger.warn("No sensor data found for field ID={}, dataType='{}'.", fieldID, dataType);
            return null;
        }

        return calculateWeightedMean(data, startOfHour);
    }

    private Double calculateWeightedMean(List<SensorData> data, Timestamp startOfHour) {
        double total = 0.0;
        double weightSum = 0.0;

        for (SensorData sensor : data) {
            double value = sensor.getDataValue();
            double weightFactor = Math.abs(startOfHour.getTime() - sensor.getTimestamp().getTime());

            total += value * weightFactor;
            weightSum += weightFactor;
        }

        return weightSum == 0 ? null : total / weightSum;
    }

    private Day getExistingDay(int plantID) {
        return dayRepository.findByPlant_PlantIDAndDate(
                plantID,
                Timestamp.valueOf(LocalDate.now().atStartOfDay())
        );
    }

    private static class WeatherData {
        private final double temperature;
        private final double humidity;
        private final double windSpeed;
        private final int cloudCover;
        private final double pressure;

        public WeatherData(double temperature, double humidity, double windSpeed, int cloudCover, double pressure) {
            this.temperature = temperature;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.cloudCover = cloudCover;
            this.pressure = pressure;
        }

        public double getTemperature() {
            return temperature;
        }

        public double getHumidity() {
            return humidity;
        }

        public double getWindSpeed() {
            return windSpeed;
        }

        public int getCloudCover() {
            return cloudCover;
        }

        public double getPressure() {
            return pressure;
        }
    }

    private static class SolarData {
        private final double ghi;
        private final boolean isSunny;

        public SolarData(double ghi, boolean isSunny) {
            this.ghi = ghi;
            this.isSunny = isSunny;
        }

        public double getGhi() {
            return ghi;
        }

        public boolean isSunny() {
            return isSunny;
        }
    }
}
