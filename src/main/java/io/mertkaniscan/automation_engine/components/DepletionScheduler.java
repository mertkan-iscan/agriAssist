package io.mertkaniscan.automation_engine.components;

import io.mertkaniscan.automation_engine.services.logic.CalculatorService;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;

@Component
public class DepletionScheduler {

    private final CalculatorService calculatorService;
    private final ThreadPoolTaskScheduler taskScheduler;
    private ScheduledFuture<?> scheduledTask;

    private long interval = 3600000; // Default interval: 1 hour in milliseconds

    public DepletionScheduler(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.initialize();
    }

    @PostConstruct
    public void startScheduler() {
        scheduleTask();
    }

    public void updateInterval(long newInterval) {
        this.interval = newInterval;

        // Cancel the existing task and schedule a new one
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        scheduleTask();
    }

    private void scheduleTask() {
        this.scheduledTask = taskScheduler.scheduleAtFixedRate(() -> {
            // Example input values; replace with actual data from your sources
            double evapotranspiration = 5.0;
            double rainfall = 3.0;

            // Perform depletion calculation
            calculatorService.calculateDepletion(evapotranspiration, rainfall);
        }, interval);
    }
}
