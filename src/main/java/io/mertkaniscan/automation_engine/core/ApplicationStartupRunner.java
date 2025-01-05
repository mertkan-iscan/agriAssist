package io.mertkaniscan.automation_engine.core;


import io.mertkaniscan.automation_engine.components.ScheduledSensorDataFetcher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import org.springframework.stereotype.Component;

@Component
public class ApplicationStartupRunner implements ApplicationRunner {

    private final ScheduledSensorDataFetcher scheduledSensorDataFetcher;

    public ApplicationStartupRunner(ScheduledSensorDataFetcher scheduledSensorDataFetcher) {
        this.scheduledSensorDataFetcher = scheduledSensorDataFetcher;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        scheduledSensorDataFetcher.initializeDeviceTasks();
    }
}