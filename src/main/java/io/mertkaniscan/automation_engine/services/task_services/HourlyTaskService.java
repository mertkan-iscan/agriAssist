package io.mertkaniscan.automation_engine.services.task_services;

import io.mertkaniscan.automation_engine.models.*;
import io.mertkaniscan.automation_engine.repositories.DayRepository;
import io.mertkaniscan.automation_engine.repositories.FieldRepository;
import io.mertkaniscan.automation_engine.services.CalculatorService;
import io.mertkaniscan.automation_engine.services.EToCalculatorService;
import io.mertkaniscan.automation_engine.services.forecast_services.solar_forecast_service.SolarResponse;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherForecastService;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherResponse;

import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.services.main_services.SensorDataService;
import io.mertkaniscan.automation_engine.utils.calculators.Calculators;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
    private final EToCalculatorService eToCalculatorService;
    private final SensorDataService sensorDataService;
    private final Calculators calculators;
    private final FieldService fieldService;

    public HourlyTaskService(FieldRepository fieldRepository,
                             DayRepository dayRepository,
                             WeatherForecastService weatherForecastService,
                             CalculatorService calculatorService, EToCalculatorService eToCalculatorService, SensorDataService sensorDataService, Calculators calculators, FieldService fieldService) {

        this.fieldRepository = fieldRepository;
        this.dayRepository = dayRepository;
        this.weatherForecastService = weatherForecastService;
        this.calculatorService = calculatorService;
        this.eToCalculatorService = eToCalculatorService;
        this.sensorDataService = sensorDataService;
        this.calculators = calculators;
        this.fieldService = fieldService;
    }

    public void setHourlyRecords() {
        log.info("Starting hourly record update task.");
        fieldRepository.findAll().forEach(this::updateFieldHourlyRecords);
        log.info("Hourly record update task completed.");
    }

    @Transactional
    public void updateFieldHourlyRecords(Field field) {
        try {

            if (field.getPlantInField() == null) {
                log.warn("No plant associated with field '{}'. Skipping hourly record update.", field.getFieldName());
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            int hourIndex = now.getHour();
            int previousHourIndex = hourIndex == 0 ? 23 : hourIndex - 1;

            Day today = dayRepository.findByPlant_PlantIDAndDateWithHours(
                    field.getPlantInField().getPlantID(),
                    Timestamp.valueOf(LocalDate.now().atStartOfDay())
            );

            if (today == null) {
                log.warn("No Day record found for field '{}'. Skipping.", field.getFieldName());
                return;
            }

            Hour currentHour = today.getHours().stream()
                    .filter(hour -> hour.getHourIndex() == hourIndex)
                    .findFirst()
                    .orElse(null);

            Hour previousHour = today.getHours().stream()
                    .filter(hour -> hour.getHourIndex() == previousHourIndex)
                    .findFirst()
                    .orElse(null);

            if (currentHour != null) {
                setHourWeatherValues(currentHour, previousHour, field);
                setHourWaterValues(currentHour, previousHour, field);
            }

            if (previousHour != null) {
                setPreviousHourValues(previousHour, field);
            }

            if (previousHour != null && currentHour != null) {
                calculatePlantKcb(currentHour, previousHour, field);
            }

        } catch (Exception e) {
            log.error("Error updating hourly records for field '{}'. Error: {}", field.getFieldName(), e.getMessage(), e);
        }
    }

    private void setPreviousHourValues(@NotNull Hour previousHour, Field field) {

        try {
            WeatherResponse freshWeatherResponse = fieldService.getWeatherDataByFieldId(field.getFieldID());

            Double oneHourRain = null;
            if (freshWeatherResponse != null) {
                if (freshWeatherResponse.getCurrent() != null) {
                    if (freshWeatherResponse.getCurrent().getRain() != null) {
                        oneHourRain = freshWeatherResponse.getCurrent().getRain().getOneHour();
                        log.info("Rain data fetched successfully for field ID {}: {} mm", field.getFieldID(), oneHourRain);
                    } else {
                        log.info("Rain data is not available for field ID {}.", field.getFieldID());
                    }
                } else {
                    log.info("Current weather data is not available for field ID {}.", field.getFieldID());
                }
            } else {
                log.info("Weather response is null for field ID {}.", field.getFieldID());
            }

            if(oneHourRain != null){

                previousHour.setHappenedPrecipitation(oneHourRain);
                previousHour.setRainWetArea(1.0);
            }else{

                previousHour.setHappenedPrecipitation(0.0);

                previousHour.setRainWetArea(0.0);
            }

            Double previousHourRainWetArea = previousHour.getRainWetArea();

            Double previousHourIrrigationWetArea = previousHour.getIrrigationWetArea();
            if(previousHourIrrigationWetArea == null) {
                previousHourIrrigationWetArea = 0.0;
            }

            Double totalWetArea = (previousHourRainWetArea + previousHourIrrigationWetArea) / 2;

            double fieldWiltingPoint = field.getWiltingPoint();
            double fieldCapacity = field.getFieldCapacity();

            Double TEW = previousHour.getTEWValueHourly();
            double soilMoisture = (TEW * 100) / (field.getMaxEvaporationDepth() * 1000);
            Double sensorREWValue = previousHour.getREWValueHourly();

            double Kr = Calculators.calculateSensorKr(sensorREWValue, soilMoisture, fieldCapacity, fieldWiltingPoint);

            Plant plant = field.getPlantInField();
            double Kcb = plant.getCurrentKcValue();
            double Kcmax = Calculators.calculateKcMax(Kcb, previousHour.getSensorHumidity(),previousHour.getWindSpeed(field.getFieldType()));

            Double Ke = Calculators.calculateKe(Kr, totalWetArea, Kcmax, Kcb);

            previousHour.setKrValue(Kr);
            previousHour.setKeValue(Ke);
            previousHour.setKcMaxValue(Kcmax);

            previousHour.setLastUpdated(LocalDateTime.now());

            dayRepository.save(previousHour.getDay());

            log.info("Updated values for the previous hour (index={}) in field '{}'.", previousHour.getHourIndex(), field.getFieldName());
        } catch (Exception e) {
            log.error("Failed to update previous hour values for hour={} in field '{}'. Error: {}", previousHour.getHourIndex(), field.getFieldName(), e.getMessage(), e);
        }
    }

    private void setHourWeatherValues(Hour currentHour, Hour previousHour, Field field) {
        try {
            if (field.getFieldType() == Field.FieldType.GREENHOUSE) {

            } else if (field.getFieldType() == Field.FieldType.OUTDOOR) {

            }


            int hourIndex = currentHour.getHourIndex();

            WeatherResponse weatherResponse = weatherForecastService.getWeatherDataObj(
                    field.getLatitude(),
                    field.getLongitude()
            );

            Timestamp currentHourTimestamp = currentHour.getTimestamp();
            Timestamp previousHourTimestamp = previousHour.getTimestamp();
            System.out.println("currentHourTimestamp: " + currentHourTimestamp);

            Double meanTemperature = sensorDataService.getMeanValueBetweenTimestamps(field.getFieldID(), "weather_temp", previousHourTimestamp, currentHourTimestamp);
            Double meanHumidity = sensorDataService.getMeanValueBetweenTimestamps(field.getFieldID(), "weather_hum", previousHourTimestamp, currentHourTimestamp);
            log.info("mean temperature: {}", meanTemperature);
            log.info("mean humidity: {}", meanHumidity);

            SolarResponse solarResponse = currentHour.getDay().getSolarResponse();

            if (solarResponse == null) {
                throw new RuntimeException("Solar data is missing in the Day record for field: " + field.getFieldName());
            }

            double sensorEToHourly = eToCalculatorService.calculateSensorEToHourly(
                    meanTemperature,
                    meanHumidity,
                    weatherResponse,
                    solarResponse,
                    field,
                    hourIndex);

            double currentEToHourly = eToCalculatorService.calculateCurrentEToHourly(
                    weatherResponse,
                    solarResponse,
                    field,
                    hourIndex);

            currentHour.setForecastEToHourly(currentEToHourly);
            currentHour.setSensorEToHourly(sensorEToHourly);

            currentHour.setForecastTemperature(weatherResponse.getHourly().get(hourIndex).getTemp());
            currentHour.setForecastHumidity(weatherResponse.getHourly().get(hourIndex).getHumidity().doubleValue());

            currentHour.setLastUpdated(LocalDateTime.now());

            currentHour.setSensorTemperature(meanTemperature);
            currentHour.setSensorHumidity(meanHumidity);

            currentHour.setSolarRadiation(
                    eToCalculatorService.calculateSolarRadiationHourly(weatherResponse, solarResponse, hourIndex));

            WeatherResponse.Rain rain = weatherResponse.getCurrent().getRain();
            if (rain != null && rain.getOneHour() != null) {
                //currentHour.setHappenedPrecipitation(rain.getOneHour());
            } else {
                //currentHour.setHappenedPrecipitation(0.0);
            }

            dayRepository.save(currentHour.getDay());

            log.info("Updated hourly record for hour={} in field '{}'.", hourIndex, field.getFieldName());

        } catch (Exception e) {
            log.error("Failed to update Hour record for field '{}'. Error: {}", field.getFieldName(), e.getMessage(), e);
        }
    }

    @Transactional
    public void setHourWaterValues(Hour currentHour, Hour previousHour,  Field field) {
        try {

            if (field.getPlantInField() == null) {
                log.warn("No plant associated with field '{}'. Skipping.", field.getFieldName());
                return;
            }

            Timestamp currentHourTimestamp = currentHour.getTimestamp();
            Timestamp previousHourTimestamp = previousHour.getTimestamp();

            Double TAW = calculatorService.calculateSensorTAW(field, previousHourTimestamp, currentHourTimestamp);
            Double TEW = calculatorService.calculateSensorTEW(field, previousHourTimestamp, currentHourTimestamp);

            Double RAW = calculatorService.calculateSensorRAW(TAW, field);
            Double REW = calculatorService.calculateSensorREW(TEW, field);

            //Double Kr = calculatorService.calculateSensorKr(field, TEW, REW);

            //Double Ke = calculatorService.calculateKe(currentHour, field);

            Day today = dayRepository.findByPlant_PlantIDAndDateWithHours(
                    field.getPlantInField().getPlantID(),
                    Timestamp.valueOf(LocalDate.now().atStartOfDay())
            );

            if (today == null) {
                log.warn("No Day record found for field '{}'. Skipping.", field.getFieldName());
                return;
            }

            currentHour.setTAWValueHourly(TAW);
            currentHour.setTEWValueHourly(TEW);

            currentHour.setRAWValueHourly(RAW);
            currentHour.setREWValueHourly(REW);

            //currentHour.setKrValue(Kr);

            dayRepository.save(currentHour.getDay());

        } catch (Exception e) {
            log.error("Error setting hourly record values for field '{}'. Error: {}", field.getFieldName(), e.getMessage(), e);
        }
    }

    @Transactional
    public void calculatePlantKcb(Hour currentHour, Hour previousHour, Field field) {
        try {
            if (field == null) {
                log.warn("Field is null. Skipping calculation.");
                return;
            }

            if (field.getPlantInField() == null) {
                log.warn("No plant associated with field '{}'. Skipping.", field.getFieldName());
                return;
            }

            Day today = dayRepository.findByPlant_PlantIDAndDateWithHours(
                    field.getPlantInField().getPlantID(),
                    Timestamp.valueOf(LocalDate.now().atStartOfDay())
            );

            if (today == null) {
                log.warn("No Day record found for field '{}'. Skipping.", field.getFieldName());
                return;
            }

            if (previousHour == null) {
                log.warn("Previous hour is null for field '{}'. Skipping.", field.getFieldName());
                return;
            }

            Double previousHourRAWValueHourly = previousHour.getRAWValueHourly();
            Double previousHourHappenedPrecipitation = previousHour.getHappenedPrecipitation();
            Double previousHourIrrigationAmount = previousHour.getIrrigationAmount() == null ? 0.0 : previousHour.getIrrigationAmount();
            Double previousHourSensorEToHourly = previousHour.getSensorEToHourly();
            Double previousHourKeValue = previousHour.getKeValue();
            Double previousHourKcbAdjustedValue = previousHour.getKcbAdjustedValue();
            Double currentHourRAWValueHourly = currentHour.getRAWValueHourly();


            log.debug("Calculating KcbAdjusted for field '{}':", field.getFieldName());
            log.debug("previousHourRAWValueHourly: {}", previousHourRAWValueHourly);
            log.debug("previousHourHappenedPrecipitation: {}", previousHourHappenedPrecipitation);
            log.debug("previousHourIrrigationAmount: {}", previousHourIrrigationAmount);
            log.debug("previousHourSensorEToHourly: {}", previousHourSensorEToHourly);
            log.debug("previousHourKeValue: {}", previousHourKeValue);
            log.debug("previousHourKcbAdjustedValue: {}", previousHourKcbAdjustedValue);
            log.debug("currentHourRAWValueHourly: {}", currentHourRAWValueHourly);


            if (previousHourRAWValueHourly == null ||
                    previousHourHappenedPrecipitation == null ||
                    previousHourIrrigationAmount == null ||
                    previousHourSensorEToHourly == null ||
                    previousHourKeValue == null ||
                    previousHourKcbAdjustedValue == null) {
                log.warn("One or more values from previous hour are null for field '{}'. Skipping.", field.getFieldName());
                return;
            }

            if (currentHour == null) {
                log.warn("Current hour is null for field '{}'. Skipping.", field.getFieldName());
                return;
            }

            double currentHourKcbAdjusted =
                    (previousHourRAWValueHourly
                            + previousHourHappenedPrecipitation
                            + previousHourIrrigationAmount
                            - currentHourRAWValueHourly
                            - (previousHourKeValue * previousHourSensorEToHourly))
                            / previousHourSensorEToHourly;

            log.debug("Calculated currentHourKcbAdjusted: {}", currentHourKcbAdjusted);

            currentHour.setKcbAdjustedValue(currentHourKcbAdjusted);

            dayRepository.save(currentHour.getDay());

        } catch (Exception e) {
            log.error("Error setting hourly record values for field '{}'. Error: {}", field.getFieldName(), e.getMessage(), e);
        }
    }

}