package io.mertkaniscan.automation_engine.components;

import io.mertkaniscan.automation_engine.services.task_services.HourlyTaskService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HourlyTaskJob implements Job {

    @Autowired
    private HourlyTaskService hourlyTaskService;

    @Override
    public void execute(JobExecutionContext context) {
        hourlyTaskService.recordHourlyData();
    }
}