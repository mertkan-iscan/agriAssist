package io.mertkaniscan.automation_engine.controllers.api;

import io.mertkaniscan.automation_engine.services.python_module_services.PythonVenvService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/python-module")
public class PythonModuleController {

    private final PythonVenvService pythonVenvService;

    @Autowired
    public PythonModuleController(PythonVenvService pythonVenvService) {
        this.pythonVenvService = pythonVenvService;
    }

    @PostMapping("/start")
    public String startPythonScript() {
        try {
            // Stop the currently running Python script
            pythonVenvService.runPythonScript();

            return "Python script stopped successfully.";

        } catch (Exception e) {
            return "Error restarting Python script: " + e.getMessage();
        }
    }

    @PostMapping("/stop")
    public String stopPythonScript() {
        try {
            // Stop the currently running Python script
            pythonVenvService.stopPythonScript();

            return "Python script stopped successfully.";

        } catch (Exception e) {
            return "Error restarting Python script: " + e.getMessage();
        }
    }

    @PostMapping("/restart")
    public String restartPythonScript() {
        try {
            // Stop the currently running Python script
            pythonVenvService.stopPythonScript();
            // Wait a bit before restarting (optional)
            Thread.sleep(2000);

            // Start the Python script again
            pythonVenvService.runPythonScript();
            return "Python script restarted successfully.";

        } catch (Exception e) {
            return "Error restarting Python script: " + e.getMessage();
        }
    }
}