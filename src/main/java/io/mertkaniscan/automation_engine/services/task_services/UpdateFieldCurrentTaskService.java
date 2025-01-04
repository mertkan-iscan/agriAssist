package io.mertkaniscan.automation_engine.services.task_services;

import io.mertkaniscan.automation_engine.models.*;
import io.mertkaniscan.automation_engine.repositories.DayRepository;
import io.mertkaniscan.automation_engine.repositories.FieldRepository;
import io.mertkaniscan.automation_engine.services.CalculatorService;
import io.mertkaniscan.automation_engine.services.EToCalculatorService;
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
public class UpdateFieldCurrentTaskService {

    private final FieldRepository fieldRepository;
    private final DayRepository dayRepository;
    private final WeatherForecastService weatherForecastService;
    private final CalculatorService calculatorService;
    private final EToCalculatorService eToCalculatorService;
    private final SensorDataService sensorDataService;

    public UpdateFieldCurrentTaskService(FieldRepository fieldRepository,
                                         DayRepository dayRepository,
                                         WeatherForecastService weatherForecastService,
                                         CalculatorService calculatorService, EToCalculatorService eToCalculatorService, SensorDataService sensorDataService) {

        this.fieldRepository = fieldRepository;
        this.dayRepository = dayRepository;
        this.weatherForecastService = weatherForecastService;
        this.calculatorService = calculatorService;
        this.eToCalculatorService = eToCalculatorService;
        this.sensorDataService = sensorDataService;
    }

    public void updateFieldCurrentValues() {
        log.info("Starting hourly record update task.");
        fieldRepository.findAll().forEach(this::updateFieldHourlyRecords);
        log.info("Hourly record update task completed.");
    }

    @Transactional
    public void updateFieldHourlyRecords(Field field) {
        try {

            if (field.getPlantInField() == null) {
                log.warn("No plant associated with field '{}'. Skipping field current update.", field.getFieldName());
                return;
            }

            updateFieldCurrentValues(field);

        } catch (Exception e) {
            log.error("Error updating hourly records for field '{}'. Error: {}", field.getFieldName(), e.getMessage(), e);
        }
    }

    private void updateFieldCurrentValues(Field field) {
        try {

            LocalDateTime now = LocalDateTime.now();
            int hourIndex = now.getHour();

            Day today = dayRepository.findByPlant_PlantIDAndDateWithHours(
                    field.getPlantInField().getPlantID(),
                    Timestamp.valueOf(LocalDate.now().atStartOfDay())
            );
            log.info("Today's day record pulled successfully.");

            WeatherResponse weatherResponse = weatherForecastService.getWeatherDataObj(
                    field.getLatitude(),
                    field.getLongitude()
            );
            log.info("Weather response pulled successfully.");

            // get solar data
            SolarResponse solarResponse = today.getSolarResponse();
            if (solarResponse == null) {
                throw new RuntimeException("Solar data is missing in the Day record for field: " + field.getFieldName());
            }
            log.info("Solar response pulled from db successfully.");

            Timestamp currentHourTimestamp = Timestamp.valueOf(LocalDateTime.now().minusHours(1));
            Double meanTemperature = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(field.getFieldID(), "weather_temp", currentHourTimestamp);
            Double meanHumidity = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(field.getFieldID(), "weather_hum", currentHourTimestamp);
            log.info("Sensor data pulled successfully.");

//            double sensorEToHourly = eToCalculatorService.calculateSensorEToHourly(
//                    meanTemperature,
//                    meanHumidity,
//                    weatherResponse,
//                    solarResponse,
//                    field,
//                    hourIndex);
//
//            double currentEToHourly = eToCalculatorService.calculateCurrentEToHourly(
//                    weatherResponse,
//                    solarResponse,
//                    field,
//                    hourIndex);
//
//            field.getCurrentValues().setForecastETo(currentEToHourly);
//            field.getCurrentValues().setSensorETo(sensorEToHourly);



            FieldCurrentValues newCurrentValues = field.getCurrentValues();

            if (newCurrentValues == null) {

                newCurrentValues = new FieldCurrentValues();
                newCurrentValues.setField(field);

                field.setCurrentValues(newCurrentValues);
            } else {

                resetFieldCurrentValues(newCurrentValues);
            }

            newCurrentValues.setDeValue(0.0);
            newCurrentValues.setWetArea(0.0);

            newCurrentValues.setForecastTemperature(weatherResponse.getCurrent().getTemp());
            newCurrentValues.setForecastHumidity(weatherResponse.getCurrent().getHumidity().doubleValue());
            newCurrentValues.setForecastWindSpeed(weatherResponse.getCurrent().getWindSpeed());

            newCurrentValues.setSensorTemperature(meanTemperature);
            newCurrentValues.setSensorHumidity(meanHumidity);

            newCurrentValues.setSolarRadiation(
                    eToCalculatorService.calculateSolarRadiationHourly(
                    weatherResponse,
                    solarResponse,
                    hourIndex));

            WeatherResponse.Rain rain = weatherResponse.getCurrent().getRain();
            if (rain != null && rain.getOneHour() != null && rain.getOneHour() > 0) {
                newCurrentValues.setIsRaining(true);
            } else {
                newCurrentValues.setIsRaining(false);
            }

            newCurrentValues.setTewValue(calculatorService.calculateSensorTEW(field, 10));
            newCurrentValues.setTawValue(calculatorService.calculateSensorTAW(field, 10));
            field.setCurrentValues(newCurrentValues);

            newCurrentValues.setRewValue(calculatorService.calculateSensorREW(field.getCurrentValues().getTewValue(), field));
            newCurrentValues.setRawValue(calculatorService.calculateSensorRAW(field.getCurrentValues().getTawValue(), field));

            newCurrentValues.setKeValue(calculatorService.calculateKe(field));
            field.setCurrentValues(newCurrentValues);

            fieldRepository.save(field);

            log.info("Updated current record for field '{}'.", field.getFieldName());
            log.info("Current values: {}", field.getCurrentValues());

        } catch (Exception e) {
            log.error("Failed to update current record for field '{}'. Error: {}", field.getFieldName(), e.getMessage(), e);
        }
    }

    private void resetFieldCurrentValues(FieldCurrentValues currentValues) {
        currentValues.setDeValue(null);
        currentValues.setWetArea(null);
        currentValues.setForecastTemperature(null);
        currentValues.setForecastHumidity(null);
        currentValues.setForecastWindSpeed(null);
        currentValues.setSensorTemperature(null);
        currentValues.setSensorHumidity(null);
        currentValues.setSolarRadiation(null);
        currentValues.setIsRaining(null);
        currentValues.setTewValue(null);
        currentValues.setTawValue(null);
        currentValues.setRewValue(null);
        currentValues.setRawValue(null);
        currentValues.setKeValue(null);
    }
}
