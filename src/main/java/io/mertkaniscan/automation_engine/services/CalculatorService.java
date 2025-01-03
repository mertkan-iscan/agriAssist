package io.mertkaniscan.automation_engine.services;


import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.models.Hour;
import io.mertkaniscan.automation_engine.models.SensorData;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.services.main_services.SensorDataService;
import io.mertkaniscan.automation_engine.services.forecast_services.solar_forecast_service.SolarResponse;
import io.mertkaniscan.automation_engine.services.forecast_services.weather_forecast_service.WeatherResponse;
import io.mertkaniscan.automation_engine.utils.calculators.Calculators;

import io.mertkaniscan.automation_engine.utils.calculators.DailyEToCalculator;
import io.mertkaniscan.automation_engine.utils.calculators.HourlyEToCalculator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CalculatorService {

    private final FieldService fieldService;
    private final SensorDataService sensorDataService;
    private final Calculators calculators;
    private final SoilMoistureCalculatorService soilMoistureCalculatorService;

    public CalculatorService(FieldService fieldService, SensorDataService sensorDataService, Calculators calculators, SoilMoistureCalculatorService soilMoistureCalculatorService) {
        this.fieldService = fieldService;
        this.sensorDataService = sensorDataService;
        this.calculators = calculators;
        this.soilMoistureCalculatorService = soilMoistureCalculatorService;
    }

    public Double calculateKe(Field field) {
        log.info("Calculating Ke for field: {}", field.getFieldID());

        double KcbAdjusted = field.getPlantInField().getCurrentKcValue();
        Double humidity = field.getCurrentValues().getSensorHumidity();

        if (humidity == null && field.getFieldType().equals(Field.FieldType.GREENHOUSE)) {
            humidity = field.getCurrentValues().getForecastHumidity();
        } else {
            log.warn("Humidity is null for field: {}", field.getFieldID());
            return null;
        }

        Double windSpeed = field.getCurrentValues().getSensorWindSpeed();
        if (humidity == null) {
            humidity = field.getCurrentValues().getForecastWindSpeed();
        }

        double KcMax = Calculators.calculateKcMax(KcbAdjusted, humidity, windSpeed);
        double Kr = calculateSensorKr(field);
        double fw = calculateFw(field);

        Double result = Calculators.calculateKe(Kr, fw, KcMax, KcbAdjusted);
        log.info("Ke calculated: {}", result);
        return result;
    }

    private double calculateFw(Field field) {
        log.info("Calculating Fw for field: {}", field.getFieldID());

        double wettedArea = field.getCurrentValues().getWetArea();
        double result = Calculators.calculateFw(wettedArea, field.getTotalArea());

        log.info("Fw calculated: {}", result);
        return result;
    }

    public double calculateSensorKr(Field field) {
        log.info("Calculating Kr for field: {}", field.getFieldID());

        double evaporationCoeff = field.getEvaporationCoeff();
        double fieldCurrentDeValue = field.getCurrentValues().getDeValue();
        Double sensorTEWValue = field.getCurrentValues().getTewValue();
        double sensorREWValue = sensorTEWValue * evaporationCoeff;

        double result;
        if (fieldCurrentDeValue >= sensorTEWValue) {
            result = 0.0;
        } else if (fieldCurrentDeValue <= sensorREWValue) {
            result = 1.0;
        } else {
            result = (sensorTEWValue - fieldCurrentDeValue) / (sensorTEWValue - sensorREWValue);
        }

        log.info("Kr calculated: {}", result);
        return result;
    }

    public double calculateSensorTAW(Field field, int minutesBack) {
        log.info("Calculating TAW for field: {}, minutesBack: {}", field.getFieldID(), minutesBack);

        double currentRootDepth = field.getPlantInField().getCurrentRootZoneDepth();
        double fieldWiltingPoint = field.getWiltingPoint();
        Timestamp since = new Timestamp(System.currentTimeMillis() - minutesBack * 60 * 1000L);

        double[][] calibratedMoisture = getDepthValues(field, since);
        double soilWaterPercentage = soilMoistureCalculatorService.calculateSphericalMoisture(currentRootDepth, calibratedMoisture);
        log.info("currentRootDepth: {}, calibratedMoisture: {}, soilWaterPercentage: {}", currentRootDepth, Arrays.toString(calibratedMoisture), soilWaterPercentage);

        double result = Calculators.calculateSensorTAW(soilWaterPercentage, fieldWiltingPoint, currentRootDepth);
        log.info("TAW calculated: {}", result);
        return result;
    }

    public Double calculateSensorRAW(Field field) {
        log.info("Calculating RAW for field: {}", field.getFieldID());

        double allowableDepletion = field.getPlantInField().getAllowableDepletion();
        double TAW = field.getCurrentValues().getTawValue();

        Double result = Calculators.calculateSensorRAW(TAW, allowableDepletion);
        log.info("RAW calculated: {}", result);
        return result;
    }

    public double calculateSensorTEW(Field field, int minutesBack) {
        log.info("Calculating TEW for field: {}, minutesBack: {}", field.getFieldID(), minutesBack);

        double maxEvoporationDepth = field.getMaxEvaporationDepth();
        double fieldWiltingPoint = field.getWiltingPoint();
        Timestamp since = new Timestamp(System.currentTimeMillis() - minutesBack * 60 * 1000L);

        double[][] calibratedMoisture = getDepthValues(field, since);
        double soilWaterPercentage = soilMoistureCalculatorService.calculateSphericalMoisture(maxEvoporationDepth, calibratedMoisture);
        log.info("maxEvoporationDepth: {}, calibratedMoisture: {}, soilWaterPercentage: {}", maxEvoporationDepth, Arrays.toString(calibratedMoisture), soilWaterPercentage);

        double result = Calculators.calculateSensorTEW(soilWaterPercentage, fieldWiltingPoint, maxEvoporationDepth);
        log.info("TEW calculated: {}", result);
        return result;
    }

    public Double calculateSensorREW(Field field) {
        log.info("Calculating REW for field: {}", field.getFieldID());

        double evaporationCoeff = field.getEvaporationCoeff();
        double TEW = field.getCurrentValues().getTewValue();

        Double result = Calculators.calculateSensorREW(TEW, evaporationCoeff);
        log.info("REW calculated: {}", result);
        return result;
    }

    public double[][] getDepthValues(Field field, Timestamp since) {
        log.info("Fetching depth values for field: {}, since: {}", field.getFieldID(), since);

        double depth_1_1 = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(field.getFieldID(), "depth_1_1", since);
        double depth_1_2 = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(field.getFieldID(), "depth_1_2", since);
        double depth_1_3 = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(field.getFieldID(), "depth_1_3", since);
        double depth_2_1 = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(field.getFieldID(), "depth_2_1", since);
        double depth_2_2 = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(field.getFieldID(), "depth_2_2", since);
        double depth_2_3 = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(field.getFieldID(), "depth_2_3", since);

        double[][] calibratedMoisture = {
                {0.03, 0.0, depth_1_1},
                {0.03, 0.15, depth_1_2},
                {0.03, 0.3, depth_1_3},
                {0.1, 0.0, depth_2_1},
                {0.1, 0.15, depth_2_2},
                {0.1, 0.3, depth_2_3}
        };

        for (int i = 0; i < calibratedMoisture.length; i++) {
            log.info("Depth Row {}: {}", i, Arrays.toString(calibratedMoisture[i]));
        }

        log.info("Depth values fetched successfully for field: {}", field.getFieldID());
        return calibratedMoisture;
    }
}
