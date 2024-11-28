package io.mertkaniscan.automation_engine.services.weather_forecast_services;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SolarResponse {
    private double lat;
    private double lon;
    private String date;
    private String tz;
    private String sunrise;
    private String sunset;
    private Irradiance irradiance;

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTz() {
        return tz;
    }

    public void setTz(String tz) {
        this.tz = tz;
    }

    public String getSunrise() {
        return sunrise;
    }

    public void setSunrise(String sunrise) {
        this.sunrise = sunrise;
    }

    public String getSunset() {
        return sunset;
    }

    public void setSunset(String sunset) {
        this.sunset = sunset;
    }

    public Irradiance getIrradiance() {
        return irradiance;
    }

    public void setIrradiance(Irradiance irradiance) {
        this.irradiance = irradiance;
    }

    // Getters and Setters

    public static class Irradiance {
        private List<DailyIrradiance> daily;
        private List<HourlyIrradiance> hourly;

        public List<DailyIrradiance> getDaily() {
            return daily;
        }

        public void setDaily(List<DailyIrradiance> daily) {
            this.daily = daily;
        }

        public List<HourlyIrradiance> getHourly() {
            return hourly;
        }

        public void setHourly(List<HourlyIrradiance> hourly) {
            this.hourly = hourly;
        }

        public static class DailyIrradiance {

            @JsonProperty("clear_sky")
            private SkyIrradiance clearSky;

            @JsonProperty("cloudy_sky")
            private SkyIrradiance cloudySky;

            public SkyIrradiance getClearSky() {
                return clearSky;
            }

            public void setClearSky(SkyIrradiance clearSky) {
                this.clearSky = clearSky;
            }

            public SkyIrradiance getCloudySky() {
                return cloudySky;
            }

            public void setCloudySky(SkyIrradiance cloudySky) {
                this.cloudySky = cloudySky;
            }
        }

        public static class HourlyIrradiance {
            private int hour;

            @JsonProperty("clear_sky")
            private SkyIrradiance clearSky;

            @JsonProperty("cloudy_sky")
            private SkyIrradiance cloudySky;

            public SkyIrradiance getCloudySky() {
                return cloudySky;
            }

            public void setCloudySky(SkyIrradiance cloudySky) {
                this.cloudySky = cloudySky;
            }

            public SkyIrradiance getClearSky() {
                return clearSky;
            }

            public void setClearSky(SkyIrradiance clearSky) {
                this.clearSky = clearSky;
            }

            public int getHour() {
                return hour;
            }

            public void setHour(int hour) {
                this.hour = hour;
            }
        }

        public static class SkyIrradiance {
            private double ghi;
            private double dni;
            private double dhi;

            public double getGhi() {
                return ghi;
            }

            public void setGhi(double ghi) {
                this.ghi = ghi;
            }

            public double getDhi() {
                return dhi;
            }

            public void setDhi(double dhi) {
                this.dhi = dhi;
            }

            public double getDni() {
                return dni;
            }

            public void setDni(double dni) {
                this.dni = dni;
            }
        }
    }
}
