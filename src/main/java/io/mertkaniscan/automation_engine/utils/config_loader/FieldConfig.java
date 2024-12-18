package io.mertkaniscan.automation_engine.utils.config_loader;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Data
public class FieldConfig {

    // Getters and Setters
    private String soilType;            // Toprak türü
    private double fieldCapacity;       // Tarla kapasitesi (0-1 arasında bir oran)
    private double wiltingPoint;        // Solma noktası (0-1 arasında bir oran)
    private double bulkDensity;         // Toprak yoğunluğu (g/cm³)
    private double saturation;          // Maksimum su tutma kapasitesi (0-1 arasında bir oran)
    private double infiltrationRate;    // Su sızma oranı (mm/saat)
    private double maxEvaporationDepth; // Maksimum buharlaşma derinliği (m)

}
