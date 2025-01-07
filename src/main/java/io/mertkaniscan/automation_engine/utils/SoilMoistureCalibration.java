package io.mertkaniscan.automation_engine.utils;

import java.util.Arrays;

public class SoilMoistureCalibration {

    public static class CalibrationResult {
        public final String status;
        public final String message;
        public final Result result;

        public CalibrationResult(String status, String message, Result result) {
            this.status = status;
            this.message = message;
            this.result = result;
        }
    }

    public static class Result {
        public final int degree;
        public final double[] coefficients;
        public final double rSquared;
        public final double aic;
        public final double bic;

        public Result(int degree, double[] coefficients, double rSquared, double aic, double bic) {
            this.degree = degree;
            this.coefficients = coefficients;
            this.rSquared = rSquared;
            this.aic = aic;
            this.bic = bic;
        }
    }

    public static class CalibrationData {
        public final double[] sensorReadings;
        public final double[] moisturePercentages;

        public CalibrationData(double[] sensorReadings, double[] moisturePercentages) {
            this.sensorReadings = sensorReadings;
            this.moisturePercentages = moisturePercentages;
        }
    }

    public static CalibrationResult soilMoistureCalibration(CalibrationData data) {
        try {
            // Sensör değerlerini 4095'ten çıkar
            double[] sensorDiff = Arrays.stream(data.sensorReadings)
                    .map(x -> 4095 - x)
                    .toArray();

            int[] degrees = {1, 2, 3, 4};
            double bestRSquared = Double.NEGATIVE_INFINITY;
            double[] bestPolynomial = null;
            int bestDegree = 0;
            double bestAIC = Double.POSITIVE_INFINITY;
            double bestBIC = Double.POSITIVE_INFINITY;

            int n = data.sensorReadings.length;

            for (int degree : degrees) {
                // Polinom katsayılarını hesapla
                double[] coefficients = fitPolynomial(sensorDiff, data.moisturePercentages, degree);

                // Tahmin edilen değerleri hesapla
                double[] predictedMoisture = new double[n];
                for (int i = 0; i < n; i++) {
                    predictedMoisture[i] = evaluatePolynomial(coefficients, sensorDiff[i]);
                }

                // Kalite metriklerini hesapla
                double[] residuals = new double[n];
                double meanMoisture = Arrays.stream(data.moisturePercentages).average().orElse(0);
                double rss = 0;
                double ssTot = 0;

                for (int i = 0; i < n; i++) {
                    residuals[i] = data.moisturePercentages[i] - predictedMoisture[i];
                    rss += residuals[i] * residuals[i];
                    ssTot += Math.pow(data.moisturePercentages[i] - meanMoisture, 2);
                }

                double rSquared = 1 - (rss / ssTot);

                int k = degree + 1;
                double aic = n * Math.log(rss / n) + 2 * k;
                double bic = n * Math.log(rss / n) + k * Math.log(n);

                if (aic < bestAIC && bic < bestBIC) {
                    bestRSquared = rSquared;
                    bestPolynomial = coefficients;
                    bestDegree = degree;
                    bestAIC = aic;
                    bestBIC = bic;
                }
            }

            Result result = new Result(bestDegree, bestPolynomial, bestRSquared, bestAIC, bestBIC);
            return new CalibrationResult("success", "Calibration completed successfully.", result);

        } catch (Exception e) {
            return new CalibrationResult("error", "Calibration failed: " + e.getMessage(), null);
        }
    }

    // Polinom fitting yardımcı metodu
    private static double[] fitPolynomial(double[] x, double[] y, int degree) {
        int n = x.length;
        double[][] matrix = new double[degree + 1][degree + 1];
        double[] vector = new double[degree + 1];

        // Normal denklem matrisini oluştur
        for (int i = 0; i <= degree; i++) {
            for (int j = 0; j <= degree; j++) {
                for (int k = 0; k < n; k++) {
                    matrix[i][j] += Math.pow(x[k], i + j);
                }
            }

            for (int k = 0; k < n; k++) {
                vector[i] += y[k] * Math.pow(x[k], i);
            }
        }

        return solveSystem(matrix, vector);
    }

    // Polinom değerlendirme yardımcı metodu
    private static double evaluatePolynomial(double[] coefficients, double x) {
        double result = 0;
        for (int i = 0; i < coefficients.length; i++) {
            result += coefficients[i] * Math.pow(x, i);
        }
        return result;
    }

    // Lineer denklem sistemi çözücü
    private static double[] solveSystem(double[][] A, double[] b) {
        int n = A.length;

        // Gaussian elimination
        for (int k = 0; k < n - 1; k++) {
            for (int i = k + 1; i < n; i++) {
                double factor = A[i][k] / A[k][k];
                for (int j = k; j < n; j++) {
                    A[i][j] -= factor * A[k][j];
                }
                b[i] -= factor * b[k];
            }
        }

        // Back substitution
        double[] solution = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0;
            for (int j = i + 1; j < n; j++) {
                sum += A[i][j] * solution[j];
            }
            solution[i] = (b[i] - sum) / A[i][i];
        }

        return solution;
    }

    public static void main(String[] args) {

        double[] sensorReadings = {4095, 4095, 4095, 3724, 3290, 2407, 1778, 1582, 1330, 1120, 1045, 948, 883, 784, 816, 759, 800};
        double[] moisturePercentages = {0, 6.67, 13.33, 20, 23.33, 26.67, 33.33, 40, 46.67, 53.33, 60, 66.67, 73.33, 80, 86.67, 93.33, 100};

        CalibrationData data = new CalibrationData(sensorReadings, moisturePercentages);
        CalibrationResult result = soilMoistureCalibration(data);

        if (result.status.equals("success")) {
            System.out.println("Best degree: " + result.result.degree);
            System.out.println("R-squared: " + result.result.rSquared);
            System.out.println("AIC: " + result.result.aic);
            System.out.println("BIC: " + result.result.bic);
            System.out.println("Coefficients: " + Arrays.toString(result.result.coefficients));
        } else {
            System.out.println("Error: " + result.message);
        }
    }
}