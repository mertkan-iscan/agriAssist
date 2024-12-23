package io.mertkaniscan.automation_engine.services.task_services;

import io.mertkaniscan.automation_engine.models.*;
import io.mertkaniscan.automation_engine.repositories.DayRepository;
import io.mertkaniscan.automation_engine.repositories.FieldRepository;
import io.mertkaniscan.automation_engine.services.logic.CalculatorService;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.services.weather_forecast_services.WeatherForecastService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DailyTaskServiceNew {

    private static final Logger logger = LogManager.getLogger(DailyTaskServiceNew.class);

    private final FieldRepository fieldRepository;
    private final DayRepository dayRepository;
    private final WeatherForecastService weatherForecastService;
    private final CalculatorService calculatorService;
    private final FieldService fieldService;

    public DailyTaskServiceNew(FieldRepository fieldRepository,
                               DayRepository dayRepository,
                               WeatherForecastService weatherForecastService,
                               CalculatorService calculatorService, FieldService fieldService) {
        this.fieldRepository = fieldRepository;
        this.dayRepository = dayRepository;
        this.weatherForecastService = weatherForecastService;
        this.calculatorService = calculatorService;
        this.fieldService = fieldService;
    }

    @Transactional
    public void createDailyRecords() {
        logger.info("Starting daily record creation task.");
        List<Field> fields = fieldRepository.findAll();
        logger.debug("Fetched {} fields from database.", fields.size());

        for (Field field : fields) {
            logger.debug("Processing field: {}", field.getFieldName());
            try {
                processField(field);
            } catch (Exception e) {
                logger.error("Failed to process field '{}': {}", field.getFieldName(), e.getMessage(), e);
            }
        }
        logger.info("Daily record creation task completed.");
    }

    private void processField(Field field) {
        Plant plant = field.getPlantInField();
        if (plant == null) {
            logger.warn("Field '{}' has no associated plant. Skipping.", field.getFieldName());
            return;
        }

        try {
            processDailyRecord(field, plant);
        } catch (Exception e) {
            logger.error("Error creating daily record for field '{}': {}", field.getFieldName(), e.getMessage(), e);
        }
    }

    private void processDailyRecord(Field field, Plant plant) {
        Timestamp currentDate = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        logger.debug("Checking existing Day record for plantID={} and date={}.", plant.getPlantID(), currentDate);

        Day existingDay = dayRepository.findByPlant_PlantIDAndDate(plant.getPlantID(), currentDate);

        if (existingDay == null) {
            logger.info("No Day record found. Creating new record for plantID={} and date={}.", plant.getPlantID(), currentDate);
            createNewDayRecord(field, plant, currentDate);
        } else {
            logger.info("Day record found for plantID={} and date={}. Updating record.", plant.getPlantID(), currentDate);
            updateExistingDayRecord(field, existingDay, plant);
        }
    }

    private void createNewDayRecord(Field field, Plant plant, Timestamp currentDate) {
        try {
            Day day = setStaticDayVariables(field, plant, currentDate);
            createHoursForDay(day);
            dayRepository.save(day);
            logger.info("Successfully created new Day record for plantID={} on date={}.", plant.getPlantID(), currentDate);
        } catch (Exception e) {
            logger.error("Error creating daily record for field '{}': {}", field.getFieldName(), e.getMessage(), e);
            throw e;
        }
    }

    private Day setStaticDayVariables(Field field, Plant plant, Timestamp currentDate) {
        try {
            // Create new Day instance first
            Day day = new Day();
            day.setDate(currentDate);
            day.setPlant(plant);

            // Save the Day entity first to get its ID
            day = dayRepository.save(day);

            // Fetch and prepare weather data
            WeatherResponse weatherResponse = fieldService.getWeatherDataByFieldId(field.getFieldID());
            // Create deep copy of Weather entities to avoid detached entity issues
            deepCopyWeatherResponse(weatherResponse);

            // Set relationships for WeatherResponse
            weatherResponse.setDay(day);
            weatherResponse.setField(field);
            day.setWeatherResponse(weatherResponse);

            // Get sunrise/sunset from weather data and set on day
            if (weatherResponse.getDaily() != null && !weatherResponse.getDaily().isEmpty()) {
                long sunrise = weatherResponse.getDaily().get(0).getSunrise();
                long sunset = weatherResponse.getDaily().get(0).getSunset();
                day.setSunrise(new Timestamp(sunrise * 1000));
                day.setSunset(new Timestamp(sunset * 1000));
            }

            // Fetch and prepare solar data
            SolarResponse solarResponse = fieldService.getSolarDataByFieldId(field.getFieldID(), LocalDate.now());
            solarResponse.setDay(day);
            solarResponse.setField(field);
            day.setSolarResponse(solarResponse);

            // Save everything
            return dayRepository.save(day);

        } catch (Exception e) {
            logger.error("Error in setStaticDayVariables: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void deepCopyWeatherResponse(WeatherResponse weatherResponse) {
        if (weatherResponse.getCurrent() != null && weatherResponse.getCurrent().getWeather() != null) {
            List<WeatherResponse.Weather> newWeatherList = new ArrayList<>();
            for (WeatherResponse.Weather weather : weatherResponse.getCurrent().getWeather()) {
                WeatherResponse.Weather newWeather = new WeatherResponse.Weather();
                newWeather.setWeatherId(weather.getWeatherId());
                newWeather.setMain(weather.getMain());
                newWeather.setDescription(weather.getDescription());
                newWeather.setIcon(weather.getIcon());
                newWeatherList.add(newWeather);
            }
            weatherResponse.getCurrent().setWeather(newWeatherList);
        }

        if (weatherResponse.getDaily() != null) {
            for (WeatherResponse.Daily daily : weatherResponse.getDaily()) {
                if (daily.getWeather() != null) {
                    List<WeatherResponse.Weather> newWeatherList = new ArrayList<>();
                    for (WeatherResponse.Weather weather : daily.getWeather()) {
                        WeatherResponse.Weather newWeather = new WeatherResponse.Weather();
                        newWeather.setWeatherId(weather.getWeatherId());
                        newWeather.setMain(weather.getMain());
                        newWeather.setDescription(weather.getDescription());
                        newWeather.setIcon(weather.getIcon());
                        newWeatherList.add(newWeather);
                    }
                    daily.setWeather(newWeatherList);
                }
            }
        }

        if (weatherResponse.getHourly() != null) {
            for (WeatherResponse.Hourly hourly : weatherResponse.getHourly()) {
                if (hourly.getWeather() != null) {
                    List<WeatherResponse.Weather> newWeatherList = new ArrayList<>();
                    for (WeatherResponse.Weather weather : hourly.getWeather()) {
                        WeatherResponse.Weather newWeather = new WeatherResponse.Weather();
                        newWeather.setWeatherId(weather.getWeatherId());
                        newWeather.setMain(weather.getMain());
                        newWeather.setDescription(weather.getDescription());
                        newWeather.setIcon(weather.getIcon());
                        newWeatherList.add(newWeather);
                    }
                    hourly.setWeather(newWeatherList);
                }
            }
        }
    }

    private void updateExistingDayRecord(Field field, Day day, Plant plant) {
        logger.debug("Updating Day record for plantID={} on date={}.", plant.getPlantID(), day.getDate());

        // Ensure all 24 hours exist
        for (int hourIndex = 0; hourIndex < 24; hourIndex++) {
            // Check if the hour already exists
            int finalHourIndex = hourIndex;
            boolean exists = day.getHours().stream()
                    .anyMatch(hour -> hour.getHourIndex() == finalHourIndex);

            if (!exists) {
                // Create and add missing hour
                Hour newHour = new Hour(hourIndex, day);
                day.getHours().add(newHour);
                logger.info("Created missing hour record for hourIndex={} in Day ID={}.", hourIndex, day.getDayID());
            }
        }

        dayRepository.save(day);
        logger.info("Day record updated with missing hours for plantID={} on date={}.", plant.getPlantID(), day.getDate());
    }

    private void createHoursForDay(Day day) {
        for (int hourIndex = 0; hourIndex < 24; hourIndex++) {
            // Check if the hour already exists
            int finalHourIndex = hourIndex;
            boolean exists = day.getHours().stream()
                    .anyMatch(hour -> hour.getHourIndex() == finalHourIndex);

            if (!exists) {
                // Create and add hour
                Hour hourRecord = new Hour(hourIndex, day);
                day.getHours().add(hourRecord);
                logger.debug("Created hour record for hourIndex={} in Day ID={}.", hourIndex, day.getDayID());
            }
        }
    }

}
