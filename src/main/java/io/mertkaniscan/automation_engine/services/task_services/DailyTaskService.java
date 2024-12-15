package io.mertkaniscan.automation_engine.services.task_services;

import io.mertkaniscan.automation_engine.models.*;

import io.mertkaniscan.automation_engine.repositories.DayRepository;
import io.mertkaniscan.automation_engine.repositories.FieldRepository;
import io.mertkaniscan.automation_engine.repositories.SolarResponseRepository;

import io.mertkaniscan.automation_engine.repositories.WeatherResponseRepository;
import io.mertkaniscan.automation_engine.services.logic.CalculatorService;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.services.weather_forecast_services.WeatherForecastService;

import io.mertkaniscan.automation_engine.models.WeatherResponse;
import io.mertkaniscan.automation_engine.models.WeatherResponse.Daily;
import io.mertkaniscan.automation_engine.models.WeatherResponse.Hourly;

import io.mertkaniscan.automation_engine.utils.calculators.Calculators;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DailyTaskService {

    private static final Logger logger = LogManager.getLogger(DailyTaskService.class);

    private final FieldRepository fieldRepository;
    private final DayRepository dayRepository;
    private final WeatherForecastService weatherForecastService;
    private final Calculators calculator;
    private final CalculatorService calculatorService;
    private final SolarResponseRepository solarResponseRepository;
    private final WeatherResponseRepository weatherResponseRepository;
    private final FieldService fieldService;

    // Helper record classes for data transfer
    private record SolarValues(double ghi, double dhi) {}

    private record WeatherValues(double temp, double humidity, double windSpeed, double pressure) {}

    public DailyTaskService(FieldRepository fieldRepository,
                            DayRepository dayRepository,
                            WeatherForecastService weatherForecastService,
                            Calculators calculator,
                            CalculatorService calculatorService,
                            SolarResponseRepository solarResponseRepository, WeatherResponseRepository weatherResponseRepository,
                            FieldService fieldService) {

        this.fieldRepository = fieldRepository;
        this.dayRepository = dayRepository;
        this.weatherForecastService = weatherForecastService;
        this.calculator = calculator;
        this.calculatorService = calculatorService;
        this.solarResponseRepository = solarResponseRepository;
        this.weatherResponseRepository = weatherResponseRepository;
        this.fieldService = fieldService;
    }

    @Transactional
    public void createDailyRecords() {
        logger.info("Starting daily task to create or reuse Day records and pre-calculate hourly guessed ETo.");

        List<Field> fields = fieldRepository.findAll();
        logger.debug("Fetched {} fields from the database.", fields.size());

        for (Field field : fields) {
            processField(field);
        }

        logger.info("Daily task completed.");
    }

    private void processField(Field field) {
        logger.debug("Processing field: {}", field.getFieldName());

        Plant plant = field.getPlantInField();

        if (plant == null) {
            logger.warn("Field '{}' does not have an associated plant. Skipping.", field.getFieldName());
            return;
        }

        try {
            processDailyRecord(field, plant);


        } catch (Exception e) {
            logger.error("Error creating daily record for field '{}'. Error: {}", field.getFieldName(), e.getMessage(), e);
        }
    }

    private void processDailyRecord(Field field, Plant plant) {

        Timestamp startOfDay = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        logger.debug("Checking for existing Day record: plantID={}, date={}", plant.getPlantID(), startOfDay);

        Day existingDay = dayRepository.findByPlant_PlantIDAndDate(plant.getPlantID(), startOfDay);

        // Fetch weather and solar data
        WeatherData weatherData = fetchWeatherData(field);

        // Process day record
        if (existingDay == null) {
            logger.info("No Day record found for plantID={} and date={}. Creating new Day and hours.",
                    plant.getPlantID(), startOfDay);
            createNewDayRecord(field, plant, startOfDay, weatherData);
        } else {
            logger.info("Day record already exists for plantID={} and date={}. Checking hour records...",
                    plant.getPlantID(), startOfDay);
            updateExistingDayRecord(field, existingDay, plant, weatherData);
        }
    }

    private WeatherData fetchWeatherData(Field field) {
        Double latitude = field.getLatitude();
        Double longitude = field.getLongitude();

        WeatherResponse weatherResponse = weatherForecastService.getAndParseWeatherData(
                latitude, longitude
        );
        SolarResponse solarResponse = weatherForecastService.getAndParseSolarData(
                latitude, longitude, LocalDate.now()
        );

        return new WeatherData(weatherResponse, solarResponse);
    }

    private void createNewDayRecord(Field field, Plant plant, Timestamp startOfDay, WeatherData weatherData) {

        Daily dailyWeather = weatherData.weatherResponse.getDaily().get(0);

        Day day = createDay(plant, startOfDay, dailyWeather);


        // Calculate and set daily ETo
        double dailyETo = calculatorService.calculateEToDaily(
                dailyWeather.getTemp().getMax(),      // Tmax
                dailyWeather.getTemp().getMin(),      // Tmin
                weatherData.solarResponse.getIrradiance().getDaily().get(0).getCloudySky().getGhi(), // ghi
                dailyWeather.getWind_speed(),         // windSpeed
                dailyWeather.getHumidity(),           // humidity
                field.getLatitude(),                  // latitude
                field.getElevation(),                  // elevation
                dailyWeather.getPressure()            // pressureHpa
        );

        if (Double.isNaN(dailyETo) || dailyETo <= 0) {
            logger.error("ETo calculation failed for plantID={} and date={}", plant.getPlantID(), startOfDay);
            throw new IllegalArgumentException("Calculated ETo is invalid or null");
        }

        day.setGuessedEtoDaily(dailyETo);

        // Link solar response to field and day
        weatherData.solarResponse.setField(field);
        weatherData.solarResponse.setDay(day);

        solarResponseRepository.save(weatherData.solarResponse);
        //weatherResponseRepository.save(weatherData.weatherResponse);

        // Create hours
        createHoursForDay(field, day, plant, weatherData);
        dayRepository.save(day);
    }

    private Day createDay(Plant plant, Timestamp startOfDay, Daily dailyWeather) {

        Double meanTemperature = calculateMeanTemperature(dailyWeather);
        Double meanHumidity = (double) dailyWeather.getHumidity();

        Timestamp sunrise = Timestamp.from(Instant.ofEpochSecond(dailyWeather.getSunrise()));
        Timestamp sunset = Timestamp.from(Instant.ofEpochSecond(dailyWeather.getSunset()));

        Double vpd = calculator.calculateVPD(meanTemperature, meanHumidity);

        Day day = new Day(startOfDay, sunrise, sunset, vpd, plant);
        day.setHours(new ArrayList<>());
        return dayRepository.save(day);
    }

    private double calculateMeanTemperature(Daily dailyWeather) {
        double dayTemp = dailyWeather.getTemp().getDay();
        double nightTemp = dailyWeather.getTemp().getNight();
        return (dayTemp + nightTemp) / 2.0;
    }

    private void updateExistingDayRecord(Field field, Day day, Plant plant, WeatherData weatherData) {
        if (day.getHours() == null || day.getHours().size() < 24) {
            logger.info("Hours missing or deleted for dayID={}. Recreating hours...", day.getDayID());

            if (day.getHours() == null) {
                day.setHours(new ArrayList<>());
            } else {
                day.getHours().clear();
            }

            createHoursForDay(field, day, plant, weatherData);
            dayRepository.save(day);
            logger.info("Hours recreated for dayID={}.", day.getDayID());
        } else {
            logger.info("All 24 hours already exist for dayID={}. No action needed.", day.getDayID());
        }
    }

    private void createHoursForDay(Field field, Day day, Plant plant, WeatherData weatherData) {
        List<Hourly> hourlyForecasts = weatherData.weatherResponse.getHourly();
        List<SolarResponse.Irradiance.HourlyIrradiance> hourlyIrradiances =
                weatherData.solarResponse.getIrradiance().getHourly();

        for (int i = 0; i < 24; i++) {
            Hourly hourlyWeather = hourlyForecasts.get(i);
            boolean isSunny = hourlyWeather.getClouds() < 50;

            SolarValues solarValues = getSolarValues(hourlyIrradiances.get(i), isSunny);
            WeatherValues weatherValues = getWeatherValues(hourlyWeather);
            Hour hourRecord = createHourRecord(field, i, weatherValues, solarValues, plant, day);

            day.getHours().add(hourRecord);
        }
    }

    private SolarValues getSolarValues(SolarResponse.Irradiance.HourlyIrradiance hourIrradiance, boolean isSunny) {
        double ghi = isSunny ? hourIrradiance.getClearSky().getGhi() : hourIrradiance.getCloudySky().getGhi();
        double dhi = isSunny ? hourIrradiance.getClearSky().getDhi() : hourIrradiance.getCloudySky().getDhi();
        return new SolarValues(ghi, dhi);
    }

    private WeatherValues getWeatherValues(Hourly hourlyWeather) {
        return new WeatherValues(
                hourlyWeather.getTemp(),
                hourlyWeather.getHumidity(),
                hourlyWeather.getWind_speed(),
                hourlyWeather.getPressure() / 10.0
        );
    }

    private Hour createHourRecord(Field field, int hourIndex, WeatherValues weather, SolarValues solar, Plant plant, Day day) {

        double guessedEto = calculatorService.calculateEToHourly(
                weather.temp,           // Current temperature
                weather.humidity,       // Current humidity
                weather.windSpeed,      // Current wind speed
                field.getLatitude(),    // Field latitude
                field.getElevation(),    // Field elevation
                solar.ghi,             // Current hour's solar radiation
                weather.pressure       // Current pressure in hPa
        );

        double ke = calculatorService.calculateKe(
                plant.getCurrentCropCoefficient(),
                weather.humidity,
                weather.windSpeed,
                0, 0, 0
        );

        return new Hour(hourIndex, ke, null, guessedEto, day);
    }



    private static class WeatherData {
        final WeatherResponse weatherResponse;
        final SolarResponse solarResponse;

        WeatherData(WeatherResponse weatherResponse, SolarResponse solarResponse) {
            this.weatherResponse = weatherResponse;
            this.solarResponse = solarResponse;
        }
    }
}