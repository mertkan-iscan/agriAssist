package io.mertkaniscan.automation_engine.components;

import io.mertkaniscan.automation_engine.services.logic.DepletionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DepletionScheduler {

    private final DepletionService depletionService;

    public DepletionScheduler(DepletionService depletionService) {
        this.depletionService = depletionService;
    }

    // Scheduled to run every hour
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void scheduleDepletionCalculation() {
        depletionService.calculateDepletion();
    }
}