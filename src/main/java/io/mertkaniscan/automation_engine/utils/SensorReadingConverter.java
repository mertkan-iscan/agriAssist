package io.mertkaniscan.automation_engine.utils;

import io.mertkaniscan.automation_engine.components.ScheduledSensorDataFetcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class SensorReadingConverter {

    private static final Logger logger = LogManager.getLogger(SensorReadingConverter.class);

    /**
     * Converts a soil moisture raw reading to a percentage using a calibration polynomial.
     * The polynomial is represented as a Map<Double, Integer>, where the key is the coefficient
     * and the value is the degree of the term.
     *
     * @param rawReading          The raw sensor reading (0-4095).
     * @param calibrationPolynomial A Map representing the calibration polynomial.
     * @return The soil moisture percentage (0-100).
     */
    public static double convertSoilMoistureReading(int rawReading, Map<Double, Integer> calibrationPolynomial) {

        if (rawReading < 0 || rawReading > 4095) {
            throw new IllegalArgumentException("Invalid soil moisture sensor reading: " + rawReading);
        }

        if (calibrationPolynomial == null || calibrationPolynomial.isEmpty()) {
            throw new IllegalArgumentException("Calibration polynomial cannot be null or empty.");
        }

        double mirroredValue = 4095 - rawReading;

        double percentage = 0.0;

        for (Map.Entry<Double, Integer> term : calibrationPolynomial.entrySet()) {
            double coefficient = term.getKey();
            int degree = term.getValue();
            percentage += coefficient * Math.pow(mirroredValue, degree);
        }

        return Math.max(0, Math.min(percentage, 100));
    }
}
