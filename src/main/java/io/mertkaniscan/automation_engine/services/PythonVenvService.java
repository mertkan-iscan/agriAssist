package io.mertkaniscan.automation_engine.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.stream.Collectors;

@Service
public class PythonVenvService {

    private static final Logger logger = LogManager.getLogger(PythonVenvService.class);

    private Process pythonProcess;

    @Value("${venv.directory}")
    private String venvDirectory;

    private final String requirementsFile = "src/main/resources/python_module_requirements.txt";
    private final String pythonScript = "src/main/java/io/mertkaniscan/automation_engine/python_scripts/main.py";

    @PostConstruct
    public void initializePythonEnvironment() {
        logger.info("Initializing Python environment...");
        createVenv();
        installDependencies(requirementsFile);
        runPythonScript();
        addShutdownHook();
    }

    @PreDestroy
    public void stopPythonScript() {
        logger.info("Stopping Python script...");
        if (pythonProcess != null && pythonProcess.isAlive()) {
            pythonProcess.destroy();

            try {
                if (!pythonProcess.waitFor(5, TimeUnit.SECONDS)) {
                    pythonProcess.destroyForcibly();
                    logger.warn("Python script forcibly terminated.");
                } else {
                    logger.info("Python script terminated gracefully.");
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
            logger.info("Virtual environment already exists: {}", venvDirectory);
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String[] createVenvCommand = os.contains("win")
                ? new String[]{"python", "-m", "venv", venvDirectory}
                : new String[]{"python3", "-m", "venv", venvDirectory};

        try {
            logger.info("Creating virtual environment using command: {}", String.join(" ", createVenvCommand));
            ProcessBuilder processBuilder = new ProcessBuilder(createVenvCommand);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("Virtual environment created successfully at {}", venvDirectory);
            } else {
                logger.error("Virtual environment creation failed with exit code {}", exitCode);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error while creating virtual environment", e);
        }
    }

    public boolean isVenvCreated() {
        String activatePath = venvDirectory + (System.getProperty("os.name").toLowerCase().contains("win")
                ? "\\Scripts\\activate"
                : "/bin/activate");
        boolean exists = new java.io.File(activatePath).exists();
        logger.debug("Checking if virtual environment is created at {}: {}", activatePath, exists);
        return exists;
    }

    public void installDependencies(String requirementsFile) {
        if (!isVenvCreated()) {
            logger.warn("Virtual environment not found. Please create it first.");
            return;
        }

        String pipCommand = System.getProperty("os.name").toLowerCase().contains("win")
                ? venvDirectory + "\\Scripts\\pip"
                : venvDirectory + "/bin/pip";

        try {
            List<String> dependencies = Files.readAllLines(Paths.get(requirementsFile));
            logger.debug("Dependencies to install: {}", dependencies);

            for (String dependency : dependencies) {
                if (dependency.trim().isEmpty() || dependency.startsWith("#")) {
                    logger.debug("Skipping dependency: {}", dependency);
                    continue;
                }

                logger.info("Installing dependency: {}", dependency);
                ProcessBuilder processBuilder = new ProcessBuilder(pipCommand, "install", dependency);
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                // Capture both stdout and stderr
                String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
                        .lines()
                        .collect(Collectors.joining("\n"));

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    logger.info("Successfully installed dependency: {}\nOutput:\n{}", dependency, output);
                } else {
                    logger.error("Failed to install dependency: {}. Exit code: {}.\nOutput:\n{}", dependency, exitCode, output);
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error while installing dependencies", e);
        }
    }

    public void runPythonScript() {
        if (!isVenvCreated()) {
            logger.warn("Virtual environment not found. Please create it first.");
            return;
        }

        String pythonCommand = venvDirectory + (System.getProperty("os.name").toLowerCase().contains("win")
                ? "\\Scripts\\python"
                : "/bin/python");

        String[] runScriptCommand = {pythonCommand, pythonScript};

        try {
            logger.info("Starting Python script using command: {}", String.join(" ", runScriptCommand));
            ProcessBuilder processBuilder = new ProcessBuilder(runScriptCommand);
            processBuilder.redirectErrorStream(true);
            pythonProcess = processBuilder.start();

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.debug("Python script output: {}", line);
                    }
                } catch (IOException e) {
                    logger.error("Error reading Python script output", e);
                }
            }).start();

            logger.info("Python script is running in the background.");
        } catch (IOException e) {
            logger.error("Failed to start Python script", e);
        }
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("JVM shutting down. Stopping Python script...");
            stopPythonScript();
        }));
    }
}