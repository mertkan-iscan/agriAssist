package io.mertkaniscan.automation_engine;


import io.mertkaniscan.automation_engine.services.python_module_services.NetworkService;
import io.mertkaniscan.automation_engine.services.python_module_services.PythonTaskService;
import org.json.JSONObject;

public class PythonModuleTest {
    public static void main(String[] args) {

        NetworkService networkService = new NetworkService();

        PythonTaskService taskService = new PythonTaskService(networkService);

        String serverHost = "127.0.0.1";
        int serverPort = 5432;

        double[] sensorReadings = {4095, 3996, 3428, 3141, 2260, 1606, 1089, 960, 960, 960, 960};
        double[] moisturePercentages = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};

        System.out.println("Testing Calibration Data Task...");

        JSONObject calibrationResponse = taskService.sendCalibrationData(
                serverHost, serverPort, sensorReadings, moisturePercentages);

        System.out.println("Calibration Response: " + calibrationResponse);

        // Test data for soil water calculation
        double[][] sensorReadings2D = {
                {0, 0, 3.3},
                {0, 15, 6.6},
                {0, 30, 3.3},
                {5, 0, 6.6},
                {5, 15, 6.6},
                {5, 30, 6.6}
        };

        double radius = 5.0;
        double height = 10.0;
        String mode = "sphere";
        double[] coeffs = {1, 2, 3};

        System.out.println("Testing Soil Water Calculation Task...");

        JSONObject soilWaterResponse = taskService.sendSoilWaterVolumeCalculation(
                serverHost, serverPort, sensorReadings2D, radius, height, mode, coeffs);

        System.out.println("Soil Water Calculation Response: " + soilWaterResponse);

        // Test data for soil water volume from calibrated moisture
        double[][] calibratedMoisture = {
                {0, 0, 10.5},
                {0, 15, 15.0},
                {0, 30, 20.0},
                {5, 0, 18.0},
                {5, 15, 22.5},
                {5, 30, 25.0}
        };

        System.out.println("Testing Soil Water Volume from Calibrated Moisture Task...");

        JSONObject soilWaterFromCalibratedResponse = taskService.sendSoilWaterVolumeFromCalibratedMoisture(
                serverHost, serverPort, calibratedMoisture, radius, height);

        System.out.println("Soil Water Volume from Calibrated Moisture Response: " + soilWaterFromCalibratedResponse);
    }
}
