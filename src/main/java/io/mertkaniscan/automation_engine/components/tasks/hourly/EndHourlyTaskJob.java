package io.mertkaniscan.automation_engine.components.tasks.hourly;

import io.mertkaniscan.automation_engine.services.task_services.HourlyTaskService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
public class EndHourlyTaskJob implements Job {

    private final HourlyTaskService hourlyTaskService;

    public EndHourlyTaskJob(HourlyTaskService hourlyTaskService) {
        this.hourlyTaskService = hourlyTaskService;
    }

    @Override
    public void execute(JobExecutionContext context) {

    }
}