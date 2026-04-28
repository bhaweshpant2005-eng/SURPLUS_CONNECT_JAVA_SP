package com.example.surplusconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Time-Slot entity for Interval Scheduling (Feature 8).
 * 
 * Uses Greedy Interval Scheduling Algorithm:
 * - Sort tasks by finish time
 * - Select non-overlapping intervals to maximize deliveries
 */
@Entity
public class TimeSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long allocationId;
    private Long ngoId;
    private String vehicleId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String taskType;  // "PICKUP" or "DELIVERY"
    private String status;    // "SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED"

    public TimeSlot() {}

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAllocationId() { return allocationId; }
    public void setAllocationId(Long allocationId) { this.allocationId = allocationId; }

    public Long getNgoId() { return ngoId; }
    public void setNgoId(Long ngoId) { this.ngoId = ngoId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
