package io.mertkaniscan.automation_engine.services.irrigation_services;

import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.models.IrrigationRequest;
import io.mertkaniscan.automation_engine.repositories.IrrigationRepository;
import io.mertkaniscan.automation_engine.services.device_services.ActuatorCommandSocketService;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Service
public class IrrigationService {

    private final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    private final Map<Integer, ScheduledFuture<?>> irrigationTasks = new ConcurrentHashMap<>();

    private final IrrigationRepository irrigationRepository;
    private final ActuatorCommandSocketService actuatorCommandSocketService;
    private final FieldService fieldService;

    public IrrigationService(IrrigationRepository irrigationRepository, ActuatorCommandSocketService actuatorCommandSocketService, FieldService fieldService) {
        this.irrigationRepository = irrigationRepository;
        this.actuatorCommandSocketService = actuatorCommandSocketService;
        this.fieldService = fieldService;
    }

    @PostConstruct
    public void init() {
        taskScheduler.setPoolSize(10);
        taskScheduler.initialize();
        initializePendingTasks();
    }

    private void initializePendingTasks() {
        List<IrrigationRequest> pendingRequests = irrigationRepository.findByStatus(IrrigationRequest.IrrigationStatus.PENDING);

        for (IrrigationRequest request : pendingRequests) {
            try {
                LocalDateTime now = LocalDateTime.now();

                // Skip requests with an end time older than now
                if (request.getIrrigationEndTime().isBefore(now)) {
                    System.err.println("Skipping expired irrigation request with ID: " + request.getId());
                    request.setStatus(IrrigationRequest.IrrigationStatus.FAILED);
                    irrigationRepository.save(request);
                    continue;
                }

                // Adjust requests with a start time in the past
                if (request.getIrrigationStartTime().isBefore(now)) {
                    System.out.println("Adjusting irrigation request with ID: " + request.getId() + " to start now.");
                    request.setIrrigationtStartTime(now);
                    Duration remainingDuration = Duration.between(now, request.getIrrigationEndTime());
                    request.setIrrigationDuration((int) remainingDuration.toMinutes());
                    irrigationRepository.save(request);
                }

                // Start irrigation immediately
                processIrrigationRequest(request);

            } catch (IllegalArgumentException e) {
                // Log and mark invalid requests as failed
                System.err.println("Invalid irrigation request with ID: " + request.getId() + " - " + e.getMessage());
                request.setStatus(IrrigationRequest.IrrigationStatus.FAILED);
                irrigationRepository.save(request);
            }
        }
    }


    public List<IrrigationRequest> getScheduledIrrigationsByField(int fieldID) {
        // Retrieve the field by its ID
        Field field = fieldService.getFieldById(fieldID);

        if (field == null) {
            // Return an empty list if the field does not exist
            return new ArrayList<>();
        }

        // Retrieve all irrigation requests for the given field without filtering
        return irrigationRepository.findByFieldFieldID(fieldID);
    }

    public void processIrrigationRequest(IrrigationRequest request) {

        System.out.println("FlowRate: " + request.getFlowRate());
        System.out.println("TotalWaterAmount: " + request.getTotalWaterAmount());
        System.out.println("IrrigationDuration: " + request.getIrrigationDuration());

        // Validate irrigation start time
        if (request.getIrrigationStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Irrigation start time must be in the future.");
        }

        // Validate: Ensure exactly two non-null and positive values are provided
        int providedValues = 0;
        if (request.getFlowRate() != null && request.getFlowRate() > 0) providedValues++;
        if (request.getTotalWaterAmount() != null && request.getTotalWaterAmount() > 0) providedValues++;
        if (request.getIrrigationDuration() != null && request.getIrrigationDuration() > 0) providedValues++;

        System.err.println("Provided values: " + providedValues);


        if (providedValues != 2) {
            throw new IllegalArgumentException("Exactly two of flowRate, totalWaterAmount, or irrigationDuration must be provided.");
        }

        // Calculate missing value
        if (request.getFlowRate() != null && request.getFlowRate() > 0 &&
                request.getTotalWaterAmount() != null && request.getTotalWaterAmount() > 0) {

            double durationInHours = request.getTotalWaterAmount() / request.getFlowRate();
            int durationInMinutes = (int) Math.round(durationInHours * 60);
            request.setIrrigationDuration(durationInMinutes);

        } else if (request.getFlowRate() != null && request.getFlowRate() > 0 &&
                request.getIrrigationDuration() != null && request.getIrrigationDuration() > 0) {

            double totalWaterAmount = request.getFlowRate() * (request.getIrrigationDuration() / 60.0);
            request.setTotalWaterAmount(totalWaterAmount);

        } else if (request.getTotalWaterAmount() != null && request.getTotalWaterAmount() > 0 &&
                request.getIrrigationDuration() != null && request.getIrrigationDuration() > 0) {

            double flowRate = request.getTotalWaterAmount() / (request.getIrrigationDuration() / 60.0);
            request.setFlowRate(flowRate);
        }

        // Calculate and set end time
        LocalDateTime endTime = request.getIrrigationStartTime().plusMinutes(request.getIrrigationDuration());

        request.setStatus(IrrigationRequest.IrrigationStatus.PENDING);
        request.setIrrigationEndTime(endTime);

        irrigationRepository.save(request);
        scheduleIrrigationTask(request);
    }



    private void scheduleIrrigationTask(IrrigationRequest request) {

        LocalDateTime now = LocalDateTime.now();

        Duration startDelay = Duration.between(now, request.getIrrigationStartTime());
        Duration stopDelay = Duration.between(now, request.getIrrigationEndTime());

        if (!startDelay.isNegative() && !startDelay.isZero()) {

            ScheduledFuture<?> startTask = taskScheduler.schedule(
                    () -> startIrrigation(request),
                    Instant.now().plusMillis(startDelay.toMillis())
            );

            irrigationTasks.put(request.getId(), startTask);
        }

        if (!stopDelay.isNegative() && !stopDelay.isZero()) {

            taskScheduler.schedule(

                    () -> stopIrrigation(request),
                    Instant.now().plusMillis(stopDelay.toMillis())
            );
        }
    }

    public void startIrrigation(IrrigationRequest request) {
        try {
            request.setStatus(IrrigationRequest.IrrigationStatus.IN_PROGRESS);
            irrigationRepository.save(request);

            int fieldId = request.getField().getFieldID();
            double flowRate = request.getFlowRate();

            actuatorCommandSocketService.startIrrigation(fieldId, flowRate);

            System.out.println("Irrigation started for field ID: " + fieldId);
        } catch (Exception e) {
            request.setStatus(IrrigationRequest.IrrigationStatus.FAILED);
            irrigationRepository.save(request);
            System.err.println("Failed to start irrigation: " + e.getMessage());
        }
    }

    private void stopIrrigation(IrrigationRequest request) {
        try {
            actuatorCommandSocketService.stopIrrigation(request.getField().getFieldID());
            request.setStatus(IrrigationRequest.IrrigationStatus.COMPLETED);
            irrigationRepository.save(request);

            System.out.println("Irrigation stopped for field ID: " + request.getField().getFieldID());
        } catch (Exception e) {
            System.err.println("Failed to stop irrigation: " + e.getMessage());
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
        IrrigationRequest existingRequest = irrigationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Irrigation request with ID " + id + " not found."));

        existingRequest.setFlowRate(updatedRequest.getFlowRate());
        existingRequest.setTotalWaterAmount(updatedRequest.getTotalWaterAmount());
        existingRequest.setIrrigationDuration(updatedRequest.getIrrigationDuration());
        existingRequest.setIrrigationtStartTime(updatedRequest.getIrrigationStartTime());
        existingRequest.setStatus(updatedRequest.getStatus());

        if (updatedRequest.getTotalWaterAmount() > 0 && updatedRequest.getFlowRate() > 0) {
            double durationInHours = updatedRequest.getTotalWaterAmount() / updatedRequest.getFlowRate();
            int durationInMinutes = (int) Math.round(durationInHours * 60);
            existingRequest.setIrrigationDuration(durationInMinutes);
        }

        irrigationRepository.save(existingRequest);

        if (irrigationTasks.containsKey(id)) {
            irrigationTasks.get(id).cancel(false);
            irrigationTasks.remove(id);
        }

        processIrrigationRequest(existingRequest);
    }


}
