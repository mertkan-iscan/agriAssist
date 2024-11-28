package io.mertkaniscan.automation_engine.services.weather_forecast_services.weather_response_obj;

public class Minute {

    public long dt;
    public double precipitation;

    public long getDt() {
        return dt;
    }

    public void setDt(long dt) {
        this.dt = dt;
    }

    public double getPrecipitation() {
        return precipitation;
    }

    public void setPrecipitation(double precipitation) {
        this.precipitation = precipitation;
    }
}
