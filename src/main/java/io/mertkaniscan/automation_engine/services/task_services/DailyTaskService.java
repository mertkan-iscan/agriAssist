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
                            CalculatorService calculatorService, EToCalculatorService eToCalculatorService, FieldService fieldService, SensorDataService sensorDataService) {
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

            int currentHourIndex = LocalDateTime.now().getHour();

            for (int hourIndex = 0; hourIndex < 24; hourIndex++) {

                Hour newHour = new Hour(hourIndex, day);
                day.getHours().add(newHour);

                if (hourIndex <= currentHourIndex) {
                    updatePastHourData(newHour, field, hourIndex, day);
                }
                else {
                    updateFutureHourData(newHour, field, hourIndex, day);
                }
            }

            dayRepository.save(day);

            log.info("Successfully created new Day record with 24 Hour entries for plantID={} on date={}.",
                    plant.getPlantID(), currentDate);
        } catch (Exception e) {
            log.error("Error creating daily record for field '{}': {}", field.getFieldName(), e.getMessage(), e);
            throw e; // Re-throw to handle it further up if needed
        }
    }

    private Day setStaticDayVariables(WeatherResponse weatherResponse, SolarResponse solarResponse, Field field, Plant plant, Timestamp currentDate) {
        try {

            long sunriseEpoch = weatherResponse.getCurrent().getSunrise();
            long sunsetEpoch = weatherResponse.getCurrent().getSunset();
            Timestamp sunrise = new Timestamp(sunriseEpoch * 1000L);
            Timestamp sunset = new Timestamp(sunsetEpoch * 1000L);

            Double TAW = calculatorService.calculateSensorTAW(field, 10);
            Double TEW = calculatorService.calculateSensorTEW(field, 10);
            Double RAW = calculatorService.calculateSensorRAW(TAW, field);
            Double REW = calculatorService.calculateSensorREW(TEW, field);

            Double Kr = calculatorService.calculateSensorKr(field);

            // Create new Day entity
            Day day = new Day();
            day.setPlant(plant);
            day.setDate(currentDate);

            day.setSunrise(sunrise);
            day.setSunset(sunset);

            day.setWeatherResponse(weatherResponse);
            day.setSolarResponse(solarResponse);

            day.setTAWValueDaily(TAW);
            day.setTEWValueDaily(TEW);
            day.setRAWValueDaily(RAW);
            day.setREWValueDaily(REW);

            Double guessedEto = eToCalculatorService.calculateEToDaily(weatherResponse, solarResponse, field);
            day.setGuessedEtoDaily(guessedEto);

            return day;

        } catch (Exception e) {
            log.error("Error in setStaticDayVariables: {}", e.getMessage(), e);
            throw e;
        }
    }


    private void updateExistingDayRecord(Field field, Day day, Plant plant) {
        log.debug("Updating Day record for plantID={} on date={}.", plant.getPlantID(), day.getDate());

        int currentHourIndex = LocalDateTime.now().getHour();


        for (int hourIndex = 0; hourIndex < 24; hourIndex++) {

            int finalHourIndex = hourIndex;
            Optional<Hour> existingHourOpt = day.getHours().stream()
                    .filter(hour -> hour.getHourIndex() == finalHourIndex)
                    .findFirst();

            if (existingHourOpt.isEmpty()) {

                Hour newHour = new Hour(hourIndex, day);
                day.getHours().add(newHour);

                if (hourIndex <= currentHourIndex) {

                    updatePastHourData(newHour, field, hourIndex, day);
                } else {
                    updateFutureHourData(newHour, field, hourIndex, day);
                }

                log.info("Created missing hour record for hourIndex={} in Day ID={}.", hourIndex, day.getDayID());

            } else {
                Hour existingHour = existingHourOpt.get();

                if (hourIndex <= currentHourIndex) {

                    updatePastHourData(existingHour, field, hourIndex, day);
                } else {
                    updateFutureHourData(existingHour, field, hourIndex, day);
                }
            }
        }

        dayRepository.save(day);
        log.info("Day record updated with missing and outdated hours for plantID={} on date={}.",
                plant.getPlantID(), day.getDate());
    }


    private void updatePastHourData(Hour hour, Field field, int hourIndex, Day day) {
        try {
            LocalDate currentDate = LocalDate.now();
            Timestamp startOfHour = Timestamp.valueOf(currentDate.atTime(hourIndex, 0));
            Timestamp endOfHour = Timestamp.valueOf(currentDate.atTime(hourIndex, 59, 59));

            WeatherResponse freshWeatherResponse = fieldService.getWeatherDataByFieldId(field.getFieldID());
            SolarResponse freshSolarResponse = fieldService.getSolarDataByFieldId(field.getFieldID(), currentDate);


            Double meanTemperature = sensorDataService.getMeanValueBetweenTimestamps(
                    field.getFieldID(), "weather_temp", startOfHour, endOfHour);

            Double meanHumidity = sensorDataService.getMeanValueBetweenTimestamps(
                    field.getFieldID(), "weather_hum", startOfHour, endOfHour);

            if (meanTemperature != null && meanHumidity != null) {
                double sensorETo = eToCalculatorService.calculateSensorEToHourly(
                        meanTemperature,
                        meanHumidity,
                        freshWeatherResponse,
                        freshSolarResponse,
                        field,
                        hourIndex
                );

                hour.setSensorEToHourly(sensorETo);
                hour.setSensorTemperature(meanTemperature);
                hour.setSensorHumidity(meanHumidity);
                hour.setLastUpdated(LocalDateTime.now());



                Double TAW = calculatorService.calculateSensorTAW(field, startOfHour, endOfHour);
                Double TEW = calculatorService.calculateSensorTEW(field, startOfHour, endOfHour);
                Double RAW = calculatorService.calculateSensorRAW(TAW, field);
                Double REW = calculatorService.calculateSensorREW(TEW, field);
                Double Kr = calculatorService.calculateSensorKr(field);



                hour.setTAWValueHourly(TAW);
                hour.setTEWValueHourly(TEW);

                hour.setRAWValueHourly(RAW);
                hour.setREWValueHourly(REW);

                hour.setKrValue(Kr);


            } else {
                log.warn("Insufficient sensor data for ETo calculation at hourIndex={} in Day ID={}",
                        hourIndex, day.getDayID());
            }

            // 2) Populate forecast-related fields
            populateForecastData(hour, field, hourIndex, day, freshWeatherResponse, freshSolarResponse);

        } catch (Exception e) {
            log.error("Error updating hour data for hourIndex={} in Day ID={}: {}",
                    hourIndex, day.getDayID(), e.getMessage(), e);
        }
    }


    private void updateFutureHourData(Hour hour, Field field, int hourIndex, Day day) {
        try {
            LocalDate currentDate = LocalDate.now();

            WeatherResponse freshWeatherResponse = fieldService.getWeatherDataByFieldId(field.getFieldID());
            SolarResponse freshSolarResponse = fieldService.getSolarDataByFieldId(field.getFieldID(), currentDate);

            populateForecastData(hour, field, hourIndex, day, freshWeatherResponse, freshSolarResponse);

        } catch (Exception e) {
            log.error("Error updating hour data for hourIndex={} in Day ID={}: {}",
                    hourIndex, day.getDayID(), e.getMessage(), e);
        }
    }


    private void populateForecastData(
            Hour hour,
            Field field,
            int hourIndex,
            Day day,
            WeatherResponse weatherResponse,
            SolarResponse solarResponse
    ) {
        try {
            Double forecastTemp = weatherResponse.getHourly().get(hourIndex).getTemp();
            Double forecastRH = weatherResponse.getHourly().get(hourIndex).getHumidity().doubleValue();
            Double windSpeed = weatherResponse.getHourly().get(hourIndex).getWindSpeed();
            double forecastPrecipitation = calculateForecastPrecipitation(weatherResponse, hourIndex);

            double solarRadiation = eToCalculatorService.calculateSolarRadiationHourly(
                    weatherResponse, solarResponse, hourIndex
            );

            double forecastETo = eToCalculatorService.calculateForecastEToHourly(
                    weatherResponse, solarResponse, field, hourIndex
            );

            hour.setForecastEToHourly(forecastETo);
            hour.setForecastTemperature(forecastTemp);
            hour.setForecastHumidity(forecastRH);
            hour.setForecastWindSpeed(windSpeed);
            hour.setForecastPrecipitation(forecastPrecipitation);
            hour.setSolarRadiation(solarRadiation);

        } catch (Exception e) {
            log.error("Error populating forecast data for hourIndex={} in Day ID={}: {}",
                    hourIndex, day.getDayID(), e.getMessage(), e);
        }
    }

    private double calculateForecastPrecipitation(WeatherResponse freshWeatherResponse, int hourIndex) {
        WeatherResponse.Hourly hourlyData = freshWeatherResponse.getHourly().get(hourIndex);
        double pop = hourlyData.getPop();
        WeatherResponse.Rain rain = hourlyData.getRain();
        return (pop >= 0.3 && rain != null && rain.getOneHour() != null) ? rain.getOneHour() : 0.0;
    }

    private void isIrrigationRequiredToday() {
        //get RAW
        //if RAW < dailyWaterDepletition
            //calculate how many hours plants can survive
            //if there is rain in these hours
                //get rain amount
                //if + rain -etc(hours)
            //else no rain



        //get currentRaw
        //calculate each hour guessedRAW (RAW - plantWaterUsage(KcbAdjusted x ETo) + rain)
        //loop todays hours
            //if guessedRAW < 0
                //if its nighttime
                    //do every hour until sunrise
                        //calculate Ks (soilMoisture / TAW - RAW) until sunrise
                            //if Ks >= 0.7
                                //skip hour
                            //else Ks < 0.7
                                //irrigate

                                    //set irrigation water amount =

                //calculate plantWaterUsage + evaporationWaterLoss for remaining hours
                    // Ks = soilMoisture / TAW - RAW
            //if guessedRAW < 0 didn t happened in todays hours
                //return no irrigation


    }
}












