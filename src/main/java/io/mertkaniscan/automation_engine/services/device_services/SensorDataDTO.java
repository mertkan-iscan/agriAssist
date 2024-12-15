package io.mertkaniscan.automation_engine.services.device_services;


public class SensorDataDTO {
    private String dataType;
    private Double dataValue;

    public SensorDataDTO(String dataType, Double dataValue) {
        this.dataType = dataType;
        this.dataValue = dataValue;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Double getDataValue() {
        return dataValue;
    }

    public void setDataValue(Double dataValue) {
        this.dataValue = dataValue;
    }
}
