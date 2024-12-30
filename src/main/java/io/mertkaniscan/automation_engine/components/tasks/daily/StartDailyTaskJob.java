package io.mertkaniscan.automation_engine.components.tasks.daily;

import io.mertkaniscan.automation_engine.services.task_services.DailyTaskService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
public class StartDailyTaskJob implements Job {

    private final DailyTaskService dailyTaskService;

    public StartDailyTaskJob(DailyTaskService dailyTaskService) {
        this.dailyTaskService = dailyTaskService;
    }

    @Override
    public void execute(JobExecutionContext context) {

    }
}