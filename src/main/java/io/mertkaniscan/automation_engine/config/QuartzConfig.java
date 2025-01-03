package io.mertkaniscan.automation_engine.config;

import io.mertkaniscan.automation_engine.components.tasks.FieldCurrentUpdaterTaskJob;
import io.mertkaniscan.automation_engine.components.tasks.DailyTaskJob;
import io.mertkaniscan.automation_engine.components.tasks.HourlyTaskJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail dailyDataUpdaterTaskJobDetail() {
        return JobBuilder.newJob(DailyTaskJob.class)
                .withIdentity("dailyDataUpdaterTaskJobDetail")
                .storeDurably()
                .build();
    }
    @Bean
    public Trigger dailyDataUpdaterTaskTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(dailyDataUpdaterTaskJobDetail())
                .withIdentity("dailyDataUpdaterTaskTrigger")
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(0, 0))
                .build();
    }


    @Bean
    public JobDetail hourlyDataUpdaterTaskJobDetail() {
        return JobBuilder.newJob(HourlyTaskJob.class)
                .withIdentity("hourlyDataUpdaterTaskJobDetail")
                .storeDurably()
                .build();
    }
    @Bean
    public Trigger hourlyDataUpdaterTaskTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(hourlyDataUpdaterTaskJobDetail())
                .withIdentity("hourlyDataUpdaterTaskTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?"))
                .build();
    }



    @Bean
    public JobDetail fieldCurrentVariableUpdaterTaskJobDetail() {
        return JobBuilder.newJob(FieldCurrentUpdaterTaskJob.class)
                .withIdentity("fieldCurrentVariableUpdaterTaskJobDetail")
                .storeDurably()
                .build();
    }
    @Bean
    public Trigger fieldCurrentVariableUpdaterTaskTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(fieldCurrentVariableUpdaterTaskJobDetail())
                .withIdentity("fieldCurrentVariableUpdaterTaskTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ?"))
                .build();
    }
}