package io.mertkaniscan.automation_engine.components.tasks.hourly;

import io.mertkaniscan.automation_engine.services.task_services.UpdateHourlyRecordTaskService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
public class HourlyTaskJob implements Job {

    private final UpdateHourlyRecordTaskService updateHourlyRecordTaskService;

    public HourlyTaskJob(UpdateHourlyRecordTaskService updateHourlyRecordTaskService) {
        this.updateHourlyRecordTaskService = updateHourlyRecordTaskService;
    }

    @Override
    public void execute(JobExecutionContext context) {
        updateHourlyRecordTaskService.updateHourlyRecords();
    }
}