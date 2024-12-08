package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private Double flowRate; // Liters per hour

    @Column(nullable = false)
    private Double totalWaterAmount; // Total water in liters

    @Column(name = "irrigation_start_time", nullable = false)
    @JsonProperty("irrigationStartTime")
    private LocalDateTime irrigationStartTime;

    @Column(name = "irrigation_duration", nullable = false)
    @JsonProperty("irrigationDuration")
    private Integer irrigationDuration; // Duration in minutes

    @Column(name = "irrigation_end_time", nullable = false)
    @JsonIgnore
    private LocalDateTime irrigationEndTime;


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

    public Double getFlowRate() {
        return flowRate;
    }

    public void setFlowRate(Double flowRate) {
        this.flowRate = flowRate;
    }

    public Double getTotalWaterAmount() {
        return totalWaterAmount;
    }

    public void setTotalWaterAmount(Double totalWaterAmount) {
        this.totalWaterAmount = totalWaterAmount;
    }

    public LocalDateTime getIrrigationStartTime() {
        return irrigationStartTime;
    }

    public void setIrrigationtStartTime(LocalDateTime irrigationtStartTime) {
        this.irrigationStartTime = irrigationtStartTime;
    }

    public Integer getIrrigationDuration() {
        return irrigationDuration;
    }

    public void setIrrigationDuration(Integer irrigationDuration) {
        this.irrigationDuration = irrigationDuration;
    }

    public LocalDateTime getIrrigationEndTime() {
        return irrigationEndTime;
    }

    public void setIrrigationEndTime(LocalDateTime irrigationEndTime) {
        this.irrigationEndTime = irrigationEndTime;
    }
}

