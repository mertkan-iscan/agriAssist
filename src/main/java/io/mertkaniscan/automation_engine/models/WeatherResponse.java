package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    @ManyToOne
    @JoinColumn(name = "fieldID", nullable = false)
    @JsonBackReference(value = "field-weatherResponses")
    private Field field;

    @OneToOne
    @JoinColumn(name = "dayID", unique = true, nullable = false)
    private Day day;

    @OneToOne(cascade = CascadeType.ALL)
    //@JsonManagedReference("weatherResponse-currentWeathers")
    private CurrentWeather current;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    //@JsonManagedReference("weatherResponse-minutely")
    private List<Minute> minutely;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    //@JsonManagedReference("weatherResponse-hourly")
    private List<Hourly> hourly;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    //@JsonManagedReference("weatherResponse-daily")
    private List<Daily> daily;

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

        @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private List<Weather> weather;

        @OneToOne(cascade = CascadeType.ALL)
        private Rain rain;
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

        @OneToOne(cascade = CascadeType.ALL)
        private Temp temp;

        @OneToOne(cascade = CascadeType.ALL)
        private FeelsLike feels_like;

        private int pressure;
        private int humidity;
        private double dew_point;
        private double wind_speed;
        private int wind_deg;
        private double wind_gust;

        @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private List<Weather> weather;

        private int clouds;
        private double pop;
        private double uvi;
        private double rain;
        private String summary;
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

        @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private List<Weather> weather;

        private double pop;

        @OneToOne(cascade = CascadeType.ALL)
        private Rain rain;

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
    }
}
