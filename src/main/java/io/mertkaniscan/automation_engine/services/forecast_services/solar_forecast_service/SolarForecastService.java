package io.mertkaniscan.automation_engine.services.forecast_services.solar_forecast_service;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class SolarForecastService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.solar.url}")
    private String solarApiUrl;

    @Autowired
    public SolarForecastService(ObjectMapper objectMapper, RestTemplate restTemplate) {

        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public String fetchSolarData(double latitude, double longitude, LocalDate date) {
        try {
            String formattedDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String url = String.format("%s?lat=%s&lon=%s&date=%s&appid=%s",
                    solarApiUrl, latitude, longitude, formattedDate, apiKey);
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.error("Failed to fetch solar data: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch solar data", e);
        }
    }

    public SolarResponse parseSolarData(String json) {
        try {
            return objectMapper.readValue(json, SolarResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse solar data: {}", e.getMessage());
            throw new RuntimeException("Failed to parse solar data", e);
        }
    }

    public SolarResponse getSolarData(double latitude, double longitude, LocalDate date) {
        try {
            String jsonResponse = fetchSolarData(latitude, longitude, date);
            return parseSolarData(jsonResponse);
        } catch (Exception e) {
            log.error("Failed to get solar data for lat={}, lon={}, date={}: {}", latitude, longitude, date, e.getMessage());
            throw new RuntimeException("Failed to get solar data", e);
        }
    }

}
