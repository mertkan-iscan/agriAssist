package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "weather_response")
@Data
@Getter
@Setter
public class WeatherResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double lat;
    private double lon;
    private String timezone;
    private int timezone_offset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fieldID", nullable = false)
    @JsonBackReference(value = "field-weatherResponses")
    private Field field;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dayID", nullable = false)
    private Day day;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "current_weather_id")
    private CurrentWeather current;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "weather_response_id")
    private List<Minute> minutely = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "weather_response_id")
    private List<Hourly> hourly = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "weather_response_id")
    private List<Daily> daily = new ArrayList<>();

    @Entity
    @Table(name = "current_weather")
    @Getter
    @Setter
    public static class CurrentWeather {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private long dt;
        private long sunrise;
        private long sunset;
        private String readableSunrise;
        private String readableSunset;
        private double temp;
        private double feels_like;
        private int pressure;
        private int humidity;
        private double dew_point;
        private int uvi;
        private int clouds;
        private int visibility;
        private double wind_speed;
        private int wind_deg;
        private double wind_gust;

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinColumn(name = "current_weather_id")
        private List<Weather> weather = new ArrayList<>();

        @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinColumn(name = "rain_id")
        private Rain rain;

        // Add copy constructor
        public CurrentWeather() {}

        public CurrentWeather(CurrentWeather other) {
            this.dt = other.dt;
            this.sunrise = other.sunrise;
            this.sunset = other.sunset;
            this.readableSunrise = other.readableSunrise;
            this.readableSunset = other.readableSunset;
            this.temp = other.temp;
            this.feels_like = other.feels_like;
            this.pressure = other.pressure;
            this.humidity = other.humidity;
            this.dew_point = other.dew_point;
            this.uvi = other.uvi;
            this.clouds = other.clouds;
            this.visibility = other.visibility;
            this.wind_speed = other.wind_speed;
            this.wind_deg = other.wind_deg;
            this.wind_gust = other.wind_gust;
            if (other.weather != null) {
                this.weather = other.weather.stream()
                        .map(Weather::new)
                        .collect(java.util.stream.Collectors.toList());
            }
            if (other.rain != null) {
                this.rain = new Rain(other.rain);
            }
        }
    }

    @Entity
    @Table(name = "daily_weather")
    @Getter
    @Setter
    public static class Daily {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private long dt;
        private long sunrise;
        private long sunset;
        private long moonrise;
        private long moonset;
        private double moon_phase;

        @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinColumn(name = "temp_id")
        private Temp temp;

        @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinColumn(name = "feels_like_id")
        private FeelsLike feels_like;

        private int pressure;
        private int humidity;
        private double dew_point;
        private double wind_speed;
        private int wind_deg;
        private double wind_gust;

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinColumn(name = "daily_id")
        private List<Weather> weather = new ArrayList<>();

        private int clouds;
        private double pop;
        private double uvi;
        private double rain;
        private String summary;

        // Add copy constructor
        public Daily() {}

        public Daily(Daily other) {
            this.dt = other.dt;
            this.sunrise = other.sunrise;
            this.sunset = other.sunset;
            this.moonrise = other.moonrise;
            this.moonset = other.moonset;
            this.moon_phase = other.moon_phase;
            this.temp = other.temp != null ? new Temp(other.temp) : null;
            this.feels_like = other.feels_like != null ? new FeelsLike(other.feels_like) : null;
            this.pressure = other.pressure;
            this.humidity = other.humidity;
            this.dew_point = other.dew_point;
            this.wind_speed = other.wind_speed;
            this.wind_deg = other.wind_deg;
            this.wind_gust = other.wind_gust;
            if (other.weather != null) {
                this.weather = other.weather.stream()
                        .map(Weather::new)
                        .collect(java.util.stream.Collectors.toList());
            }
            this.clouds = other.clouds;
            this.pop = other.pop;
            this.uvi = other.uvi;
            this.rain = other.rain;
            this.summary = other.summary;
        }
    }

    @Entity
    @Table(name = "hourly_weather")
    @Getter
    @Setter
    public static class Hourly {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private long dt;
        private double temp;
        private double feels_like;
        private int pressure;
        private int humidity;
        private double dew_point;
        private int uvi;
        private int clouds;
        private int visibility;
        private double wind_speed;
        private int wind_deg;
        private double wind_gust;

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinColumn(name = "hourly_id")
        private List<Weather> weather = new ArrayList<>();

        private double pop;

        @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinColumn(name = "rain_id")
        private Rain rain;

        // Add copy constructor
        public Hourly() {}

        public Hourly(Hourly other) {
            this.dt = other.dt;
            this.temp = other.temp;
            this.feels_like = other.feels_like;
            this.pressure = other.pressure;
            this.humidity = other.humidity;
            this.dew_point = other.dew_point;
            this.uvi = other.uvi;
            this.clouds = other.clouds;
            this.visibility = other.visibility;
            this.wind_speed = other.wind_speed;
            this.wind_deg = other.wind_deg;
            this.wind_gust = other.wind_gust;
            if (other.weather != null) {
                this.weather = other.weather.stream()
                        .map(Weather::new)
                        .collect(java.util.stream.Collectors.toList());
            }
            this.pop = other.pop;
            if (other.rain != null) {
                this.rain = new Rain(other.rain);
            }
        }
    }

    @Entity
    @Table(name = "minute_weather")
    @Getter
    @Setter
    public static class Minute {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private long dt;
        private double precipitation;

        // Add copy constructor
        public Minute() {}

        public Minute(Minute other) {
            this.dt = other.dt;
            this.precipitation = other.precipitation;
        }
    }

    @Entity
    @Table(name = "rain")
    @Getter
    @Setter
    public static class Rain {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @JsonProperty("1h")
        private double _1h;

        // Add copy constructor
        public Rain() {}

        public Rain(Rain other) {
            this._1h = other._1h;
        }
    }

    @Entity
    @Table(name = "temperature")
    @Getter
    @Setter
    public static class Temp {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private double day;
        private double min;
        private double max;
        private double night;
        private double eve;
        private double morn;

        // Add copy constructor
        public Temp() {}

        public Temp(Temp other) {
            this.day = other.day;
            this.min = other.min;
            this.max = other.max;
            this.night = other.night;
            this.eve = other.eve;
            this.morn = other.morn;
        }
    }

    @Entity
    @Table(name = "weather")
    @Getter
    @Setter
    public static class Weather {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private int weatherId;
        private String main;
        private String description;
        private String icon;

        // Add copy constructor
        public Weather() {}

        public Weather(Weather other) {
            this.weatherId = other.weatherId;
            this.main = other.main;
            this.description = other.description;
            this.icon = other.icon;
        }
    }

    @Entity
    @Table(name = "feels_like")
    @Getter
    @Setter
    public static class FeelsLike {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private double day;
        private double night;
        private double eve;
        private double morn;

        // Add copy constructor
        public FeelsLike() {}

        public FeelsLike(FeelsLike other) {
            this.day = other.day;
            this.night = other.night;
            this.eve = other.eve;
            this.morn = other.morn;
        }
    }

    // Add copy constructor for WeatherResponse
    public WeatherResponse() {}

    public WeatherResponse(WeatherResponse other) {
        this.lat = other.lat;
        this.lon = other.lon;
        this.timezone = other.timezone;
        this.timezone_offset = other.timezone_offset;
        if (other.current != null) {
            this.current = new CurrentWeather(other.current);
        }
        if (other.minutely != null) {
            this.minutely = other.minutely.stream()
                    .map(Minute::new)
                    .collect(java.util.stream.Collectors.toList());
        }
        if (other.hourly != null) {
            this.hourly = other.hourly.stream()
                    .map(Hourly::new)
                    .collect(java.util.stream.Collectors.toList());
        }
        if (other.daily != null) {
            this.daily = other.daily.stream()
                    .map(Daily::new)
                    .collect(java.util.stream.Collectors.toList());
        }
    }
}