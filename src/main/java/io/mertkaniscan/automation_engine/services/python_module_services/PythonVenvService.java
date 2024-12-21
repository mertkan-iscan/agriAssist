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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class PythonVenvService {

    private static final Logger logger = LogManager.getLogger(PythonVenvService.class);

    private Process pythonProcess;
    private final ExecutorService watchdogExecutor = Executors.newSingleThreadExecutor();

    @Value("${venv.directory}")
    private String venvDirectory;

    @Value("${venv.start}")
    private boolean venvStart;

    @Value("${python.server.port}")
    private int pythonServerPort;

    private final String requirementsFile = "src/main/resources/python_module_requirements.txt";
    private final String pythonScript = "src/main/java/io/mertkaniscan/automation_engine/python_scripts/main.py";

    @PostConstruct
    public void initializePythonEnvironment() {
        if (venvStart) {
            logger.info("venv.start is set to true. Initializing virtual environment and running Python script...");
            createVenv();
            updatePip();
            installDependencies(requirementsFile);
            runPythonScript();
        } else {
            logger.info("venv.start is set to false. Skipping virtual environment initialization.");
        }
    }

    public void createVenv() {
        if (isVenvCreated()) {
            logger.info("Virtual environment already created.");
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String[] createVenvCommand = os.contains("win")
                ? new String[]{"python", "-m", "venv", venvDirectory}
                : new String[]{"python3", "-m", "venv", venvDirectory};

        try {
            logger.info("Creating virtual environment...");
            Process process = new ProcessBuilder(createVenvCommand).start();
            process.waitFor();
            logger.info("Virtual environment created.");
        } catch (IOException | InterruptedException e) {
            logger.error("Error creating virtual environment", e);
            Thread.currentThread().interrupt();
        }
    }

    public boolean isVenvCreated() {
        String os = System.getProperty("os.name").toLowerCase();
        String activatePath = os.contains("win")
                ? venvDirectory + "\\Scripts\\activate"
                : venvDirectory + "/bin/activate";

        return new java.io.File(activatePath).exists();
    }

    public void updatePip() {
        if (!isVenvCreated()) {
            logger.warn("Virtual environment not found. Please create the virtual environment first.");
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String[] updatePipCommand = os.contains("win")
                ? new String[]{venvDirectory + "\\Scripts\\python", "-m", "pip", "install", "--upgrade", "pip"}
                : new String[]{venvDirectory + "/bin/python", "-m", "pip", "install", "--upgrade", "pip"};

        try {
            logger.info("Updating pip to the latest version...");
            Process process = new ProcessBuilder(updatePipCommand).start();
            logProcessOutput(process, "[Python UpdatePip] ");
            if (process.waitFor() == 0) {
                logger.info("pip updated successfully.");
            } else {
                logger.error("Failed to update pip.");
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error updating pip", e);
            Thread.currentThread().interrupt();
        }
    }

    public void installDependencies(String requirementsFile) {
        if (!isVenvCreated()) {
            logger.warn("Virtual environment not found. Please create the virtual environment first.");
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String[] installCommand = os.contains("win")
                ? new String[]{venvDirectory + "\\Scripts\\pip", "install", "-r", requirementsFile}
                : new String[]{venvDirectory + "/bin/pip", "install", "-r", requirementsFile};

        try {
            logger.info("Installing dependencies...");
            Process process = new ProcessBuilder(installCommand).start();
            logProcessOutput(process, "[Python PipInstall] ");
            if (process.waitFor() == 0) {
                logger.info("Dependencies installed successfully.");
            } else {
                logger.error("Failed to install dependencies.");
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error installing dependencies", e);
            Thread.currentThread().interrupt();
        }
    }

    public void runPythonScript() {
        if (!isVenvCreated()) {
            logger.warn("Virtual environment not found. Please create the virtual environment first.");
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();

        try {
            logger.info("Starting Python script...");

            // Instead of "start" + "cmd.exe /k", we launch Python directly.
            // This way, java is the parent process of the Python interpreter,
            // and we can capture the output streams directly.

            String[] runScriptCommand;
            if (os.contains("win")) {
                runScriptCommand = new String[]{
                        venvDirectory + "\\Scripts\\python",
                        pythonScript
                };
            } else {
                runScriptCommand = new String[]{
                        venvDirectory + "/bin/python",
                        pythonScript
                };
            }

            pythonProcess = new ProcessBuilder(runScriptCommand).start();

            // Capture stdout and stderr in separate threads
            logProcessOutput(pythonProcess, "[Python Script] ");

            logger.info("Python script started. Is process alive? "
                    + (pythonProcess != null && pythonProcess.isAlive()));

        } catch (IOException e) {
            logger.error("Failed to start Python script", e);
        }
    }

    /**
     * Captures and logs the output of a given process (both stdout and stderr) in separate threads.
     */
    private void logProcessOutput(Process process, String prefix) {
        // stdout
        Thread outThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info(prefix + line);
                }
            } catch (IOException e) {
                logger.error("Error reading process STDOUT", e);
            }
        });

        // stderr
        Thread errThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.error(prefix + line);
                }
            } catch (IOException e) {
                logger.error("Error reading process STDERR", e);
            }
        });

        // Start threads
        outThread.start();
        errThread.start();
    }

    /**
     * Stop the running Python script, if alive.
     */
    public void stopPythonScript() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            logger.info("Stopping Python script...");
            pythonProcess.destroy();

            try {
                // Give the process some time to stop
                if (!pythonProcess.waitFor(5, TimeUnit.SECONDS)) {
                    logger.warn("Python script did not exit in time; forcing termination...");
                    pythonProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for Python script to stop", e);
                Thread.currentThread().interrupt();
            }

            if (!pythonProcess.isAlive()) {
                logger.info("Python script stopped successfully.");
            } else {
                logger.warn("Python script is still running.");
            }
        } else {
            logger.info("No running Python process found to stop.");
        }
    }

    @PreDestroy
    public void cleanup() {
        // Stop Python script if it's still running
        stopPythonScript();
        watchdogExecutor.shutdownNow();
        logger.info("PythonVenvService resources cleaned up.");
    }
}
