package io.mertkaniscan.automation_engine;

import org.jfree.chart.plot.XYPlot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.ChartUtils;
import java.io.File;
import java.io.IOException;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class TCPClientTest {

    @Test
    public void testSoilSensorCalibration() throws Exception {
        // Prepare mock sensor readings and moisture percentages
        double[] sensorReadings = {1023, 998, 856, 785, 564, 401, 272, 240, 240, 240};
        double[] moisturePercentages = {0, 12.5, 25, 37.5, 50, 62.5, 75, 87.5, 93.7, 100};

        // Prepare expected response JSON from server
        JSONObject responseJson = new JSONObject();
        responseJson.put("degree", 2);
        responseJson.put("coefficients", new JSONArray(new double[]{1.5, -2.0, 0.5}));

        // Convert response to bytes to simulate the server response
        byte[] responseBytes = responseJson.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream serverOut = new ByteArrayOutputStream();
        try (DataOutputStream dataOut = new DataOutputStream(serverOut)) {
            dataOut.writeInt(responseBytes.length); // Send length of the response
            dataOut.write(responseBytes);            // Send the JSON response
        }

        // Mock socket input and output streams
        try (ByteArrayInputStream serverIn = new ByteArrayInputStream(serverOut.toByteArray());
             ByteArrayOutputStream clientOut = new ByteArrayOutputStream()) {

            Socket mockSocket = new MockSocket(clientOut, serverIn);  // Using a mocked socket for testing

            // Test the sendCalibrationData method
            JSONObject response = sendCalibrationData(mockSocket, sensorReadings, moisturePercentages);

            // Assert the response data
            assertEquals(2, response.getInt("degree"));
            JSONArray coefficientsJson = response.getJSONArray("coefficients");
            double[] coefficients = coefficientsJson.toList().stream().mapToDouble(o -> ((Number) o).doubleValue()).toArray();
            assertArrayEquals(new double[]{1.5, -2.0, 0.5}, coefficients);

            // Plot data and polynomial function
            plotDataAndFunction(sensorReadings, moisturePercentages, coefficients);
        }
    }

    private void plotDataAndFunction(double[] sensorReadings, double[] moisturePercentages, double[] coefficients) {
        XYSeries dataSeries = new XYSeries("Sensor Data");
        XYSeries functionSeries = new XYSeries("Polynomial Fit");

        // Add sensor readings as data points
        for (int i = 0; i < sensorReadings.length; i++) {
            dataSeries.add(sensorReadings[i], moisturePercentages[i]);
        }

        // Calculate polynomial fit points within sensor readings range
        for (double x = 0; x <= 1023; x += 10) {
            double y = 0;
            for (int i = 0; i < coefficients.length; i++) {
                y += coefficients[i] * Math.pow(x, coefficients.length - 1 - i);
            }
            functionSeries.add(x, y);
        }

        // Create dataset and chart
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(dataSeries);
        dataset.addSeries(functionSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Soil Sensor Calibration",
                "Sensor Reading (0 - 1023)",
                "Moisture Percentage (0 - 100)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // Set the axis range to focus on relevant data points
        XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setRange(0, 1023);  // X-axis range based on sensor readings
        plot.getRangeAxis().setRange(0, 100);    // Y-axis range based on moisture percentages

        // Construct the polynomial equation as a string
        StringBuilder equation = new StringBuilder("y = ");
        for (int i = 0; i < coefficients.length; i++) {
            double coeff = coefficients[i];
            int power = coefficients.length - 1 - i;

            // Format the coefficient
            if (i > 0 && coeff >= 0) {
                equation.append("+ ");
            }
            equation.append(String.format("%.2f", coeff));

            // Append the variable and power
            if (power > 0) {
                equation.append("x");
                if (power > 1) {
                    equation.append("^").append(power);
                }
                equation.append(" ");
            }
        }

        // Add polynomial equation as a subtitle
        chart.addSubtitle(new org.jfree.chart.title.TextTitle(equation.toString()));

        // Save chart as an image
        try {
            File imageFile = new File("soil_sensor_calibration_chart.png");
            ChartUtils.saveChartAsPNG(imageFile, chart, 800, 600);
            System.out.println("Chart saved as: " + imageFile.getAbsolutePath());
            System.out.println("Polynomial Equation: " + equation);
        } catch (IOException e) {
            System.err.println("Error saving chart as PNG: " + e.getMessage());
        }
    }



    private JSONObject sendCalibrationData(Socket socket, double[] sensorReadings, double[] moisturePercentages) throws Exception {
        JSONObject json = new JSONObject();
        json.put("task", "soil_sensor_calibration");
        JSONObject data = new JSONObject();
        data.put("sensor_readings", new JSONArray(sensorReadings));
        data.put("moisture_percentages", new JSONArray(moisturePercentages));
        json.put("data", data);

        byte[] jsonBytes = json.toString().getBytes(StandardCharsets.UTF_8);
        try (DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {
            dataOut.writeInt(jsonBytes.length);
            dataOut.write(jsonBytes);
            dataOut.flush();
        }

        try (DataInputStream dataIn = new DataInputStream(socket.getInputStream())) {
            int responseLength = dataIn.readInt();
            byte[] responseBytes = new byte[responseLength];
            dataIn.readFully(responseBytes);
            return new JSONObject(new String(responseBytes, StandardCharsets.UTF_8));
        }
    }

    private static class MockSocket extends Socket {
        private final ByteArrayOutputStream clientOut;
        private final ByteArrayInputStream serverIn;

        public MockSocket(ByteArrayOutputStream clientOut, ByteArrayInputStream serverIn) {
            this.clientOut = clientOut;
            this.serverIn = serverIn;
        }

        @Override
        public InputStream getInputStream() {
            return serverIn;
        }

        @Override
        public OutputStream getOutputStream() {
            return clientOut;
        }
    }

    private static class ChartFrame extends ApplicationFrame {
        public ChartFrame(String title, JFreeChart chart) {
            super(title);
            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
            setContentPane(chartPanel);
        }
    }
}
