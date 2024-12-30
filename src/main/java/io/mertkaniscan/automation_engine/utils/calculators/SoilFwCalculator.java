//package io.mertkaniscan.automation_engine.utils.calculators;
//
//import io.mertkaniscan.automation_engine.models.Field;
//import io.mertkaniscan.automation_engine.utils.config_loader.ConfigLoader;
//import io.mertkaniscan.automation_engine.utils.config_loader.FieldConfig;
//import org.springframework.stereotype.Service;
//
//@Service
//public class SoilFwCalculator {
//
//    private final ConfigLoader config;
//
//    public SoilFwCalculator(ConfigLoader config) {
//        this.config = config;
//    }
//
//    public static double calculateFw(Field field, double initialTheta, double irrigationDuration,
//                                     double emitterFlowRate) {
//
//        FieldConfig fieldConfig = config.getFieldConfig(field.getFieldSoilType().toString());
//
//        if (fieldConfig == null) {
//            throw new IllegalArgumentException("Geçersiz toprak türü: " + field.getFieldSoilType());
//        }
//
//        double absorptionRate = calculateDynamicInfiltrationRate(initialTheta, fieldConfig);
//
//        double wettedRadius = calculateWettedRadius(emitterFlowRate, irrigationDuration, absorptionRate);
//
//        double wettedArea = Math.PI * Math.pow(wettedRadius, 2);
//
//        return wettedArea / field.getTotalArea();
//    }
//
//}
