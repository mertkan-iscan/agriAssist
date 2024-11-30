package io.mertkaniscan.automation_engine.services.device_services;

import java.math.BigDecimal;

public class SensorDataDTO {
    private String dataType;
    private BigDecimal dataValue;

    public SensorDataDTO(String dataType, BigDecimal dataValue) {
        this.dataType = dataType;
        this.dataValue = dataValue;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public BigDecimal getDataValue() {
        return dataValue;
    }

    public void setDataValue(BigDecimal dataValue) {
        this.dataValue = dataValue;
    }
}
