package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "irrigation_requests")
public class IrrigationRequest {

    public enum IrrigationStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @JsonBackReference("field-irrigationRequest")
    @ManyToOne
    @JoinColumn(name = "fieldid", nullable = false) // foreign key in irrigation_requests table
    private Field field;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IrrigationStatus status;

    @Column(nullable = false)
    private double flowRate; // Liters per hour

    @Column(nullable = false)
    private double totalWaterAmount; // Total water in liters

    @Column(name = "irrigation_time", nullable = false)
    @JsonProperty("irrigationTime") // Maps "irrigationTime" JSON key to this field
    private LocalDateTime irrigationTime; // Irrigation start time

    @Column(name = "irrigation_duration", nullable = false)
    @JsonProperty("irrigationDuration")
    private int irrigationDuration; // Duration in minutes

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public IrrigationStatus getStatus() {
        return status;
    }

    public void setStatus(IrrigationStatus status) {
        this.status = status;
    }

    public double getFlowRate() {
        return flowRate;
    }

    public void setFlowRate(double flowRate) {
        this.flowRate = flowRate;
    }

    public double getTotalWaterAmount() {
        return totalWaterAmount;
    }

    public void setTotalWaterAmount(double totalWaterAmount) {
        this.totalWaterAmount = totalWaterAmount;
    }

    public LocalDateTime getStartTime() {
        return irrigationTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.irrigationTime = startTime;
    }

    public int getDuration() {
        return irrigationDuration;
    }

    public void setDuration(int duration) {
        this.irrigationDuration = duration;
    }
}
