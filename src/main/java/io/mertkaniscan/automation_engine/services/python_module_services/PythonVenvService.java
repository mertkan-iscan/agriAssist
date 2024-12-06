package io.mertkaniscan.automation_engine.services.python_module_services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

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

        createVenv();

        installDependencies(requirementsFile);

        runPythonScript();

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
        String[] installCommand;

        if (os.contains("win")) {
            installCommand = new String[]{
                    venvDirectory + "\\Scripts\\pip",
                    "install",
                    "-r",
                    requirementsFile
            };
        } else {
            installCommand = new String[]{
                    venvDirectory + "/bin/pip",
                    "install",
                    "-r",
                    requirementsFile
            };
        }

        try {
            logger.info("Installing dependencies from requirements file...");
            ProcessBuilder installProcess = new ProcessBuilder(installCommand);
            installProcess.redirectErrorStream(true);
            Process process = installProcess.start();

            // Capture and log output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("Dependencies installed successfully.");
            } else {
                logger.error("Failed to install dependencies. Exit code: " + exitCode);
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
            runScriptCommand = new String[]{"cmd.exe", "/c", "start cmd /k " + venvDirectory + "\\Scripts\\python " + pythonScript};
        } else {
            runScriptCommand = new String[]{"/bin/bash", "-c", "xterm -hold -e " + venvDirectory + "/bin/python " + pythonScript};
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
}