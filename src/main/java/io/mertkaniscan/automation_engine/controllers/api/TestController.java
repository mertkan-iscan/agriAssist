package io.mertkaniscan.automation_engine.controllers.api;

import io.mertkaniscan.automation_engine.services.task_services.DailyTaskService;
import io.mertkaniscan.automation_engine.services.task_services.HourlyTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private DailyTaskService dailyTaskService;

    @Autowired
    private HourlyTaskService hourlyTaskService;

    @GetMapping("/triggerDailyTask")
    public ResponseEntity<String> triggerDailyTask() {
        dailyTaskService.createDailyRecords();
        return ResponseEntity.ok("Daily task triggered successfully.");
    }

    @GetMapping("/triggerHourlyTask")
    public ResponseEntity<String> triggerHourlyTask() {
        //hourlyTaskService.recordHourlyData();
        return ResponseEntity.ok("Hourly task triggered successfully.");
    }
}