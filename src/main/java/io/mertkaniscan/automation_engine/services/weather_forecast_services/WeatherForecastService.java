package io.mertkaniscan.automation_engine.services.weather_forecast_services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.*;
import java.time.format.DateTimeFormatter;


@Service
@Slf4j
public class WeatherForecastService {
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.onecall.url}")
    private String onecallApiUrl;

    @Autowired
    public WeatherForecastService(ObjectMapper objectMapper,
                                  RestTemplate restTemplate) {

        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    private String getWeatherData(double latitude, double longitude) {
        try {
            String url = String.format("%s?lat=%s&lon=%s&appid=%s&units=metric",
                    onecallApiUrl, latitude, longitude, apiKey);
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.error("Failed to fetch weather data: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch weather data", e);
        }
    }

    private String convertUnixTimestamp(long timestamp) {
        return Instant.ofEpochSecond(timestamp)
                .atZone(ZoneId.of("Europe/Istanbul"))
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    }

    public WeatherResponse getWeatherDataObj(double latitude, double longitude) {
        try {
            String jsonResponse = getWeatherData(latitude, longitude);
            return objectMapper.readValue(jsonResponse, WeatherResponse.class);
        } catch (Exception e) {
            log.error("Failed to get weather data for lat={}, lon={}: {}", latitude, longitude, e.getMessage());
            throw new RuntimeException("Failed to get weather data", e);
        }
    }
}