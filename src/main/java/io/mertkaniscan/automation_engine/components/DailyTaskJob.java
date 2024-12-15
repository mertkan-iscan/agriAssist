package io.mertkaniscan.automation_engine.components;

import io.mertkaniscan.automation_engine.services.task_services.DailyTaskService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DailyTaskJob implements Job {

    @Autowired
    private DailyTaskService dailyTaskService;

    @Override
    public void execute(JobExecutionContext context) {
        dailyTaskService.createDailyRecords();
    }
}
