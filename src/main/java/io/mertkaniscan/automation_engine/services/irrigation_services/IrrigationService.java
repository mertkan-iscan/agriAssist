package io.mertkaniscan.automation_engine.services.irrigation_services;


import io.mertkaniscan.automation_engine.models.Day;
import io.mertkaniscan.automation_engine.models.Hour;
import io.mertkaniscan.automation_engine.models.Plant;
import io.mertkaniscan.automation_engine.repositories.DayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.mertkaniscan.automation_engine.models.Field;
import io.mertkaniscan.automation_engine.models.IrrigationRequest;
import io.mertkaniscan.automation_engine.models.Plant;
import io.mertkaniscan.automation_engine.repositories.IrrigationRepository;
import io.mertkaniscan.automation_engine.services.device_services.ActuatorCommandSocketService;
import io.mertkaniscan.automation_engine.services.main_services.FieldService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class IrrigationService {
    private final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    private final Map<Integer, List<ScheduledFuture<?>>> irrigationTasks = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> activeIrrigations = new ConcurrentHashMap<>();

    private final IrrigationRepository irrigationRepository;
    private final ActuatorCommandSocketService actuatorCommandSocketService;
    private final FieldService fieldService;
    private final DayRepository dayRepository;

    public IrrigationService(IrrigationRepository irrigationRepository,
                             ActuatorCommandSocketService actuatorCommandSocketService,
                             FieldService fieldService, DayRepository dayRepository) {
        this.irrigationRepository = irrigationRepository;
        this.actuatorCommandSocketService = actuatorCommandSocketService;
        this.fieldService = fieldService;
        this.dayRepository = dayRepository;
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
                    request.setIrrigationStartTime(now);
                    Duration remainingDuration = Duration.between(now, request.getIrrigationEndTime());
                    request.setIrrigationDuration((int) remainingDuration.toMinutes());
                    irrigationRepository.save(request);
                }

                // Check for overlapping irrigations before processing
                if (!hasOverlappingIrrigation(request)) {
                    processIrrigationRequest(request);
                } else {
                    request.setStatus(IrrigationRequest.IrrigationStatus.FAILED);
                    irrigationRepository.save(request);
                    System.err.println("Skipping overlapping irrigation request with ID: " + request.getId());
                }

            } catch (IllegalArgumentException e) {
                System.err.println("Invalid irrigation request with ID: " + request.getId() + " - " + e.getMessage());
                request.setStatus(IrrigationRequest.IrrigationStatus.FAILED);
                irrigationRepository.save(request);
            }
        }
    }

    private boolean hasOverlappingIrrigation(IrrigationRequest request) {
        List<IrrigationRequest> existingRequests = irrigationRepository.findByFieldFieldID(request.getField().getFieldID());
        return existingRequests.stream()
                .filter(r -> r.getId() != request.getId()) // Exclude the current request
                .filter(r -> r.getStatus() == IrrigationRequest.IrrigationStatus.PENDING
                        || r.getStatus() == IrrigationRequest.IrrigationStatus.IN_PROGRESS)
                .anyMatch(r -> {
                    LocalDateTime newStart = request.getIrrigationStartTime();
                    LocalDateTime newEnd = newStart.plusMinutes(request.getIrrigationDuration());
                    LocalDateTime existingStart = r.getIrrigationStartTime();
                    LocalDateTime existingEnd = r.getIrrigationEndTime();

                    return !(newEnd.isBefore(existingStart) || newStart.isAfter(existingEnd));
                });
    }

    public List<IrrigationRequest> getScheduledIrrigationsByField(int fieldID) {
        Field field = fieldService.getFieldById(fieldID);
        if (field == null) {
            return new ArrayList<>();
        }
        return irrigationRepository.findByFieldFieldID(fieldID);
    }

    public void processIrrigationRequest(IrrigationRequest request) {

        if (request.getIrrigationStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Irrigation start time must be in the future.");
        }

        int providedValues = 0;
        if (request.getFlowRate() != null && request.getFlowRate() > 0) providedValues++;
        if (request.getTotalWaterAmount() != null && request.getTotalWaterAmount() > 0) providedValues++;
        if (request.getIrrigationDuration() != null && request.getIrrigationDuration() > 0) providedValues++;

        if (providedValues != 2) {
            throw new IllegalArgumentException("Exactly two of flowRate, totalWaterAmount, or irrigationDuration must be provided.");
        }

        if (hasOverlappingIrrigation(request)) {
            throw new IllegalStateException("An irrigation is already scheduled for this time period.");
        }

        if (request.getFlowRate() != null && request.getTotalWaterAmount() != null) {

            double durationInHours = request.getTotalWaterAmount() / request.getFlowRate();
            request.setIrrigationDuration((int) Math.round(durationInHours * 60));

        } else if (request.getFlowRate() != null && request.getIrrigationDuration() != null) {

            double totalWaterAmount = request.getFlowRate() * (request.getIrrigationDuration() / 60.0);
            request.setTotalWaterAmount(totalWaterAmount);

        } else if (request.getTotalWaterAmount() != null && request.getIrrigationDuration() != null) {

            double flowRate = request.getTotalWaterAmount() / (request.getIrrigationDuration() / 60.0);
            request.setFlowRate(flowRate);

        }

        // Set end time and status
        LocalDateTime endTime = request.getIrrigationStartTime().plusMinutes(request.getIrrigationDuration());
        request.setIrrigationEndTime(endTime);
        request.setStatus(IrrigationRequest.IrrigationStatus.PENDING);

        irrigationRepository.save(request);
        scheduleIrrigationTask(request);
    }

    private void scheduleIrrigationTask(IrrigationRequest request) {
        LocalDateTime now = LocalDateTime.now();

        Duration startDelay = Duration.between(now, request.getIrrigationStartTime());
        Duration stopDelay = Duration.between(now, request.getIrrigationEndTime());

        List<ScheduledFuture<?>> tasks = new ArrayList<>();

        if (!startDelay.isNegative() && !startDelay.isZero()) {

            ScheduledFuture<?> startTask = taskScheduler.schedule(
                    () -> startIrrigation(request),
                    Instant.now().plusMillis(startDelay.toMillis())
            );

            tasks.add(startTask);
        }

        if (!stopDelay.isNegative() && !stopDelay.isZero()) {

            ScheduledFuture<?> stopTask = taskScheduler.schedule(
                    () -> stopIrrigation(request),
                    Instant.now().plusMillis(stopDelay.toMillis())
            );

            tasks.add(stopTask);
        }

        irrigationTasks.put(request.getId(), tasks);
    }

    public void startIrrigation(IrrigationRequest request) {
        try {
            // Check if there's already an active irrigation for this field
            if (activeIrrigations.getOrDefault(request.getField().getFieldID(), false)) {

                request.setStatus(IrrigationRequest.IrrigationStatus.FAILED);
                irrigationRepository.save(request);
                cancelScheduledTasks(request.getId());

                System.err.println("Failed to start irrigation: Field " + request.getField().getFieldID() + " is already being irrigated");

                return;
            }

            request.setStatus(IrrigationRequest.IrrigationStatus.IN_PROGRESS);
            irrigationRepository.save(request);

            int fieldId = request.getField().getFieldID();
            actuatorCommandSocketService.startIrrigation(fieldId, request.getFlowRate());
            activeIrrigations.put(fieldId, true);

            LocalDateTime irrigationStartTime = request.getIrrigationStartTime();
            LocalDateTime irrigationEndTime = request.getIrrigationEndTime();

            double irrigationWaterAmountLiter = request.getTotalWaterAmount();
            double infiltrationRateCmH = request.getField().getInfiltrationRate();
            double wetArea = irrigationWaterAmountLiter / (10 * infiltrationRateCmH);
            Plant plant = request.getField().getPlantInField();

            saveIrrigationforHour(plant, irrigationStartTime, irrigationEndTime, irrigationWaterAmountLiter, wetArea);

            System.out.println("Irrigation started for field ID: " + fieldId);

        } catch (Exception e) {
            System.err.println("Failed to start irrigation for field ID: " +
                    request.getField().getFieldID() + ". Error: " + e.getMessage());

            request.setStatus(IrrigationRequest.IrrigationStatus.FAILED);
            irrigationRepository.save(request);
            cancelScheduledTasks(request.getId());
        }
    }

    private void stopIrrigation(IrrigationRequest request) {
        try {
            // Don't stop irrigation if the request failed or was cancelled
            if (request.getStatus() == IrrigationRequest.IrrigationStatus.FAILED ||
                    request.getStatus() == IrrigationRequest.IrrigationStatus.CANCELLED) {
                return;
            }

            int fieldId = request.getField().getFieldID();
            actuatorCommandSocketService.stopIrrigation(fieldId);
            request.setStatus(IrrigationRequest.IrrigationStatus.COMPLETED);
            irrigationRepository.save(request);
            activeIrrigations.remove(fieldId);

            System.out.println("Irrigation stopped for field ID: " + fieldId);
        } catch (Exception e) {
            System.err.println("Failed to stop irrigation for field ID: " +
                    request.getField().getFieldID() + ". Error: " + e.getMessage());
            request.setStatus(IrrigationRequest.IrrigationStatus.FAILED);
            irrigationRepository.save(request);
        } finally {
            cancelScheduledTasks(request.getId());
        }
    }

    private void cancelScheduledTasks(int requestId) {
        List<ScheduledFuture<?>> tasks = irrigationTasks.get(requestId);
        if (tasks != null) {
            tasks.forEach(task -> {
                if (task != null && !task.isCancelled()) {
                    task.cancel(false);
                }
            });
            irrigationTasks.remove(requestId);
        }
    }

    public void cancelIrrigation(int requestId) {
        IrrigationRequest request = irrigationRepository.findById(requestId).orElse(null);
        if (request != null) {
            // If irrigation is in progress, stop it
            if (request.getStatus() == IrrigationRequest.IrrigationStatus.IN_PROGRESS) {
                try {
                    actuatorCommandSocketService.stopIrrigation(request.getField().getFieldID());
                    activeIrrigations.remove(request.getField().getFieldID());
                } catch (Exception e) {
                    System.err.println("Error stopping irrigation during cancellation: " + e.getMessage());
                }
            }

            // Cancel scheduled tasks
            cancelScheduledTasks(requestId);

            // Set request status to CANCELLED
            request.setStatus(IrrigationRequest.IrrigationStatus.CANCELLED);
            irrigationRepository.save(request);

            // Remove remaining hourly records
            removeRemainingHourlyRecords(request);
        }
    }

    private void removeRemainingHourlyRecords(IrrigationRequest request) {

        Field field = request.getField();
        Plant plant = field.getPlantInField();
        LocalDateTime irrigationStartTime = request.getIrrigationStartTime();
        LocalDateTime irrigationEndTime = request.getIrrigationEndTime();
        LocalDateTime now = LocalDateTime.now();

        Day day = dayRepository.findByPlant_PlantIDAndDateWithHours(
                plant.getPlantID(), Timestamp.valueOf(irrigationStartTime.toLocalDate().atStartOfDay())
        );

        if (day != null) {

            int startHourIndex = irrigationStartTime.getHour();
            int endHourIndex = Math.min(irrigationEndTime.getHour(), 23);

            for (int hourIndex = startHourIndex; hourIndex <= endHourIndex; hourIndex++) {

                int finalHourIndex = hourIndex;
                Hour hour = day.getHours().stream()
                        .filter(h -> h.getHourIndex() == finalHourIndex)
                        .findFirst()
                        .orElse(null);

                if (hour == null) continue;

                LocalDateTime hourStart = irrigationStartTime.toLocalDate().atStartOfDay().plusHours(hourIndex);
                LocalDateTime hourEnd = hourStart.plusHours(1);

                if (hourEnd.isBefore(now)) {
                    continue;
                }

                if (hourStart.isBefore(now) && hourEnd.isAfter(now)) {
                    LocalDateTime overlapStart = irrigationStartTime.isAfter(hourStart)
                            ? irrigationStartTime
                            : hourStart;
                    LocalDateTime overlapEnd = now;
                    long overlapSeconds = Duration.between(overlapStart, overlapEnd).getSeconds();


                    long totalIrrigationSeconds = Duration.between(irrigationStartTime, irrigationEndTime).getSeconds();
                    double totalWaterAmount = request.getTotalWaterAmount();
                    double totalWetArea = totalWaterAmount / (10 * field.getInfiltrationRate());

                    double recalculatedWaterAmount = (totalWaterAmount * overlapSeconds) / totalIrrigationSeconds;
                    double recalculatedWetArea = (totalWetArea * overlapSeconds) / totalIrrigationSeconds;

                    hour.setIrrigationAmount(recalculatedWaterAmount);
                    hour.setIrrigationWetArea(recalculatedWetArea);
                    hour.setLastUpdated(LocalDateTime.now());

                } else if (hourStart.isAfter(now)) {

                    hour.setIrrigationAmount(null);
                    hour.setIrrigationWetArea(null);
                    hour.setLastUpdated(LocalDateTime.now());
                }
            }

            dayRepository.save(day);
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

        // Cancel existing scheduled tasks before updating
        cancelIrrigation(id);

        // Update request properties
        existingRequest.setFlowRate(updatedRequest.getFlowRate());
        existingRequest.setTotalWaterAmount(updatedRequest.getTotalWaterAmount());
        existingRequest.setIrrigationDuration(updatedRequest.getIrrigationDuration());
        existingRequest.setIrrigationStartTime(updatedRequest.getIrrigationStartTime());

        // Process the updated request as a new request
        processIrrigationRequest(existingRequest);
    }


    @Transactional
    public void saveIrrigationforHour(Plant plant, LocalDateTime irrigationStartTime, LocalDateTime irrigationEndTime, double irrigationWaterAmountLiter, double wetAreaPerHour) {
        // Get the Day record for the plant and date
        Day day = dayRepository.findByPlant_PlantIDAndDateWithHours(plant.getPlantID(), Timestamp.valueOf(irrigationStartTime.toLocalDate().atStartOfDay()));

        if (day == null) {
            throw new IllegalArgumentException("Day record not found for the given plant and date.");
        }

        // Calculate total irrigation duration in minutes
        long totalDurationMinutes = Duration.between(irrigationStartTime, irrigationEndTime).toMinutes();

        double cumulativeWetArea = 0.0; // To accumulate wet area over hours

        // Find all intersecting hours
        for (Hour hour : day.getHours()) {
            LocalDateTime hourStart = irrigationStartTime.toLocalDate().atStartOfDay().plusHours(hour.getHourIndex());
            LocalDateTime hourEnd = hourStart.plusHours(1);

            if (hourEnd.isBefore(irrigationStartTime) || hourStart.isAfter(irrigationEndTime)) {
                continue; // Skip hours that don't intersect
            }

            // Calculate overlap duration in minutes
            LocalDateTime overlapStart = irrigationStartTime.isAfter(hourStart) ? irrigationStartTime : hourStart;
            LocalDateTime overlapEnd = irrigationEndTime.isBefore(hourEnd) ? irrigationEndTime : hourEnd;
            long overlapMinutes = Duration.between(overlapStart, overlapEnd).toMinutes();

            // Calculate water amount and wet area for the overlapping duration
            double overlapWaterAmount = (irrigationWaterAmountLiter * overlapMinutes) / totalDurationMinutes;
            double overlapWetArea = (wetAreaPerHour * overlapMinutes) / totalDurationMinutes;

            // Accumulate the wet area
            cumulativeWetArea += overlapWetArea;

            // Update the Hour record
            if (hour.getIrrigationAmount() == null) {
                hour.setIrrigationAmount(overlapWaterAmount);
            } else {
                hour.setIrrigationAmount(hour.getIrrigationAmount() + overlapWaterAmount);
            }

            // Set the cumulative wet area
            hour.setIrrigationWetArea(cumulativeWetArea);

            hour.setLastUpdated(LocalDateTime.now());
        }

        // Save the updated Day record
        dayRepository.save(day);
    }
}