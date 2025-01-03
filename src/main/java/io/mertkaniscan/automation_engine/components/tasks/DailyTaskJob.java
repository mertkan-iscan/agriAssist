package io.mertkaniscan.automation_engine.components.tasks;

import io.mertkaniscan.automation_engine.services.task_services.DailyTaskService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
public class DailyTaskJob implements Job {

    private final DailyTaskService dailyTaskService;

    public DailyTaskJob(DailyTaskService dailyTaskService) {
        this.dailyTaskService = dailyTaskService;
    }

    @Override
    public void execute(JobExecutionContext context) {
        dailyTaskService.createDailyRecords();
    }
}