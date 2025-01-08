package io.mertkaniscan.automation_engine.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class SensorReadingConverter {

    public double convertSoilMoistureReading(int rawReading, Map<String, Integer> calibrationPolynomial) {
        log.debug("Starting soil moisture conversion. Raw reading: {}, Calibration polynomial: {}",
                rawReading, calibrationPolynomial);

        // Validate raw reading
        if (rawReading < 0 || rawReading > 4095) {
            log.error("Invalid soil moisture sensor reading: {}", rawReading);
            throw new IllegalArgumentException("Invalid soil moisture sensor reading: " + rawReading);
        }

        // Validate calibration polynomial
        if (calibrationPolynomial == null || calibrationPolynomial.isEmpty()) {
            log.error("Calibration polynomial is null or empty.");
            throw new IllegalArgumentException("Calibration polynomial cannot be null or empty.");
        }

        // Convert the raw reading to a mirrored value
        double mirroredValue = 4095 - rawReading;
        log.debug("Mirrored value calculated: {}", mirroredValue);

        // Calculate the percentage using the polynomial
        double percentage = 0.0;
        for (Map.Entry<String, Integer> term : calibrationPolynomial.entrySet()) {
            // Convert the coefficient from String (can be scientific notation) to double
            double coefficient = Double.parseDouble(term.getKey());
            int degree = term.getValue();
            double termValue = coefficient * Math.pow(mirroredValue, degree);
            percentage += termValue;

            log.trace("Processed term: Coefficient: {}, Degree: {}, Term value: {}",
                    coefficient, degree, termValue);
        }

        // Clamp the percentage to the range [0, 100]
        percentage = Math.max(0, Math.min(percentage, 100));
        log.debug("Final soil moisture percentage: {}", percentage);

        return percentage;
    }


    public int convertFlowRate(double requestedFlowRate, Map<String, Integer> calibrationPolynomial, double minFlowRate, double maxFlowRate) {

        log.debug("Starting flow rate conversion. Raw reading: {}, Calibration polynomial: {}", requestedFlowRate, calibrationPolynomial);

        // Validate flow rate boundaries
        if (requestedFlowRate < minFlowRate || requestedFlowRate > maxFlowRate) {
            log.error("Requested flow rate {} is out of bounds. Min: {}, Max: {}", requestedFlowRate, minFlowRate, maxFlowRate);
            throw new IllegalArgumentException("Requested flow rate is out of acceptable range: " + minFlowRate + " to " + maxFlowRate);
        }

        // Validate calibration polynomial
        if (calibrationPolynomial == null || calibrationPolynomial.isEmpty()) {
            log.error("Calibration polynomial is null or empty.");
            throw new IllegalArgumentException("Calibration polynomial cannot be null or empty.");
        }

        // Calculate the servoDegree using the polynomial
        double servoDegree = 0.0;
        for (Map.Entry<String, Integer> term : calibrationPolynomial.entrySet()) {

            // Convert the coefficient from String (can be scientific notation) to double
            double coefficient = Double.parseDouble(term.getKey());
            int degree = term.getValue();
            double termValue = coefficient * Math.pow(requestedFlowRate, degree);
            servoDegree += termValue;

            log.trace("Processed term: Coefficient: {}, Degree: {}, Term value: {}", coefficient, degree, termValue);
        }

        // Clamp the servoDegree between 0 and 90
        servoDegree = Math.max(0, Math.min(servoDegree, 90));
        log.debug("Final flow rate degree before conversion to int: {}", servoDegree);

        // Convert to an integer
        int servoDegreeInt = (int) Math.round(servoDegree);
        log.debug("Final flow rate degree as int: {}", servoDegreeInt);

        return servoDegreeInt;
    }
}