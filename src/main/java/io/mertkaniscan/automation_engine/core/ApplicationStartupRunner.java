package io.mertkaniscan.automation_engine.core;


import io.mertkaniscan.automation_engine.components.ScheduledSensorDataFetcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartupRunner implements ApplicationRunner {

    @Autowired
    ScheduledSensorDataFetcher scheduledSensorDataFetcher;


    @Override
    public void run(ApplicationArguments args) throws Exception {
        scheduledSensorDataFetcher.initializeDeviceTasks();
    }
}