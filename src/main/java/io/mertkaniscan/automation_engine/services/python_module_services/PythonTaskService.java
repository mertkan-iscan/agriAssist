package io.mertkaniscan.automation_engine.services.python_module_services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PythonTaskService {

    private static final Logger logger = LogManager.getLogger(PythonTaskService.class);

    private final NetworkService networkService;

    @Autowired
    public PythonTaskService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public JSONObject sendCalibrationData(String serverHost, int serverPort, double[] sensorReadings, double[] moisturePercentages) {
        JSONObject json = new JSONObject();
        json.put("task", "soil_sensor_calibrator");
        JSONObject data = new JSONObject();
        data.put("sensor_readings", new JSONArray(sensorReadings));
        data.put("moisture_percentages", new JSONArray(moisturePercentages));
        json.put("data", data);

        return networkService.sendData(serverHost, serverPort, json);
    }

    public JSONObject sendSoilWaterCalculation(String serverHost, int serverPort, double[][] sensorReadings, double radius, double height, String mode, double[] calibrationCoeffs) {
        JSONObject json = new JSONObject();
        json.put("task", "soil_water_calculator");

        JSONObject data = new JSONObject();
        data.put("sensor_readings", new JSONArray(sensorReadings));
        data.put("radius", radius);
        data.put("height", height);
        data.put("mode", mode);

        if (calibrationCoeffs != null && calibrationCoeffs.length > 0) {
            data.put("calibration_coeffs", new JSONArray(calibrationCoeffs));
        }

        json.put("data", data);

        return networkService.sendData(serverHost, serverPort, json);
    }

    public JSONObject sendInterpolationSoilWaterPercentage(String serverHost, int serverPort, double[][] sensorReadings, double radius, double height, double[] calibrationCoeffs) {
        JSONObject json = new JSONObject();
        json.put("task", "interpolation_soil_water_percentage");

        JSONObject data = new JSONObject();
        data.put("sensor_readings", new JSONArray(sensorReadings));
        data.put("radius", radius);
        data.put("height", height);

        if (calibrationCoeffs != null && calibrationCoeffs.length > 0) {
            data.put("calibration_coeffs", new JSONArray(calibrationCoeffs));
        }

        json.put("data", data);

        return networkService.sendData(serverHost, serverPort, json);
    }
}
