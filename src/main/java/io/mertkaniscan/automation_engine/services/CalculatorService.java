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

        double KcbAdjusted = field.getPlantInField().getCurrentKcValue();

        Double humidity = field.getCurrentValues().getSensorHumidity();

        if(humidity == null && field.getFieldType().equals(Field.FieldType.GREENHOUSE)){
            humidity = field.getCurrentValues().getForecastHumidity();
        }else{
            return null;
        }

        Double windSpeed = field.getCurrentValues().getSensorWindSpeed();
        if(humidity == null){
            humidity = field.getCurrentValues().getForecastWindSpeed();
        }

        double KcMax = Calculators.calculateKcMax(KcbAdjusted, humidity, windSpeed);
        double Kr = calculateSensorKr(field);
        double fw = calculateFw(field);

        return Calculators.calculateKe(Kr, fw, KcMax, KcbAdjusted);
    }

    private double calculateFw(Field field) {

        double wettedArea = field.getCurrentValues().getWetArea();

        return Calculators.calculateFw(wettedArea, field.getTotalArea());
    }

    public double calculateSensorKr(Field field) {

        double evaporationCoeff = field.getEvaporationCoeff();
        double fieldCurrentDeValue = field.getCurrentValues().getDeValue();

        // Calculate TEW and REW
        Double sensorTEWValue = field.getCurrentValues().getTewValue();

        double sensorREWValue = sensorTEWValue * evaporationCoeff;

        // Logic for Kr calculation
        if (fieldCurrentDeValue >= sensorTEWValue) {
            return 0.0; // All water depleted
        }
        if (fieldCurrentDeValue <= sensorREWValue) {
            return 1.0; // No reduction in evaporation
        }

        // Linear interpolation for Kr
        return (sensorTEWValue - fieldCurrentDeValue) / (sensorTEWValue - sensorREWValue);
    }

    public double calculateSensorTAW(Field field, int minutesBack) {

        double currentRootDepth = field.getPlantInField().getCurrentRootZoneDepth();
        double fieldWiltingPoint = field.getWiltingPoint();

        Timestamp since = new Timestamp(System.currentTimeMillis() - minutesBack * 60 * 1000L);

        double[][] calibratedMoisture = getDepthValues(field, since);

        double soilWaterPercentage = soilMoistureCalculatorService.calculateSphericalMoisture(currentRootDepth, calibratedMoisture);

        return Calculators.calculateSensorTAW(soilWaterPercentage, fieldWiltingPoint, currentRootDepth);
    }

    public Double calculateSensorRAW(Field field) {

        double allowableDepletion = field.getPlantInField().getAllowableDepletion();
        double TAW = field.getCurrentValues().getTawValue();

        return Calculators.calculateSensorRAW(TAW, allowableDepletion);
    }

    public double calculateSensorTEW(Field field, int minutesBack) {

        double maxEvoporationDepth = field.getPlantInField().getCurrentRootZoneDepth();
        double fieldWiltingPoint = field.getWiltingPoint();

        Timestamp since = new Timestamp(System.currentTimeMillis() - minutesBack * 60 * 1000L);

        double[][] calibratedMoisture = getDepthValues(field, since);

        double soilWaterPercentage = soilMoistureCalculatorService.calculateSphericalMoisture(maxEvoporationDepth, calibratedMoisture);

        return Calculators.calculateSensorTEW(soilWaterPercentage, fieldWiltingPoint, maxEvoporationDepth);
    }

    public Double calculateSensorREW(Field field) {

        double evaporationCoeff = field.getEvaporationCoeff();
        double TEW = field.getCurrentValues().getTewValue();

        return Calculators.calculateSensorREW(TEW, evaporationCoeff);
    }

    public double[][] getDepthValues(Field field, Timestamp since){

        double[][] calibratedMoisture = new double[2][3];

        // Fetch sensor readings for depth 1 (row 0)
        calibratedMoisture[0][0] = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(
                field.getFieldID(), "depth_1_1", since);

        calibratedMoisture[0][1] = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(
                field.getFieldID(), "depth_1_2", since);

        calibratedMoisture[0][2] = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(
                field.getFieldID(), "depth_1_3", since);

        // Fetch sensor readings for depth 2 (row 1)
        calibratedMoisture[1][0] = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(
                field.getFieldID(), "depth_2_1", since);

        calibratedMoisture[1][1] = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(
                field.getFieldID(), "depth_2_2", since);

        calibratedMoisture[1][2] = sensorDataService.getMeanSensorDataByFieldIdTypeAndTimestamp(
                field.getFieldID(), "depth_2_3", since);

        return calibratedMoisture;
    }
}
