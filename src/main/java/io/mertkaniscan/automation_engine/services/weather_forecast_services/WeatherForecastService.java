package io.mertkaniscan.automation_engine.services.weather_forecast_services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mertkaniscan.automation_engine.services.weather_forecast_services.weather_response_obj.CurrentWeather;
import io.mertkaniscan.automation_engine.services.weather_forecast_services.weather_response_obj.WeatherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.*;
import java.time.format.DateTimeFormatter;


@Service
public class WeatherForecastService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.onecall.url}")
    private String onecallApiUrl;

    @Value("${weather.api.solar.url}")
    private String solarApiUrl;

    public WeatherForecastService() {
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
    }

    public String getTimezoneOffset(double latitude, double longitude) {
        ZoneId zoneId = ZoneId.of("UTC");
        ZonedDateTime currentTime = ZonedDateTime.now(zoneId);
        ZoneOffset offset = currentTime.getOffset();
        return offset.getId(); // Returns the offset as ±HH:MM
    }

    // Method to fetch weather data from API
    public String getWeatherData(double latitude, double longitude) {
        try {
            String fullUrl = onecallApiUrl + "?lat=" + latitude + "&lon=" + longitude + "&appid=" + apiKey + "&units=metric";

            return restTemplate.getForObject(fullUrl, String.class);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch weather data", e);
        }
    }

    public String getSolarData(double latitude, double longitude, LocalDate date) {
        try {

            String formattedDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

            String fullUrl = solarApiUrl + "?lat=" + latitude + "&lon=" + longitude +
                    "&date=" + formattedDate + "&appid=" + apiKey;


            return restTemplate.getForObject(fullUrl, String.class);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch solar data", e);
        }
    }

    // Method to parse weather data
    public WeatherResponse parseWeatherData(String json) {
        try {
            // Parse the JSON string into the WeatherResponse object
            return objectMapper.readValue(json, WeatherResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to parse weather data", e);
        }
    }

    // Method to convert Unix timestamp to human-readable date
    public String convertUnixTimestamp(long timestamp) {
        ZonedDateTime dateTime = Instant.ofEpochSecond(timestamp)
                .atZone(ZoneId.of("Europe/Istanbul")); // Adjust the time zone as needed

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return dateTime.format(formatter);
    }

    public WeatherResponse getAndParseWeatherData(double latitude, double longitude) {
        try {
            String jsonResponse = getWeatherData(latitude, longitude);
            WeatherResponse weatherResponse = parseWeatherData(jsonResponse);

            // Get CurrentWeather object and convert the Unix timestamps
            CurrentWeather currentWeather = weatherResponse.getCurrent();

            String readableSunrise = convertUnixTimestamp(currentWeather.getSunrise());
            String readableSunset = convertUnixTimestamp(currentWeather.getSunset());

            // Set the human-readable sunrise and sunset in the CurrentWeather object
            currentWeather.setReadableSunrise(readableSunrise);
            currentWeather.setReadableSunset(readableSunset);

            return weatherResponse;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch and parse weather data", e);
        }
    }

    // Method to parse solar data
    public SolarResponse parseSolarData(String json) {
        try {
            return objectMapper.readValue(json, SolarResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to parse solar data", e);
        }
    }

    // Method to fetch and parse solar data
    public SolarResponse getAndParseSolarData(double latitude, double longitude, LocalDate date) {
        try {
            String jsonResponse = getSolarData(latitude, longitude, date);
            System.out.println(jsonResponse);
            return parseSolarData(jsonResponse);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch and parse solar data", e);
        }
    }
}