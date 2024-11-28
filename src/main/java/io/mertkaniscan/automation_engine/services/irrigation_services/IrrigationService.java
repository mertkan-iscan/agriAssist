package io.mertkaniscan.automation_engine.services.irrigation_services;

import io.mertkaniscan.automation_engine.models.IrrigationRequest;
import io.mertkaniscan.automation_engine.repositories.IrrigationRepository;
import io.mertkaniscan.automation_engine.services.device_services.ActuatorCommandSocketService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class IrrigationService {

    private final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

    @Autowired
    private IrrigationRepository irrigationRepository;

    @Autowired
    private ActuatorCommandSocketService actuatorCommandSocketService;

    private final Map<Integer, ScheduledFuture<?>> irrigationTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        taskScheduler.setPoolSize(10);
        taskScheduler.initialize();
        initializePendingTasks();
    }

    private void initializePendingTasks() {
        List<IrrigationRequest> pendingRequests = irrigationRepository.findByStatus(IrrigationRequest.IrrigationStatus.PENDING);
        for (IrrigationRequest request : pendingRequests) {
            scheduleIrrigationTask(request);
        }
    }

    public void scheduleIrrigation(IrrigationRequest request) {
        if (request.getTotalWaterAmount() > 0) {
            double durationInHours = request.getTotalWaterAmount() / request.getFlowRate();
            int durationInMinutes = (int) Math.round(durationInHours * 60);
            request.setDuration(durationInMinutes);
        }

        request.setStatus(IrrigationRequest.IrrigationStatus.PENDING);
        irrigationRepository.save(request);

        scheduleIrrigationTask(request);
    }

    private void scheduleIrrigationTask(IrrigationRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Duration delay = Duration.between(now, request.getStartTime());

        if (!delay.isNegative() && !delay.isZero()) {
            ScheduledFuture<?> task = taskScheduler.schedule(() -> startIrrigation(request), Instant.ofEpochSecond(delay.toMillis()));
            irrigationTasks.put(request.getId(), task);
        } else {
            startIrrigation(request);
        }
    }

    public void startIrrigation(IrrigationRequest request) {
        try {
            int duration = request.getDuration();
            int fieldId = request.getField().getFieldID();
            double flowRate = request.getFlowRate();

            actuatorCommandSocketService.startIrrigation(fieldId, flowRate, duration);

            request.setStatus(IrrigationRequest.IrrigationStatus.COMPLETED);
            irrigationRepository.save(request);

            irrigationTasks.remove(request.getId());
            System.out.println("Irrigation started for field " + fieldId);
        } catch (Exception e) {
            request.setStatus(IrrigationRequest.IrrigationStatus.FAILED);
            irrigationRepository.save(request);

            irrigationTasks.remove(request.getId());
            System.err.println("Failed to start irrigation: " + e.getMessage());
        }
    }

    public void cancelIrrigation(int requestId) {
        ScheduledFuture<?> task = irrigationTasks.get(requestId);
        if (task != null) {
            task.cancel(false);
            irrigationTasks.remove(requestId);

            IrrigationRequest request = irrigationRepository.findById(requestId).orElse(null);
            if (request != null) {
                request.setStatus(IrrigationRequest.IrrigationStatus.CANCELLED);
                irrigationRepository.save(request);
            }
        }
    }

    public List<IrrigationRequest> getIrrigationHistory(int fieldId) {
        return irrigationRepository.findByFieldFieldID(fieldId);
    }

    public IrrigationRequest getIrrigationRequestById(int id) {
        return irrigationRepository.findById(id).orElse(null);
    }

    public void editIrrigationRequest(int id, IrrigationRequest updatedRequest) {
        // Fetch the existing request from the database
        IrrigationRequest existingRequest = irrigationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Irrigation request with ID " + id + " not found."));

        // Update the fields of the existing request with the new values
        existingRequest.setFlowRate(updatedRequest.getFlowRate());
        existingRequest.setTotalWaterAmount(updatedRequest.getTotalWaterAmount());
        existingRequest.setDuration(updatedRequest.getDuration());
        existingRequest.setStartTime(updatedRequest.getStartTime());
        existingRequest.setStatus(updatedRequest.getStatus());

        // Recalculate the duration if totalWaterAmount or flowRate is provided
        if (updatedRequest.getTotalWaterAmount() > 0) {
            double durationInHours = updatedRequest.getTotalWaterAmount() / updatedRequest.getFlowRate();
            int durationInMinutes = (int) Math.round(durationInHours * 60);
            existingRequest.setDuration(durationInMinutes);
        }

        // Save the updated request
        irrigationRepository.save(existingRequest);

        // Cancel the existing task if it exists
        if (irrigationTasks.containsKey(id)) {
            irrigationTasks.get(id).cancel(false);
            irrigationTasks.remove(id);
        }

        // Reschedule the task with the updated details
        scheduleIrrigationTask(existingRequest);
    }

}
