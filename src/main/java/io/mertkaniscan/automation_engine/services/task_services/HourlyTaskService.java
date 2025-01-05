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

    public HourlyTaskService(FieldRepository fieldRepository,
                             DayRepository dayRepository,
                             WeatherForecastService weatherForecastService,
                             CalculatorService calculatorService, EToCalculatorService eToCalculatorService, SensorDataService sensorDataService, Calculators calculators) {

        this.fieldRepository = fieldRepository;
        this.dayRepository = dayRepository;
        this.weatherForecastService = weatherForecastService;
        this.calculatorService = calculatorService;
        this.eToCalculatorService = eToCalculatorService;
        this.sensorDataService = sensorDataService;
        this.calculators = calculators;
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
                setHourWeatherValues(currentHour, field);
                setHourWaterValues(currentHour, field);
            }

            if (previousHour != null) {
                setPreviousHourValues(previousHour, field);
            }

            if (previousHour != null && currentHour != null) {
                setHourlyDifferenceValues(previousHour, currentHour, field);
            }

        } catch (Exception e) {
            log.error("Error updating hourly records for field '{}'. Error: {}", field.getFieldName(), e.getMessage(), e);
        }
    }

    private void setHourlyDifferenceValues(Hour previousHour, Hour currentHour, Field field) {

        Double previousRAW = previousHour.getRAWValueHourly();
        Double currentRAW = currentHour.getRAWValueHourly();
        Double happenedPrecipitation = previousHour.getHappenedPrecipitation();
        Double irrigationAmount = previousHour.getIrrigationAmount();
        Double sensorEToHourly = previousHour.getSensorEToHourly();
        Double KeValue = previousHour.getKeValue();

        if (previousRAW == null || currentRAW == null || happenedPrecipitation == null ||
                irrigationAmount == null || sensorEToHourly == null || KeValue == null) {
            log.warn("Missing values for calculation: " +
                            "previousRAW={}, currentRAW={}, happenedPrecipitation={}, irrigationAmount={}, sensorEToHourly={}, KeValue={}",
                    previousRAW, currentRAW, happenedPrecipitation, irrigationAmount, sensorEToHourly, KeValue);
            return;
        }

        double calculatedHourlyKcb =
                (previousRAW - currentRAW
                        + happenedPrecipitation
                        + irrigationAmount
                        - (KeValue * sensorEToHourly))
                        / sensorEToHourly;

        log.info("CalculatedHourlyKcb: {}", calculatedHourlyKcb);
    }

    private void setPreviousHourValues(@NotNull Hour previousHour, Field field) {

        try {

            Double rainWetArea = previousHour.getRainWetArea();
            if(rainWetArea == null) {
                rainWetArea = 0.0;
            }

            Double irrigationWetArea = previousHour.getIrrigationWetArea();
            if(irrigationWetArea == null) {
                irrigationWetArea = 0.0;
            }

            Double totalWetArea = rainWetArea + irrigationWetArea;

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

            previousHour.setLastUpdated(LocalDateTime.now());

            dayRepository.save(previousHour.getDay());

            log.info("Updated values for the previous hour (index={}) in field '{}'.", previousHour.getHourIndex(), field.getFieldName());
        } catch (Exception e) {
            log.error("Failed to update previous hour values for hour={} in field '{}'. Error: {}", previousHour.getHourIndex(), field.getFieldName(), e.getMessage(), e);
        }
    }

    private void setHourWeatherValues(Hour hour, Field field) {
        try {

            int hourIndex = hour.getHourIndex();

            // pull fresh weather data
            WeatherResponse weatherResponse = weatherForecastService.getWeatherDataObj(
                    field.getLatitude(),
                    field.getLongitude()
            );

            Timestamp currentHourTimestamp = hour.getTimestamp();
            System.out.println("currentHourTimestamp: " + currentHourTimestamp);

            // Get mean sensor values using the new method
            Double meanTemperature = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(field.getFieldID(), "weather_temp", currentHourTimestamp);
            Double meanHumidity = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(field.getFieldID(), "weather_hum", currentHourTimestamp);
            log.info("mean temperature: {}", meanTemperature);
            log.info("mean humidity: {}", meanHumidity);

            // get previous solar data
            SolarResponse solarResponse = hour.getDay().getSolarResponse();

            if (solarResponse == null) {
                throw new RuntimeException("Solar data is missing in the Day record for field: " + field.getFieldName());
            }

            // Calculate the ETo for the current hour
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

            hour.setForecastEToHourly(currentEToHourly);
            hour.setSensorEToHourly(sensorEToHourly);


            hour.setForecastTemperature(weatherResponse.getHourly().get(hourIndex).getTemp());
            hour.setForecastHumidity(weatherResponse.getHourly().get(hourIndex).getHumidity().doubleValue());

            hour.setLastUpdated(LocalDateTime.now());

            hour.setSensorTemperature(meanTemperature);
            hour.setSensorHumidity(meanHumidity);

            hour.setSolarRadiation(
                    eToCalculatorService.calculateSolarRadiationHourly(weatherResponse, solarResponse, hourIndex));

            WeatherResponse.Rain rain = weatherResponse.getCurrent().getRain();
            if (rain != null && rain.getOneHour() != null) {
                hour.setHappenedPrecipitation(rain.getOneHour());
            } else {
                hour.setHappenedPrecipitation(0.0);
            }

            dayRepository.save(hour.getDay());

            log.info("Updated hourly record for hour={} in field '{}'.", hourIndex, field.getFieldName());

        } catch (Exception e) {
            log.error("Failed to update Hour record for field '{}'. Error: {}", field.getFieldName(), e.getMessage(), e);
        }
    }

    @Transactional
    public void setHourWaterValues(Hour currentHour, Field field) {
        try {

            if (field.getPlantInField() == null) {
                log.warn("No plant associated with field '{}'. Skipping.", field.getFieldName());
                return;
            }

            LocalDate currentDate = LocalDate.now();
            Timestamp startOfHour = Timestamp.valueOf(currentDate.atTime(currentHour.getHourIndex(), 0));
            Timestamp endOfHour = Timestamp.valueOf(currentDate.atTime(currentHour.getHourIndex(), 59, 59));

            Double TAW = calculatorService.calculateSensorTAW(field, startOfHour, endOfHour);
            Double TEW = calculatorService.calculateSensorTEW(field, startOfHour, endOfHour);

            Double RAW = calculatorService.calculateSensorRAW(TAW, field);
            Double REW = calculatorService.calculateSensorREW(TEW, field);

            Double Kr = calculatorService.calculateSensorKr(field, TEW, REW);

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

            currentHour.setKrValue(Kr);

            dayRepository.save(currentHour.getDay());

        } catch (Exception e) {
            log.error("Error setting hourly record values for field '{}'. Error: {}", field.getFieldName(), e.getMessage(), e);
        }
    }
}
