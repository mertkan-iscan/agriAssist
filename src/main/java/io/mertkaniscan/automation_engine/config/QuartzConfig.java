package io.mertkaniscan.automation_engine.config;

import io.mertkaniscan.automation_engine.components.tasks.daily.DailyTaskJob;
import io.mertkaniscan.automation_engine.components.tasks.daily.EndDailyTaskJob;
import io.mertkaniscan.automation_engine.components.tasks.daily.StartDailyTaskJob;
import io.mertkaniscan.automation_engine.components.tasks.hourly.EndHourlyTaskJob;
import io.mertkaniscan.automation_engine.components.tasks.hourly.HourlyTaskJob;
import io.mertkaniscan.automation_engine.components.tasks.hourly.StartHourlyTaskJob;
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
                .withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ?"))
                .build();
    }

    @Bean
    public JobDetail startOfDayTaskJobDetail() {
        return JobBuilder.newJob(StartDailyTaskJob.class)
                .withIdentity("startOfDayTaskJobDetail")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger startOfDayTaskTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(startOfDayTaskJobDetail())
                .withIdentity("startOfDayTaskTrigger")
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(0, 0)) // Gün başlangıcı (00:00)
                .build();
    }

    @Bean
    public JobDetail endOfDayTaskJobDetail() {
        return JobBuilder.newJob(EndDailyTaskJob.class)
                .withIdentity("endOfDayTaskJobDetail")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger endOfDayTaskTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(endOfDayTaskJobDetail())
                .withIdentity("endOfDayTaskTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("59 59 23 * * ?")) // Gün sonu (23:59:59)
                .build();
    }

    @Bean
    public JobDetail startOfHourTaskJobDetail() {
        return JobBuilder.newJob(StartHourlyTaskJob.class)
                .withIdentity("startOfHourTaskJobDetail")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger startOfHourTaskTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(startOfHourTaskJobDetail())
                .withIdentity("startOfHourTaskTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?")) // Saatin başı (hh:00:00)
                .build();
    }

    @Bean
    public JobDetail endOfHourTaskJobDetail() {
        return JobBuilder.newJob(EndHourlyTaskJob.class)
                .withIdentity("endOfHourTaskJobDetail")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger endOfHourTaskTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(endOfHourTaskJobDetail())
                .withIdentity("endOfHourTaskTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("59 59 * * * ?")) // Saatin sonu (hh:59:59)
                .build();
    }
}