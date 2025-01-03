package io.mertkaniscan.automation_engine.components.tasks;

import io.mertkaniscan.automation_engine.services.task_services.UpdateFieldCurrentTaskService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
public class FieldCurrentUpdaterTaskJob implements Job {

    private final UpdateFieldCurrentTaskService updateFieldCurrentTaskService;

    public FieldCurrentUpdaterTaskJob(UpdateFieldCurrentTaskService updateFieldCurrentTaskService) {
        this.updateFieldCurrentTaskService = updateFieldCurrentTaskService;
    }

    @Override
    public void execute(JobExecutionContext context) {
        updateFieldCurrentTaskService.updateFieldCurrentValues();
    }
}