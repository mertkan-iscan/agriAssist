package io.mertkaniscan.automation_engine.services.task_services;

import io.mertkaniscan.automation_engine.models.*;
import io.mertkaniscan.automation_engine.repositories.DayRepository;
import io.mertkaniscan.automation_engine.repositories.FieldRepository;
import io.mertkaniscan.automation_engine.services.CalculatorService;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.services.forecast_services.solar_forecast_service.SolarResponse;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherForecastService;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class DailyTaskService {

    private final FieldRepository fieldRepository;
    private final DayRepository dayRepository;
    private final WeatherForecastService weatherForecastService;
    private final CalculatorService calculatorService;
    private final FieldService fieldService;

    public DailyTaskService(FieldRepository fieldRepository,
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
        log.info("Starting daily record creation task.");
        List<Field> fields = fieldRepository.findAll();
        log.debug("Fetched {} fields from database.", fields.size());

        for (Field field : fields) {
            log.debug("Processing field: {}", field.getFieldName());
            try {
                processField(field);
            } catch (Exception e) {
                log.error("Failed to process field '{}': {}", field.getFieldName(), e.getMessage(), e);
            }
        }
        log.info("Daily record creation task completed.");
    }

    private void processField(Field field) {
        Plant plant = field.getPlantInField();
        if (plant == null) {
            log.warn("Field '{}' has no associated plant. Skipping.", field.getFieldName());
            return;
        }

        try {
            processDailyRecord(field, plant);
        } catch (Exception e) {
            log.error("Error creating daily record for field '{}': {}", field.getFieldName(), e.getMessage(), e);
        }
    }

    private void processDailyRecord(Field field, Plant plant) {
        Timestamp currentDate = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        log.debug("Checking existing Day record for plantID={} and date={}.", plant.getPlantID(), currentDate);

        Day existingDay = dayRepository.findByPlant_PlantIDAndDate(plant.getPlantID(), currentDate);

        if (existingDay == null) {
            log.info("No Day record found. Creating new record for plantID={} and date={}.", plant.getPlantID(), currentDate);
            createNewDayRecord(field, plant, currentDate);
        } else {
            log.info("Day record found for plantID={} and date={}. Updating record.", plant.getPlantID(), currentDate);
            updateExistingDayRecord(field, existingDay, plant);
        }
    }

    private void createNewDayRecord(Field field, Plant plant, Timestamp currentDate) {
        try {

            WeatherResponse weatherResponse = fieldService.getWeatherDataByFieldId(field.getFieldID());
            SolarResponse solarResponse = fieldService.getSolarDataByFieldId(field.getFieldID(), LocalDate.now());

            Day day = setStaticDayVariables(weatherResponse, solarResponse, field, plant, currentDate);
            day = dayRepository.save(day);

            createHoursForDay(weatherResponse, solarResponse, field, day);
            dayRepository.save(day);

            log.info("Successfully created new Day record for plantID={} on date={}.", plant.getPlantID(), currentDate);
        } catch (Exception e) {
            log.error("Error creating daily record for field '{}': {}", field.getFieldName(), e.getMessage(), e);
            throw e;
        }
    }

    private Day setStaticDayVariables(WeatherResponse weatherResponse, SolarResponse solarResponse, Field field, Plant plant, Timestamp currentDate) {
        try {

            long sunriseEpoch = weatherResponse.getCurrent().getSunrise();
            long sunsetEpoch = weatherResponse.getCurrent().getSunset();

            Timestamp sunrise = new Timestamp(sunriseEpoch * 1000L);
            Timestamp sunset = new Timestamp(sunsetEpoch * 1000L);

            // Create new Day entity
            Day day = new Day();

            day.setDailyDepletion(field.getCurrentDeValue());

            day.setDate(currentDate);
            day.setPlant(plant);

            day.setSunrise(sunrise);
            day.setSunset(sunset);

            day.setWeatherResponse(weatherResponse);
            day.setSolarResponse(solarResponse);

            Double guessedEto = calculatorService.calculateEToDaily(weatherResponse, solarResponse, field);
            day.setGuessedEtoDaily(guessedEto);

            return day;

        } catch (Exception e) {
            log.error("Error in setStaticDayVariables: {}", e.getMessage(), e);
            throw e;
        }
    }


    private void createHoursForDay(WeatherResponse weatherResponse, SolarResponse solarResponse, Field field, Day day) {

        for (int hourIndex = 0; hourIndex < 24; hourIndex++) {

            // Check if the hour already exists
            int finalHourIndex = hourIndex;

            boolean exists = day.getHours().stream()
                    .anyMatch(hour -> hour.getHourIndex() == finalHourIndex);

            if (!exists) {
                // Create and add hour
                Hour hourRecord = setStaticHourVariables(weatherResponse, solarResponse, field, hourIndex, day);

                day.getHours().add(hourRecord);

                log.debug("Created hour record for hourIndex={} in Day ID={}.", hourIndex, day.getDayID());
            }
        }
    }

    private Hour setStaticHourVariables(WeatherResponse weatherResponse, SolarResponse solarResponse, Field field, int hourIndex, Day day) {
        try {

            Double forecastRH = day.getWeatherResponse().getHourly().get(hourIndex).getHumidity().doubleValue();
            Double forecastTemp = day.getWeatherResponse().getHourly().get(hourIndex).getTemp();
            Double windSpeed = day.getWeatherResponse().getHourly().get(hourIndex).getWindSpeed();

            Hour hourRecord = new Hour();
            hourRecord.setHourIndex(hourIndex);
            hourRecord.setDay(day);
            hourRecord.setForecastTemperature(forecastTemp);
            hourRecord.setForecastHumidity(forecastRH);
            hourRecord.setForecastWindSpeed(windSpeed);


            double eto = calculatorService.calculateForecastEToHourly(
                    day.getWeatherResponse(),
                    day.getSolarResponse(),
                    field,
                    hourIndex);

            hourRecord.setGuessedEtoHourly(eto);

            return hourRecord;

        } catch (Exception e) {
            log.error("Error in setStaticDayVariables: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void updateExistingDayRecord(Field field, Day day, Plant plant) {
        log.debug("Updating Day record for plantID={} on date={}.", plant.getPlantID(), day.getDate());

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

                log.info("Created missing hour record for hourIndex={} in Day ID={}.", hourIndex, day.getDayID());
            }
        }

        dayRepository.save(day);
        log.info("Day record updated with missing hours for plantID={} on date={}.", plant.getPlantID(), day.getDate());
    }

}
