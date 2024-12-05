package io.mertkaniscan.automation_engine.components;

import io.mertkaniscan.automation_engine.models.*;
import io.mertkaniscan.automation_engine.services.device_services.SensorDataDTO;
import io.mertkaniscan.automation_engine.services.logic.CalculatorService;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import io.mertkaniscan.automation_engine.services.weather_forecast_services.SolarResponse;
import io.mertkaniscan.automation_engine.services.weather_forecast_services.WeatherForecastService;
import io.mertkaniscan.automation_engine.services.device_services.SensorDataSocketService;
import io.mertkaniscan.automation_engine.services.weather_forecast_services.weather_response_obj.WeatherResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DailyTask {

    private final CalculatorService calculatorService;
    private final WeatherForecastService weatherForecastService;
    private final SensorDataSocketService sensorDataSocketService;
    private final FieldService fieldService;


    @Autowired
    public DailyTask(CalculatorService calculatorService,
                     WeatherForecastService weatherForecastService,
                     SensorDataSocketService sensorDataSocketService, FieldService fieldService) {
        this.calculatorService = calculatorService;
        this.weatherForecastService = weatherForecastService;
        this.sensorDataSocketService = sensorDataSocketService;
        this.fieldService = fieldService;
    }

    public void executeDailyTask(int fieldID) {

        // fetch the plant associated with the field
        Field field = fieldService.getFieldById(fieldID);
        Plant plant = field.getPlantInField();

        if (plant == null) {
            throw new RuntimeException("No plant found for field ID: " + fieldID);
        }

        // create or fetch the Day object for today
        Day today = getOrCreateDay(plant);

        // fetch sensor data for this field
        List<SensorDataDTO> sensorDataList = fieldService.getSensorDataValueByModel(fieldID, "multi_soil_mois_temp_weather");

        //make api calls
        WeatherResponse weatherResponse = fieldService.getWeatherDataByFieldId(fieldID);
        SolarResponse solarResponse = fieldService.getSolarDataByFieldId(fieldID, currentTime);

        //fill every hour
        for (int hour = 0; hour < 24; hour++) {

            Hour hourObj = new Hour();

            hourObj.setHour(hour);
            hourObj.setDay(today);

            // sensor data
            BigDecimal temperatureSensorData = getSensorDataValue(sensorDataList, "temperature");
            BigDecimal humiditySensorData = getSensorDataValue(sensorDataList, "humidity");

            //weather forecast data
            double temperatureWeatherForecast = weatherResponse.getHourly().get(hour).getTemp();
            double windSpeedWeatherForecast = weatherResponse.getHourly().get(hour).getWind_speed();
            double pressureWeatherForecast = fieldService.getSensorDataValueByModel(fieldID, );

            //solar data
            double ghiWeatherForecast = solarResponse.getIrradiance().getHourly().get(hour).getClearSky().getGhi();
            double dniWeatherForecast = solarResponse.getIrradiance().getHourly().get(hour).getClearSky().getDni();
            double dhiWeatherForecast = solarResponse.getIrradiance().getHourly().get(hour).getClearSky().getDni();

            // Calculate ETo using weather and sensor data
            double eto = calculatorService.calculateETo(
                    temperatureWeatherForecast,
                    humiditySensorData,
                    pressureWeatherForecast,
                    ghiWeatherForecast,
                    dniWeatherForecast,
                    dhiWeatherForecast,
                    windSpeedWeatherForecast
            );
            hourObj.setEto(BigDecimal.valueOf(eto));

            // Calculate Ke using depletion and other factors
            double ke = calculatorService.calculateKe(
                    plant.getCurrentCropCoefficient(),
                    humiditySensorData,
                    windSpeedWeatherForecast,
                    0, // Example De value, replace with actual data
                    0, // Example TEW value, replace with actual data
                    0  // Example REW value, replace with actual data
            );
            hourObj.setKe(BigDecimal.valueOf(ke));

            // Save the hourly object
            today.getHours().add(hourObj);
        }

        // Step 5: Update plant's Kc at the end of the day
        double updatedKc = calculatorService.updateDailyKc(plant.getPlantStage().toString(), plant.getCurrentCropCoefficient().doubleValue());
        plant.setCurrentCropCoefficient(BigDecimal.valueOf(updatedKc));

        // Save updated plant and day objects
        savePlant(plant);
        saveDay(today);
    }

    private Day getOrCreateDay(Plant plant) {
        // Mocked logic to fetch or create a Day object
        // Replace with actual database call
        Day today = new Day();
        today.setDate(Timestamp.valueOf(LocalDateTime.now()));
        today.setPlant(plant);
        return today;
    }

}