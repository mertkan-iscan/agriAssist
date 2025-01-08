package io.mertkaniscan.automation_engine.services.task_services;

import io.mertkaniscan.automation_engine.models.*;
import io.mertkaniscan.automation_engine.repositories.DayRepository;
import io.mertkaniscan.automation_engine.repositories.FieldRepository;
import io.mertkaniscan.automation_engine.services.CalculatorService;
import io.mertkaniscan.automation_engine.services.EToCalculatorService;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.services.forecast_services.solar_forecast_service.SolarResponse;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherForecastService;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherResponse;
import io.mertkaniscan.automation_engine.services.main_services.SensorDataService;
import io.mertkaniscan.automation_engine.utils.calculators.Calculators;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class DailyTaskService {

    private final FieldRepository fieldRepository;
    private final DayRepository dayRepository;
    private final WeatherForecastService weatherForecastService;
    private final CalculatorService calculatorService;
    private final EToCalculatorService eToCalculatorService;
    private final FieldService fieldService;
    private final SensorDataService sensorDataService;

    public DailyTaskService(FieldRepository fieldRepository,
                            DayRepository dayRepository,
                            WeatherForecastService weatherForecastService,
                            CalculatorService calculatorService,
                            EToCalculatorService eToCalculatorService,
                            FieldService fieldService,
                            SensorDataService sensorDataService) {
        this.fieldRepository = fieldRepository;
        this.dayRepository = dayRepository;
        this.weatherForecastService = weatherForecastService;
        this.calculatorService = calculatorService;
        this.eToCalculatorService = eToCalculatorService;
        this.fieldService = fieldService;
        this.sensorDataService = sensorDataService;
    }

    @Transactional
    public void createDailyRecords() {
        log.info("Starting daily record creation task.");
        List<Field> fields = fieldRepository.findAll();

        for (Field field : fields) {
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
            Timestamp currentDate = Timestamp.valueOf(LocalDate.now().atStartOfDay());
            Day existingDay = dayRepository.findByPlant_PlantIDAndDate(plant.getPlantID(), currentDate);

            if (existingDay == null) {
                log.info("No Day record found. Creating a new record for plantID={} and date={}.", plant.getPlantID(), currentDate);
                createNewDayRecord(field, plant, currentDate);
            } else {
                log.info("Day record found for plantID={} and date={}. Updating record.", plant.getPlantID(), currentDate);
                updateExistingDayRecord(field, existingDay);
            }
        } catch (Exception e) {
            log.error("Error processing field '{}': {}", field.getFieldName(), e.getMessage(), e);
        }
    }

    private void createNewDayRecord(Field field, Plant plant, Timestamp currentDate) {
        try {
            WeatherResponse weatherResponse = fieldService.getWeatherDataByFieldId(field.getFieldID());
            SolarResponse solarResponse = fieldService.getSolarDataByFieldId(field.getFieldID(), LocalDate.now());

            Day newDay = initializeDay(plant, currentDate, weatherResponse, solarResponse);
            int currentHourIndex = LocalDateTime.now().getHour();

            for (int hourIndex = 0; hourIndex < 24; hourIndex++) {
                Hour newHour = new Hour(hourIndex, newDay);
                newDay.getHours().add(newHour);

                if (hourIndex <= currentHourIndex) {
                    updatePastHourData(newHour, field, hourIndex, newDay);
                } else {
                    updateFutureHourData(newHour, field, hourIndex, newDay);
                }
            }

            calculateWaterVariables(newDay, field);
            dayRepository.save(newDay);

            log.info("Successfully created Day record with 24 Hour entries for plantID={} on date={}.", plant.getPlantID(), currentDate);
        } catch (Exception e) {
            log.error("Error creating Day record for field '{}': {}", field.getFieldName(), e.getMessage(), e);
        }
    }

    private Day initializeDay(Plant plant, Timestamp currentDate, WeatherResponse weatherResponse, SolarResponse solarResponse) {
        Day newDay = new Day();
        newDay.setPlant(plant);
        newDay.setDate(currentDate);

        newDay.setSunrise(new Timestamp(weatherResponse.getCurrent().getSunrise() * 1000L));
        newDay.setSunset(new Timestamp(weatherResponse.getCurrent().getSunset() * 1000L));
        newDay.setWeatherResponse(weatherResponse);
        newDay.setSolarResponse(solarResponse);

        Double guessedEto = eToCalculatorService.calculateEToDaily(weatherResponse, solarResponse, plant.getField());
        newDay.setGuessedEtoDaily(guessedEto);

        return newDay;
    }

    private void updateExistingDayRecord(Field field, Day day) {
        int currentHourIndex = LocalDateTime.now().getHour();

        for (int hourIndex = 0; hourIndex < 24; hourIndex++) {

            int finalHourIndex = hourIndex;
            Optional<Hour> existingHour = day.getHours().stream()
                    .filter(hour -> hour.getHourIndex() == finalHourIndex)
                    .findFirst();

            if (existingHour.isEmpty()) {

                Hour newHour = new Hour(hourIndex, day);
                day.getHours().add(newHour);

                if (hourIndex <= currentHourIndex) {

                    updatePastHourData(newHour, field, hourIndex, day);
                } else {

                    updateFutureHourData(newHour, field, hourIndex, day);
                }

            } else {

                Hour hour = existingHour.get();

                if (hourIndex <= currentHourIndex) {

                    updatePastHourData(hour, field, hourIndex, day);
                } else {

                    updateFutureHourData(hour, field, hourIndex, day);
                }
            }
        }
        dayRepository.save(day);
    }

    private void updatePastHourData(Hour hour, Field field, int hourIndex, Day day) {
        try {
            Timestamp previousHourTimestamp;
            Timestamp currentHourTimestamp = hour.getTimestamp();

            WeatherResponse weatherResponse;
            SolarResponse solarResponse;

            if (hourIndex == 0) {
                previousHourTimestamp = Timestamp.valueOf(LocalDate.now().minusDays(1).atTime(23, 0));
                Day previousDay = dayRepository.findByPlant_PlantIDAndDate(field.getPlantInField().getPlantID(), previousHourTimestamp);
                if (previousDay != null) {
                    weatherResponse = previousDay.getWeatherResponse();
                    solarResponse = previousDay.getSolarResponse();
                } else {
                    log.warn("Previous day data not found for plantID={} at date={}. Skipping calculations.", field.getPlantInField().getPlantID(), previousHourTimestamp);
                    return;
                }
            } else {
                previousHourTimestamp = Timestamp.valueOf(LocalDate.now().atTime(hourIndex - 1, 0));
                weatherResponse = day.getWeatherResponse();
                solarResponse = day.getSolarResponse();
            }

            if (field.getFieldType() == Field.FieldType.GREENHOUSE) {

                Double meanTemperature = sensorDataService.getMeanValueBetweenTimestamps(field.getFieldID(), "weather_temp", previousHourTimestamp, currentHourTimestamp);
                Double meanHumidity = sensorDataService.getMeanValueBetweenTimestamps(field.getFieldID(), "weather_hum", previousHourTimestamp, currentHourTimestamp);

                if (meanTemperature != null && meanHumidity != null) {

                    hour.setSensorTemperature(meanTemperature);
                    hour.setSensorHumidity(meanHumidity);
                    double eto = eToCalculatorService.calculateSensorEToHourly(meanTemperature, meanHumidity, weatherResponse, solarResponse, field, hourIndex);
                    hour.setSensorEToHourly(eto);

                } else {
                    log.warn("Sensor data is missing for hourIndex={} in Day ID={}. Skipping sensor-based calculations.", hourIndex, day.getDayID());
                }
            } else if (field.getFieldType() == Field.FieldType.OUTDOOR) {


                double eto = eToCalculatorService.calculateForecastEToHourly(weatherResponse, solarResponse, field, hourIndex);
                hour.setForecastEToHourly(eto);
            }

            Double TAW = calculatorService.calculateSensorTAW(field, previousHourTimestamp, currentHourTimestamp);
            if (TAW != null) {
                hour.setTAWValueHourly(TAW);
            } else {
                log.warn("TAW calculation returned null for field '{}' between {} and {}.", field.getFieldName(), previousHourTimestamp, currentHourTimestamp);
            }

            Double TEW = calculatorService.calculateSensorTEW(field, previousHourTimestamp, currentHourTimestamp);
            if (TEW != null) {
                hour.setTEWValueHourly(TEW);
            } else {
                log.warn("TEW calculation returned null for field '{}' between {} and {}.", field.getFieldName(), previousHourTimestamp, currentHourTimestamp);
            }

            Double RAW = (TAW != null) ? calculatorService.calculateSensorRAW(TAW, field) : null;
            if (RAW != null) {
                hour.setRAWValueHourly(RAW);
            } else {
                log.warn("RAW calculation returned null for field '{}' using TAW={}.", field.getFieldName(), TAW);
            }

            Double REW = (TEW != null) ? calculatorService.calculateSensorREW(TEW, field) : null;
            if (REW != null) {
                hour.setREWValueHourly(REW);
            } else {
                log.warn("REW calculation returned null for field '{}' using TEW={}.", field.getFieldName(), TEW);
            }

            Double Kr = (TEW != null && REW != null) ? calculatorService.calculateSensorKr(field, TEW, REW) : null;
            if (Kr != null) {
                hour.setKrValue(Kr);
            } else {
                log.warn("Kr calculation returned null for field '{}' using TEW={} and REW={}.", field.getFieldName(), TEW, REW);
            }

            hour.setLastUpdated(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error updating past hour data for hourIndex={} in Day ID={}: {}", hourIndex, day.getDayID(), e.getMessage(), e);
        }
    }

    private void updateFutureHourData(Hour hour, Field field, int hourIndex, Day day) {
        try {

            if (field.getFieldType() == Field.FieldType.GREENHOUSE) {

            }else if (field.getFieldType() == Field.FieldType.OUTDOOR) {
                WeatherResponse weather = fieldService.getWeatherDataByFieldId(field.getFieldID());
                SolarResponse solar = fieldService.getSolarDataByFieldId(field.getFieldID(), LocalDate.now());

                hour.setForecastEToHourly(eToCalculatorService.calculateForecastEToHourly(weather, solar, field, hourIndex));

            }

            hour.setIrrigationWetArea(0.0);
            hour.setRainWetArea(0.0);

        } catch (Exception e) {
            log.error("Error updating future hour data for hourIndex={} in Day ID={}: {}", hourIndex, day.getDayID(), e.getMessage(), e);
        }
    }

    private void calculateWaterVariables(Day day, Field field) {
        try {
            Double TAW = calculatorService.calculateSensorTAW(field, 10);
            Double TEW = calculatorService.calculateSensorTEW(field, 10);
            Double RAW = calculatorService.calculateSensorRAW(TAW != null ? TAW : 0.0, field);
            Double REW = calculatorService.calculateSensorREW(TEW != null ? TEW : 0.0, field);

            day.setTAWValueDaily(TAW != null ? TAW : 0.0);
            day.setTEWValueDaily(TEW != null ? TEW : 0.0);
            day.setRAWValueDaily(RAW != null ? RAW : 0.0);
            day.setREWValueDaily(REW != null ? REW : 0.0);

        } catch (Exception e) {
            log.error("Error calculating water variables for Day ID={}: {}", day.getDayID(), e.getMessage(), e);
        }
    }
}