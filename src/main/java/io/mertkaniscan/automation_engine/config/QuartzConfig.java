package io.mertkaniscan.automation_engine.config;

import io.mertkaniscan.automation_engine.components.DailyTaskJob;
import io.mertkaniscan.automation_engine.components.HourlyTaskJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail dailyTaskJobDetail() {
        return JobBuilder.newJob(DailyTaskJob.class)
                .withIdentity("dailyTaskJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger dailyTaskTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(dailyTaskJobDetail())
                .withIdentity("dailyTaskTrigger")
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(0, 0))
                .build();
    }

    @Bean
    public JobDetail hourlyTaskJobDetail() {
        return JobBuilder.newJob(HourlyTaskJob.class)
                .withIdentity("hourlyTaskJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger hourlyTaskTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(hourlyTaskJobDetail())
                .withIdentity("hourlyTaskTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?"))
                .build();
    }
}