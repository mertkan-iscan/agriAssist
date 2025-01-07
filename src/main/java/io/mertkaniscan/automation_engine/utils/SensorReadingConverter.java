package io.mertkaniscan.automation_engine.utils;

import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@Slf4j
public class SensorReadingConverter {

    /**
     * Converts a soil moisture raw reading to a percentage using a calibration polynomial.
     * The polynomial is represented as a Map<String, Integer>, where the key is the coefficient
     * (possibly in scientific notation) and the value is the degree of the term.
     *
     * @param rawReading            The raw sensor reading (0-4095).
     * @param calibrationPolynomial A Map representing the calibration polynomial.
     *                              Example: {"6.308":0,"0.05018":1,"3.893e-05":2,"9.413e-09":3}
     * @return The soil moisture percentage (0-100).
     */
    public static double convertSoilMoistureReading(int rawReading, Map<String, Integer> calibrationPolynomial) {
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
}