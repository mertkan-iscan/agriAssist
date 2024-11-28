package io.mertkaniscan.automation_engine.components.config_loader;

public class FieldConfig {

    private String soilType;        // Toprak türü
    private double fieldCapacity;   // Tarla kapasitesi (0-1 arasında bir oran)
    private double wiltingPoint;    // Solma noktası (0-1 arasında bir oran)
    private double bulkDensity;     // Toprak yoğunluğu (g/cm³)
    private double saturation;      // Maksimum su tutma kapasitesi (0-1 arasında bir oran)
    private double infiltrationRate;// Su sızma oranı (mm/saat)

    // Getters and Setters
    public String getSoilType() {
        return soilType;
    }

    public void setSoilType(String soilType) {
        this.soilType = soilType;
    }

    public double getFieldCapacity() {
        return fieldCapacity;
    }

    public void setFieldCapacity(double fieldCapacity) {
        this.fieldCapacity = fieldCapacity;
    }

    public double getWiltingPoint() {
        return wiltingPoint;
    }

    public void setWiltingPoint(double wiltingPoint) {
        this.wiltingPoint = wiltingPoint;
    }

    public double getBulkDensity() {
        return bulkDensity;
    }

    public void setBulkDensity(double bulkDensity) {
        this.bulkDensity = bulkDensity;
    }

    public double getSaturation() {
        return saturation;
    }

    public void setSaturation(double saturation) {
        this.saturation = saturation;
    }

    public double getInfiltrationRate() {
        return infiltrationRate;
    }

    public void setInfiltrationRate(double infiltrationRate) {
        this.infiltrationRate = infiltrationRate;
    }

    @Override
    public String toString() {
        return "FieldConfig{" +
                "soilType='" + soilType + '\'' +
                ", fieldCapacity=" + fieldCapacity +
                ", wiltingPoint=" + wiltingPoint +
                ", bulkDensity=" + bulkDensity +
                ", saturation=" + saturation +
                ", infiltrationRate=" + infiltrationRate +
                '}';
    }
}
