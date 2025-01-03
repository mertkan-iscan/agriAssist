package io.mertkaniscan.automation_engine.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "field_current_values")
public class FieldCurrentValues {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "fieldid", nullable = false)
    private Field field;

    private Double keValue;
    private Double tewValue;
    private Double rewValue;

    private Double tawValue;
    private Double rawValue;

    private Double wetArea;
    private Double deValue;

    private Double forecastWindSpeed;
    private Double forecastHumidity;
    private Double forecastTemperature;

    private Double sensorWindSpeed;
    private Double sensorHumidity;
    private Double sensorTemperature;

    private Double sensorETo;
    private Double forecastETo;

    private Double solarRadiation;

    private Boolean isRaining = false;

    // Default constructor
    public FieldCurrentValues() {
    }

    // Constructor with field
    public FieldCurrentValues(Field field) {
        this.field = field;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FieldCurrentValues{\n");

        // Environment values
        sb.append("  Environment Values:\n");
        appendValue(sb, "    KE", keValue);
        appendValue(sb, "    TEW", tewValue);
        appendValue(sb, "    REW", rewValue);
        appendValue(sb, "    TAW", tawValue);
        appendValue(sb, "    RAW", rawValue);
        appendValue(sb, "    Wet Area", wetArea);
        appendValue(sb, "    DE", deValue);

        // Forecast values
        sb.append("\n  Forecast Values:\n");
        appendValue(sb, "    Wind Speed", forecastWindSpeed);
        appendValue(sb, "    Humidity", forecastHumidity);
        appendValue(sb, "    Temperature", forecastTemperature);
        appendValue(sb, "    ETo", forecastETo);

        // Sensor values
        sb.append("\n  Sensor Values:\n");
        appendValue(sb, "    Wind Speed", sensorWindSpeed);
        appendValue(sb, "    Humidity", sensorHumidity);
        appendValue(sb, "    Temperature", sensorTemperature);
        appendValue(sb, "    ETo", sensorETo);

        // Additional measurements
        sb.append("\n  Additional Measurements:\n");
        appendValue(sb, "    Solar Radiation", solarRadiation);
        appendValue(sb, "    Is Raining", isRaining);

        sb.append("}");
        return sb.toString();
    }

    private void appendValue(StringBuilder sb, String label, Object value) {
        sb.append(String.format("%s: %s\n",
                label,
                value != null ? value : "N/A"));
    }
}