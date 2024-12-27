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

            Day today = dayRepository.findByPlant_PlantIDAndDateWithHours(
                    field.getPlantInField().getPlantID(),
                    Timestamp.valueOf(LocalDate.now().atStartOfDay())
            );

            if (today == null) {
                log.warn("No Day record found for field '{}'. Skipping.", field.getFieldName());
                return;
            }

            today.getHours().stream()
                    .filter(hour -> hour.getHourIndex() == hourIndex)
                    .findFirst()
                    .ifPresent(hour -> {
                        updateHourRecord(hour, field, hourIndex);
                        //updateDepletionValue(hour, field);
                    });

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

            // Timestamp for the current hour
            Timestamp currentHourTimestamp = Timestamp.valueOf(LocalDateTime.now().minusHours(1));

            // Get mean sensor values using the new method
            Double meanTemperature = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(field.getFieldID(), "weather_hum", currentHourTimestamp);
            Double meanHumidity = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(field.getFieldID(), "weather_temp", currentHourTimestamp);

            if (meanTemperature == null || meanHumidity == null) {
                log.warn("Sensor data missing for field '{}', skipping hour update.", field.getFieldName());
                return;
            }

            log.info("mean temperature: {}", meanTemperature);
            log.info("mean humidity: {}", meanHumidity);

            // get previous solar data
            SolarResponse solarResponse = hour.getDay().getSolarResponse();

            if (solarResponse == null) {
                throw new RuntimeException("Solar data is missing in the Day record for field: " + field.getFieldName());
            }

            // Calculate the ETo for the current hour
            double sensorEToHourly = calculatorService.calculateSensorEToHourly(
                    meanTemperature,
                    meanHumidity,
                    weatherResponse,
                    solarResponse,
                    field,
                    hourIndex);

            double currentEToHourly = calculatorService.calculateCurrentEToHourly(
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

            hour.setForecastEToHourly(currentEToHourly);
            hour.setSensorEToHourly(sensorEToHourly);

            dayRepository.save(hour.getDay());

            log.info("Updated hourly record for hour={} in field '{}'.", hourIndex, field.getFieldName());

        } catch (Exception e) {
            log.error("Failed to update Hour record for field '{}'. Error: {}", field.getFieldName(), e.getMessage(), e);
        }
    }

    private void updateDepletionValue(Hour hour, Field field) {
        try {
            Timestamp currentHourTimestamp = Timestamp.valueOf(LocalDateTime.now().minusHours(1));
            double fieldCapacity = field.getFieldCapacity();
            double currentSoilMoisture = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(field.getFieldID(), "soil_moisture", currentHourTimestamp);
            double depletionValue = fieldCapacity - currentSoilMoisture;

            hour.setDeValue(depletionValue);
            hour.setLastUpdated(LocalDateTime.now());

            dayRepository.save(hour.getDay());

            log.info("Updated depletion value for hour={} in field '{}'. Depletion Value: {}", hour.getHourIndex(), field.getFieldName(), depletionValue);

        } catch (Exception e) {
            log.error("Failed to update Depletion value for hour={} in field '{}'. Error: {}", hour.getHourIndex(), field.getFieldName(), e.getMessage(), e);
        }
    }
}
