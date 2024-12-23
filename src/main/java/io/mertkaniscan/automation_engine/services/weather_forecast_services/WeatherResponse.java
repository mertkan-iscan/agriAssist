package io.mertkaniscan.automation_engine.services.weather_forecast_services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class WeatherResponse {
    private Long id;
    private Double lat;
    private Double lon;
    private String timezone;

    @JsonProperty("timezone_offset")
    private Integer timezoneOffset;

    private String day;
    private Current current;
    private List<Minutely> minutely;
    private List<Hourly> hourly;
    private List<Daily> daily;

    @Data
    @NoArgsConstructor
    public static class Current {
        private Long id;
        private Long dt;
        private Long sunrise;
        private Long sunset;

        @JsonProperty("readable_sunrise")
        private String readableSunrise;

        @JsonProperty("readable_sunset")
        private String readableSunset;

        private Double temp;

        @JsonProperty("feels_like")
        private Double feelsLike;

        private Integer pressure;
        private Integer humidity;

        @JsonProperty("dew_point")
        private Double dewPoint;

        private Integer uvi;
        private Integer clouds;
        private Integer visibility;

        @JsonProperty("wind_speed")
        private Double windSpeed;

        @JsonProperty("wind_deg")
        private Integer windDeg;

        @JsonProperty("wind_gust")
        private Double windGust;

        private List<Weather> weather;
        private Rain rain;
    }

    @Data
    @NoArgsConstructor
    public static class Minutely {
        private Long id;
        private Long dt;

        @JsonProperty("precipitation")
        private Integer precipitation;
    }

    @Data
    @NoArgsConstructor
    public static class Hourly {
        private Long id;
        private Long dt;
        private Double temp;

        @JsonProperty("feels_like")
        private Double feelsLike;

        private Integer pressure;
        private Integer humidity;

        @JsonProperty("dew_point")
        private Double dewPoint;

        private Integer uvi;
        private Integer clouds;
        private Integer visibility;

        @JsonProperty("wind_speed")
        private Double windSpeed;

        @JsonProperty("wind_deg")
        private Integer windDeg;

        @JsonProperty("wind_gust")
        private Double windGust;

        private List<Weather> weather;
        private Double pop;
        private Rain rain;
    }

    @Data
    @NoArgsConstructor
    public static class Daily {
        private Long id;
        private Long dt;
        private Long sunrise;
        private Long sunset;
        private Long moonrise;
        private Long moonset;

        @JsonProperty("moon_phase")
        private Double moonPhase;

        private Temperature temp;

        @JsonProperty("feels_like")
        private FeelsLike feelsLike;

        private Integer pressure;
        private Integer humidity;

        @JsonProperty("dew_point")
        private Double dewPoint;

        @JsonProperty("wind_speed")
        private Double windSpeed;

        @JsonProperty("wind_deg")
        private Integer windDeg;

        @JsonProperty("wind_gust")
        private Double windGust;

        private List<Weather> weather;
        private Integer clouds;
        private Double pop;
        private Double uvi;
        private Double rain;
        private String summary;
    }

    @Data
    @NoArgsConstructor
    public static class Weather {
        private Long id;

        @JsonProperty("weather_id")
        private Integer weatherId;

        private String main;
        private String description;
        private String icon;
    }

    @Data
    @NoArgsConstructor
    public static class Rain {
        private Long id;

        @JsonProperty("1h")
        private Double oneHour;
    }

    @Data
    @NoArgsConstructor
    public static class Temperature {
        private Long id;
        private Double day;
        private Double min;
        private Double max;
        private Double night;
        private Double eve;
        private Double morn;
    }

    @Data
    @NoArgsConstructor
    public static class FeelsLike {
        private Long id;
        private Double day;
        private Double night;
        private Double eve;
        private Double morn;
    }

    // Convert JSON string to WeatherResponse object
    public static WeatherResponse fromJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, WeatherResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing weather response", e);
        }
    }

    // Convert WeatherResponse object to JSON string
    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting weather response to JSON", e);
        }
    }
}
