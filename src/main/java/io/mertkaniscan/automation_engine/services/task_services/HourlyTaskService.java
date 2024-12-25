package io.mertkaniscan.automation_engine.services.task_services;

import io.mertkaniscan.automation_engine.models.*;
import io.mertkaniscan.automation_engine.repositories.DayRepository;
import io.mertkaniscan.automation_engine.repositories.FieldRepository;
import io.mertkaniscan.automation_engine.services.CalculatorService;
import io.mertkaniscan.automation_engine.services.forecast_services.solar_forecast_service.SolarResponse;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherForecastService;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherResponse;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class HourlyTaskService {

    private final FieldRepository fieldRepository;
    private final DayRepository dayRepository;
    private final WeatherForecastService weatherForecastService;
    private final CalculatorService calculatorService;

    public HourlyTaskService(FieldRepository fieldRepository,
                             DayRepository dayRepository,
                             WeatherForecastService weatherForecastService,
                             CalculatorService calculatorService) {

        this.fieldRepository = fieldRepository;
        this.dayRepository = dayRepository;
        this.weatherForecastService = weatherForecastService;
        this.calculatorService = calculatorService;
    }

    public void updateHourlyRecords() {
        log.info("Starting hourly record update task.");
        fieldRepository.findAll().forEach(this::updateFieldHourlyRecords);
        log.info("Hourly record update task completed.");
    }

    private void updateFieldHourlyRecords(Field field) {
        try {
            LocalDateTime now = LocalDateTime.now();
            int currentHour = now.getHour();

            Optional<Day> todayOpt = Optional.ofNullable(
                    dayRepository.findByPlant_PlantIDAndDate(
                            field.getPlantInField().getPlantID(),
                            Timestamp.valueOf(LocalDate.now().atStartOfDay())
                    )
            );

            if (todayOpt.isEmpty()) {
                log.warn("No Day record found for field '{}'. Skipping.", field.getFieldName());
                return;
            }

            Day today = todayOpt.get();
            today.getHours().stream()
                    .filter(hour -> hour.getHourIndex() == currentHour)
                    .findFirst()
                    .ifPresent(hour -> updateHourRecord(hour, field, currentHour));

        } catch (Exception e) {
            log.error("Error updating hourly records for field '{}'. Error: {}", field.getFieldName(), e.getMessage(), e);
        }
    }

    private void updateHourRecord(Hour hour, Field field, int currentHour) {
        try {
            // Fetch the weather data for the field's location
            WeatherResponse weatherResponse = weatherForecastService.getWeatherDataObj(
                    field.getLatitude(), field.getLongitude()
            );

            // Fetch the solar data from the associated Day entity
            SolarResponse solarResponse = hour.getDay().getSolarResponse();

            if (solarResponse == null) {
                throw new RuntimeException("Solar data is missing in the Day record for field: " + field.getFieldName());
            }

            // Retrieve hourly weather and solar data
            WeatherResponse.Hourly hourlyWeather = weatherResponse.getHourly().get(currentHour);
            SolarResponse.Irradiance.HourlyIrradiance hourlySolar = ;



            // Calculate the ETo for the current hour
            double eto = calculatorService.calculateEToHourly(weatherResponse, solarResponse, field, currentHour);

            // Update the Hour record
            hour.setSensorEToHourly(eto);
            hour.setLastUpdated(LocalDateTime.now());
            dayRepository.save(hour.getDay());

            log.info("Updated hourly record for hour={} in field '{}'.", currentHour, field.getFieldName());

        } catch (Exception e) {
            log.error("Failed to update Hour record for field '{}'. Error: {}", field.getFieldName(), e.getMessage(), e);
        }
    }

}