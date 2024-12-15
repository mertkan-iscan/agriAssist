package io.mertkaniscan.automation_engine.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
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
}

