package io.mertkaniscan.automation_engine.services.task_services;

import io.mertkaniscan.automation_engine.models.Day;
import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.models.Hour;
import io.mertkaniscan.automation_engine.models.Plant;
import io.mertkaniscan.automation_engine.repositories.DayRepository;
import io.mertkaniscan.automation_engine.repositories.FieldRepository;
import io.mertkaniscan.automation_engine.services.CalculatorService;
import io.mertkaniscan.automation_engine.services.EToCalculatorService;
import io.mertkaniscan.automation_engine.services.forecast_services.solar_forecast_service.SolarResponse;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherForecastService;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherResponse;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.services.main_services.SensorDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class StartDailyTaskService {

    private final FieldRepository fieldRepository;
    private final DayRepository dayRepository;
    private final WeatherForecastService weatherForecastService;
    private final CalculatorService calculatorService;
    private final EToCalculatorService eToCalculatorService;
    private final FieldService fieldService;
    private final SensorDataService sensorDataService;

    public StartDailyTaskService(FieldRepository fieldRepository,
                                 DayRepository dayRepository,
                                 WeatherForecastService weatherForecastService,
                                 CalculatorService calculatorService, EToCalculatorService eToCalculatorService, FieldService fieldService, SensorDataService sensorDataService) {

        this.fieldRepository = fieldRepository;
        this.dayRepository = dayRepository;
        this.weatherForecastService = weatherForecastService;
        this.calculatorService = calculatorService;
        this.eToCalculatorService = eToCalculatorService;
        this.fieldService = fieldService;
        this.sensorDataService = sensorDataService;
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
            log.info("No Day record found.");
        } else {
            log.info("Day record found for plantID={} and date={}. Updating record.", plant.getPlantID(), currentDate);
            updateExistingDayRecord(field, existingDay, plant);
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

            day.setDate(currentDate);
            day.setPlant(plant);

            day.setSunrise(sunrise);
            day.setSunset(sunset);

            day.setWeatherResponse(weatherResponse);
            day.setSolarResponse(solarResponse);

            Double guessedEto = eToCalculatorService.calculateEToDaily(weatherResponse, solarResponse, field);
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


            double eto = eToCalculatorService.calculateForecastEToHourly(
                    weatherResponse,
                    solarResponse,
                    field,
                    hourIndex);

            hourRecord.setGuessedEtoHourly(eto);

            WeatherResponse.Rain rain = weatherResponse.getHourly().get(hourIndex).getRain();
            if (rain != null && rain.getOneHour() != null) {
                hourRecord.setForecastPrecipitation(rain.getOneHour());
            } else {
                hourRecord.setForecastPrecipitation(0.0);
            }

            return hourRecord;

        } catch (Exception e) {
            log.error("Error in setStaticDayVariables: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void updateExistingDayRecord(Field field, Day day, Plant plant) {

    }

    private void updateHourData(Hour hour, Field field, int hourIndex, Day day) {

        try {
            LocalDate currentDate = LocalDate.now();
            Timestamp startOfHour = Timestamp.valueOf(currentDate.atTime(hourIndex, 0));
            Timestamp endOfHour = Timestamp.valueOf(currentDate.atTime(hourIndex, 59, 59));

            WeatherResponse weatherResponse = fieldService.getWeatherDataByFieldId(field.getFieldID());
            SolarResponse solarResponse = fieldService.getSolarDataByFieldId(field.getFieldID(), currentDate);

            // Process only past hours
            if (endOfHour.before(Timestamp.valueOf(LocalDateTime.now()))) {
                Double meanTemperature = sensorDataService.getMeanValueBetweenTimestamps(startOfHour, endOfHour, "weather_temp");
                Double meanHumidity = sensorDataService.getMeanValueBetweenTimestamps(startOfHour, endOfHour, "weather_hum");

                hour.setSensorHumidity(meanHumidity);
                hour.setSensorTemperature(meanTemperature);
                hour.setLastUpdated(LocalDateTime.now());

                if (meanTemperature != null && meanHumidity != null ) {
                    double sensorETo = eToCalculatorService.calculateSensorEToHourly(
                            meanTemperature,
                            meanHumidity,
                            weatherResponse,
                            solarResponse,
                            field,
                            hourIndex
                    );

                    hour.setSensorEToHourly(sensorETo);
                    log.info("Updated sensor-based ETo for hourIndex={} in Day ID={}: {}", hourIndex, day.getDayID(), sensorETo);
                    return;
                } else {
                    log.warn("Insufficient sensor data for ETo calculation at hourIndex={} in Day ID={}", hourIndex, day.getDayID());
                }
            }


            Double forecastRH = weatherResponse.getHourly().get(hourIndex).getHumidity().doubleValue();
            Double forecastTemp = weatherResponse.getHourly().get(hourIndex).getTemp();
            Double windSpeed = weatherResponse.getHourly().get(hourIndex).getWindSpeed();

            hour.setForecastTemperature(forecastTemp);
            hour.setForecastHumidity(forecastRH);
            hour.setForecastWindSpeed(windSpeed);

            double eto = eToCalculatorService.calculateForecastEToHourly(
                    weatherResponse, solarResponse, field, hourIndex
            );

            hour.setGuessedEtoHourly(eto);

            WeatherResponse.Hourly hourlyData = weatherResponse.getHourly().get(hourIndex);
            double pop = hourlyData.getPop();
            WeatherResponse.Rain rain = hourlyData.getRain();

            hour.setForecastPrecipitation(pop >= 0.3 && rain != null && rain.getOneHour() != null ? rain.getOneHour() : 0.0);

            log.debug("Updated hour record: {}", hour);

        } catch (Exception e) {
            log.error("Error updating hour data for hourIndex={} in Day ID={}: {}", hourIndex, day.getDayID(), e.getMessage(), e);
        }
    }
}
