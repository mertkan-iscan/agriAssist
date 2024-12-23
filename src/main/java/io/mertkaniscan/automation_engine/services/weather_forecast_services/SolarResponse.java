package io.mertkaniscan.automation_engine.services.weather_forecast_services;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SolarResponse {
    private Long id;
    private Double lat;
    private Double lon;
    private String date;
    private String tz;
    private String sunrise;
    private String sunset;
    private Irradiance irradiance;
    private Day day;

    @Data
    @NoArgsConstructor
    public static class Irradiance {
        private Long id;
        private List<Daily> daily;
        private List<Hourly> hourly;

        @Data
        @NoArgsConstructor
        public static class Daily {
            private Long id;

            @JsonProperty("clear_sky")
            private SkyData clearSky;

            @JsonProperty("cloudy_sky")
            private SkyData cloudySky;
        }

        @Data
        @NoArgsConstructor
        public static class Hourly {
            private Long id;
            private Integer hour;

            @JsonProperty("clear_sky")
            private SkyData clearSky;

            @JsonProperty("cloudy_sky")
            private SkyData cloudySky;
        }

        @Data
        @NoArgsConstructor
        public static class SkyData {
            private Double ghi;
            private Double dni;
            private Double dhi;
        }
    }

    @Data
    @NoArgsConstructor
    public static class Day {
        private Long id;
    }
}
