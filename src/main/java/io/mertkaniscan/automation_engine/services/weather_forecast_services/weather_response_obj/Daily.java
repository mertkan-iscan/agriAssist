package io.mertkaniscan.automation_engine.services.weather_forecast_services.weather_response_obj;

import java.util.List;

public class Daily {

    public long dt;

    public long sunrise;
    public long sunset;

    public long moonrise;
    public long moonset;
    public double moon_phase;

    public Temp temp;
    public FeelsLike feels_like;

    public int pressure;

    public int humidity;

    public double dew_point;

    public double wind_speed;
    public int wind_deg;
    public double wind_gust;

    public List<Weather> weather;

    public int clouds;

    public double pop;

    public double uvi;

    public double rain;

    private String summary;


    public long getSunrise() {
        return sunrise;
    }

    public void setSunrise(long sunrise) {
        this.sunrise = sunrise;
    }

    public long getSunset() {
        return sunset;
    }

    public void setSunset(long sunset) {
        this.sunset = sunset;
    }

    public long getMoonrise() {
        return moonrise;
    }

    public void setMoonrise(long moonrise) {
        this.moonrise = moonrise;
    }

    public long getMoonset() {
        return moonset;
    }

    public void setMoonset(long moonset) {
        this.moonset = moonset;
    }

    public double getMoon_phase() {
        return moon_phase;
    }

    public void setMoon_phase(double moon_phase) {
        this.moon_phase = moon_phase;
    }

    public Temp getTemp() {
        return temp;
    }

    public void setTemp(Temp temp) {
        this.temp = temp;
    }

    public FeelsLike getFeels_like() {
        return feels_like;
    }

    public void setFeels_like(FeelsLike feels_like) {
        this.feels_like = feels_like;
    }

    public int getPressure() {
        return pressure;
    }

    public void setPressure(int pressure) {
        this.pressure = pressure;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public double getDew_point() {
        return dew_point;
    }

    public void setDew_point(double dew_point) {
        this.dew_point = dew_point;
    }

    public double getWind_speed() {
        return wind_speed;
    }

    public void setWind_speed(double wind_speed) {
        this.wind_speed = wind_speed;
    }

    public int getWind_deg() {
        return wind_deg;
    }

    public void setWind_deg(int wind_deg) {
        this.wind_deg = wind_deg;
    }

    public double getWind_gust() {
        return wind_gust;
    }

    public void setWind_gust(double wind_gust) {
        this.wind_gust = wind_gust;
    }

    public List<Weather> getWeather() {
        return weather;
    }

    public void setWeather(List<Weather> weather) {
        this.weather = weather;
    }

    public int getClouds() {
        return clouds;
    }

    public void setClouds(int clouds) {
        this.clouds = clouds;
    }

    public double getPop() {
        return pop;
    }

    public void setPop(double pop) {
        this.pop = pop;
    }

    public double getUvi() {
        return uvi;
    }

    public void setUvi(double uvi) {
        this.uvi = uvi;
    }

    public double getRain() {
        return rain;
    }

    public void setRain(double rain) {
        this.rain = rain;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public long getDt() {
        return dt;
    }

    public void setDt(long dt) {
        this.dt = dt;
    }
}