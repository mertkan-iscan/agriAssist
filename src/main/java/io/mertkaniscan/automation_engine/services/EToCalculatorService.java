package io.mertkaniscan.automation_engine.services;

import io.mertkaniscan.automation_engine.models.SensorData;
import io.mertkaniscan.automation_engine.services.weather_forecast_services.weather_response_obj.WeatherResponse;
import io.mertkaniscan.automation_engine.services.device_services.SensorDataSocketService;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.services.weather_forecast_services.WeatherForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EToCalculatorService {

    @Autowired
    private SensorDataSocketService sensorDataSocketService;

    @Autowired
    private WeatherForecastService weatherForecastService;

    @Autowired
    private FieldService fieldService;

    public double calculateEto(int fieldId) {

        // Hava durumu ve sensör verilerini al
        WeatherResponse weatherResponse = fieldService.getWeatherDataByFieldId(fieldId);

        SensorData sensorData = null;


        // ET₀ hesaplama mantığını burada uygula
        double eto = calculateEto(weatherResponse, sensorData);
        return eto;
    }

    private double calculateEto(WeatherResponse weatherResponse, SensorData sensorData) {

        return 0;
    }
}
