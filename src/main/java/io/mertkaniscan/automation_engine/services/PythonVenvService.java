package io.mertkaniscan.automation_engine.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
public class PythonVenvService {

    private static final Logger logger = LogManager.getLogger(PythonVenvService.class);

    private Process pythonProcess;

    // Directory for creating the Python virtual environment
    private final String venvDirectory = "myenv";
    private final String requirementsFile = "src/main/resources/python_module_requirements.txt";
    private final String pythonScript = "src/main/java/io/mertkaniscan/automation_engine/python_scripts/main.py";

    @PostConstruct
    public void initializePythonEnvironment() {
        // Create virtual environment
        createVenv();
        // Install dependencies
        installDependencies(requirementsFile);
        // Run Python script
        runPythonScript();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("JVM is shutting down, Python script will be stopped.");
            stopPythonScript();
        }));
    }

    @PreDestroy
    public void stopPythonScript() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            // Terminate the Python script
            pythonProcess.destroy(); // Sends SIGTERM

            try {
                // If it doesn't stop within 5 seconds, forcibly terminate
                if (!pythonProcess.waitFor(5, TimeUnit.SECONDS)) {
                    pythonProcess.destroyForcibly(); // Force termination (SIGKILL)
                    logger.warn("Python script was forcibly terminated.");
                } else {
                    logger.info("Python script shut down gracefully.");
                }
            } catch (InterruptedException e) {
                logger.error("Error while waiting for Python process to terminate", e);
            }
        } else {
            logger.info("Python script is not running.");
        }
    }

    public void createVenv() {
        if (isVenvCreated()) {
            logger.info("Virtual environment already created.");
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String[] createVenvCommand;

        if (os.contains("win")) {
            createVenvCommand = new String[]{"python", "-m", "venv", venvDirectory};
        } else {
            createVenvCommand = new String[]{"python3", "-m", "venv", venvDirectory};
        }

        try {
            logger.info("Creating virtual environment...");
            ProcessBuilder createVenvProcess = new ProcessBuilder(createVenvCommand);
            Process process = createVenvProcess.start();
            process.waitFor();
            logger.info("Virtual environment created.");
        } catch (IOException | InterruptedException e) {
            logger.error("Error creating virtual environment", e);
        }
    }

    public boolean isVenvCreated() {
        String os = System.getProperty("os.name").toLowerCase();
        String activatePath;

        if (os.contains("win")) {
            activatePath = venvDirectory + "\\Scripts\\activate";
        } else {
            activatePath = venvDirectory + "/bin/activate";
        }

        return new java.io.File(activatePath).exists();
    }

    public void installDependencies(String requirementsFile) {
        if (!isVenvCreated()) {
            logger.warn("Virtual environment not found. Please create the virtual environment first.");
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String[] installCommandBase;

        if (os.contains("win")) {
            installCommandBase = new String[]{"cmd.exe", "/c", venvDirectory + "\\Scripts\\pip install"};
        } else {
            installCommandBase = new String[]{"/bin/bash", "-c", venvDirectory + "/bin/pip install"};
        }

        try {
            // Read requirements.txt file
            List<String> dependencies = Files.readAllLines(Paths.get(requirementsFile));

            for (String dependency : dependencies) {
                if (dependency.trim().isEmpty() || dependency.startsWith("#")) {
                    // Skip empty or comment lines
                    continue;
                }

                // Create install command for each dependency
                String[] installCommand = new String[installCommandBase.length + 1];
                System.arraycopy(installCommandBase, 0, installCommand, 0, installCommandBase.length);
                installCommand[installCommandBase.length] = dependency;

                // Start the installation process
                logger.info("Installing: " + dependency);
                ProcessBuilder installProcess = new ProcessBuilder(installCommand);
                Process process = installProcess.start();

                // Log the output of the installation
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.info(line);
                    }
                }

                // Wait for installation to complete
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    logger.info("Successfully installed: " + dependency);
                } else {
                    logger.error("Error occurred during installation of: " + dependency);
                }
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Error installing dependencies", e);
        }
    }

    public void runPythonScript() {
        if (!isVenvCreated()) {
            logger.warn("Virtual environment not found. Please create the virtual environment first.");
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String[] runScriptCommand;

        if (os.contains("win")) {
            runScriptCommand = new String[]{"cmd.exe", "/c", venvDirectory + "\\Scripts\\python " + pythonScript};
        } else {
            runScriptCommand = new String[]{"/bin/bash", "-c", venvDirectory + "/bin/python " + pythonScript};
        }

        try {
            logger.info("Starting Python script...");
            ProcessBuilder runScriptProcess = new ProcessBuilder(runScriptCommand);

            // Redirect error and output streams
            runScriptProcess.redirectErrorStream(true);
            pythonProcess = runScriptProcess.start();

            // Background thread to read output from the script
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.info(line); // Log output from Python script
                    }
                } catch (IOException e) {
                    logger.error("Error reading output from Python script", e);
                }
            }).start();

            logger.info("Python script is running in the background.");

        } catch (IOException e) {
            logger.error("Failed to start Python script", e);
        }
    }

    public JSONObject sendCalibrationDataToServer(String serverHost, int serverPort, double[] sensorReadings, double[] moisturePercentages) {
        Socket socket = null;
        try {
            // Connect to server
            socket = new Socket(serverHost, serverPort);

            // Prepare JSON data (includes task name and data)
            JSONObject json = new JSONObject();
            json.put("task", "soil_sensor_calibration");
            JSONObject data = new JSONObject();
            data.put("sensor_readings", new JSONArray(sensorReadings));
            data.put("moisture_percentages", new JSONArray(moisturePercentages));
            json.put("data", data);  // Data

            // Convert JSON to UTF-8 byte array
            String jsonData = json.toString();
            byte[] jsonBytes = jsonData.getBytes(StandardCharsets.UTF_8);

            // Send data length and data (as byte[])
            OutputStream out = socket.getOutputStream();
            DataOutputStream dataOut = new DataOutputStream(out);
            dataOut.writeInt(jsonBytes.length); // Send data length (4 bytes)
            dataOut.write(jsonBytes); // Send JSON data as byte array
            dataOut.flush();

            // Receive response from server as bytes
            InputStream in = socket.getInputStream();
            DataInputStream dataIn = new DataInputStream(in);

            // Read response length (4-byte length)
            int responseLength = dataIn.readInt();
            byte[] responseBytes = new byte[responseLength];
            dataIn.readFully(responseBytes);

            // Convert response to UTF-8 format
            String response = new String(responseBytes, StandardCharsets.UTF_8);

            // Return JSON response
            return new JSONObject(response);

        } catch (Exception e) {
            logger.error("Connection failed or server error", e);
            return new JSONObject().put("error", "Connection failed or server error.");
        } finally {
            // Close connection
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.error("Error closing socket", e);
                }
            }
        }
    }
}
