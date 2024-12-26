package io.mertkaniscan.automation_engine.services.task_services;

import io.mertkaniscan.automation_engine.models.*;
import io.mertkaniscan.automation_engine.repositories.DayRepository;
import io.mertkaniscan.automation_engine.repositories.FieldRepository;
import io.mertkaniscan.automation_engine.services.CalculatorService;
import io.mertkaniscan.automation_engine.services.forecast_services.solar_forecast_service.SolarResponse;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherForecastService;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherResponse;

import io.mertkaniscan.automation_engine.services.main_services.SensorDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final SensorDataService sensorDataService;

    public HourlyTaskService(FieldRepository fieldRepository,
                             DayRepository dayRepository,
                             WeatherForecastService weatherForecastService,
                             CalculatorService calculatorService, SensorDataService sensorDataService) {

        this.fieldRepository = fieldRepository;
        this.dayRepository = dayRepository;
        this.weatherForecastService = weatherForecastService;
        this.calculatorService = calculatorService;
        this.sensorDataService = sensorDataService;
    }

    public void updateHourlyRecords() {
        log.info("Starting hourly record update task.");
        fieldRepository.findAll().forEach(this::updateFieldHourlyRecords);
        log.info("Hourly record update task completed.");
    }

    @Transactional
    public void updateFieldHourlyRecords(Field field) {
        try {
            LocalDateTime now = LocalDateTime.now();
            int hourIndex = now.getHour();

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
                    .filter(hour -> hour.getHourIndex() == hourIndex)
                    .findFirst()
                    .ifPresent(hour -> updateHourRecord(hour, field, hourIndex));

        } catch (Exception e) {
            log.error("Error updating hourly records for field '{}'. Error: {}", field.getFieldName(), e.getMessage(), e);
        }
    }

    private void updateHourRecord(Hour hour, Field field, int hourIndex) {
        try {
            // pull fresh weather data
            WeatherResponse weatherResponse = weatherForecastService.getWeatherDataObj(
                    field.getLatitude(),
                    field.getLongitude()
            );

            //pull sensor datas from db if empty try to pull directly from device
            // Get mean sensor values for the last day
            double meanTemperature = sensorDataService.getMeanSensorDataByFieldIDAndType(field.getFieldID(), "temperature", 1);
            double meanHumidity = sensorDataService.getMeanSensorDataByFieldIDAndType(field.getFieldID(), "humidity", 1);

            // get previous solar data
            SolarResponse solarResponse = hour.getDay().getSolarResponse();

            if (solarResponse == null) {
                throw new RuntimeException("Solar data is missing in the Day record for field: " + field.getFieldName());
            }

            // Calculate the ETo for the current hour
            double eto = calculatorService.calculateEToHourly(
                    weatherResponse,
                    solarResponse,
                    field,
                    hourIndex);

            // Update the Hour record
            hour.setForecastTemperature(weatherResponse.getHourly().get(hourIndex).getTemp());
            hour.setForecastHumidity(weatherResponse.getHourly().get(hourIndex).getHumidity().doubleValue());

            hour.setLastUpdated(LocalDateTime.now());

            hour.setSensorTemperature(meanTemperature);
            hour.setSensorHumidity(meanHumidity);

            hour.setForecastEToHourly(eto);
            hour.setSensorEToHourly(eto);

            dayRepository.save(hour.getDay());

            log.info("Updated hourly record for hour={} in field '{}'.", hourIndex, field.getFieldName());

        } catch (Exception e) {
            log.error("Failed to update Hour record for field '{}'. Error: {}", field.getFieldName(), e.getMessage(), e);
        }
    }
}