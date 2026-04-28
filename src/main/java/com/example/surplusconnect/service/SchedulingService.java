package com.example.surplusconnect.service;

import com.example.surplusconnect.model.*;
import com.example.surplusconnect.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Scheduling Service implementing:
 * 
 * Feature 8: Time-Slot Based Allocation (Interval Scheduling Algorithm)
 * Feature 9: Backup Matching Pool
 * 
 * Uses Greedy Interval Scheduling:
 * - Sort tasks by finish time (endTime)
 * - Greedily select non-overlapping intervals
 * - Maximizes the number of deliveries per vehicle
 * 
 * Time complexity: O(N log N) for sorting + O(N) for selection
 */
@Service
public class SchedulingService {

    private final TimeSlotRepository timeSlotRepo;
    private final AllocationRepository allocationRepo;
    private final NGORepository ngoRepo;
    private final ResourceRequestRepository requestRepo;

    public SchedulingService(TimeSlotRepository timeSlotRepo,
                             AllocationRepository allocationRepo,
                             NGORepository ngoRepo,
                             ResourceRequestRepository requestRepo) {
        this.timeSlotRepo = timeSlotRepo;
        this.allocationRepo = allocationRepo;
        this.ngoRepo = ngoRepo;
        this.requestRepo = requestRepo;
    }

    /**
     * Feature 8: Greedy Interval Scheduling Algorithm.
     * 
     * Given a set of proposed time slots for a vehicle, selects the
     * maximum number of non-overlapping tasks.
     * 
     * Algorithm:
     * 1. Sort all candidate tasks by end time (ascending)
     * 2. Select the first task
     * 3. For each remaining task, select it if its start >= last selected end
     * 
     * This is a classic greedy algorithm proven to be optimal for
     * maximizing the number of non-overlapping intervals.
     * 
     * @param vehicleId The vehicle to schedule for
     * @param candidates List of proposed time slots
     * @return List of selected, non-overlapping time slots
     */
    public List<TimeSlot> scheduleOptimal(String vehicleId, List<TimeSlot> candidates) {
        if (candidates.isEmpty()) return Collections.emptyList();

        // Step 1: Sort by end time (ascending) - key step of greedy algorithm
        candidates.sort(Comparator.comparing(TimeSlot::getEndTime));

        List<TimeSlot> selected = new ArrayList<>();
        LocalDateTime lastEnd = LocalDateTime.MIN;

        // Step 2 & 3: Greedy selection
        for (TimeSlot slot : candidates) {
            if (slot.getStartTime().isAfter(lastEnd) || slot.getStartTime().isEqual(lastEnd)) {
                slot.setVehicleId(vehicleId);
                slot.setStatus("SCHEDULED");
                selected.add(slot);
                lastEnd = slot.getEndTime();
            }
        }

        timeSlotRepo.saveAll(selected);
        return selected;
    }

    /**
     * Creates a time slot for an allocation.
     */
    public TimeSlot createTimeSlot(Long allocationId, Long ngoId,
                                    LocalDateTime start, LocalDateTime end,
                                    String taskType) {
        TimeSlot slot = new TimeSlot();
        slot.setAllocationId(allocationId);
        slot.setNgoId(ngoId);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setTaskType(taskType);
        slot.setStatus("PENDING");
        return timeSlotRepo.save(slot);
    }

    /**
     * Gets all scheduled slots for a vehicle, sorted by time.
     */
    public List<TimeSlot> getVehicleSchedule(String vehicleId) {
        return timeSlotRepo.findByVehicleIdOrderByStartTimeAsc(vehicleId);
    }

    // ===========================
    // Feature 9: BACKUP MATCHING POOL
    // ===========================

    /**
     * Feature 9: Maintains secondary NGO options for failover.
     * 
     * When a primary allocation fails (NGO refuses, logistics fail),
     * this method retrieves backup candidates from the request's
     * backupNgoIds field and attempts reassignment.
     * 
     * Data Structure: Circular queue of backup IDs
     * 
     * @param requestId The request whose primary allocation failed
     * @return New allocation if backup found, null otherwise
     */
    public Allocation tryBackupAllocation(Long requestId) {
        ResourceRequest request = requestRepo.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));

        if (request.getBackupNgoIds() == null || request.getBackupNgoIds().isEmpty()) {
            return null;
        }

        // Parse backup IDs (comma-separated)
        List<Long> backupIds = Arrays.stream(request.getBackupNgoIds().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(Long::parseLong)
            .collect(Collectors.toList());

        // Try each backup in order (circular queue behavior)
        for (Long backupNgoId : backupIds) {
            Optional<NGO> ngoOpt = ngoRepo.findById(backupNgoId);
            if (ngoOpt.isPresent()) {
                NGO ngo = ngoOpt.get();
                if (ngo.getAvailableCapacity() >= request.getQuantityRequested()) {
                    // Create new allocation
                    Allocation alloc = new Allocation();
                    alloc.setNgoId(backupNgoId);
                    alloc.setRequestId(requestId);
                    alloc.setAllocatedQuantity(request.getQuantityRequested());
                    alloc.setCurrentState(LifecycleState.MATCHED);
                    alloc.setPreviousNgoLoad(ngo.getCurrentLoad());

                    // Update NGO
                    ngo.setCurrentLoad(ngo.getCurrentLoad() + request.getQuantityRequested());
                    ngoRepo.save(ngo);

                    // Update request
                    request.setStatus("Matched (Backup)");
                    requestRepo.save(request);

                    return allocationRepo.save(alloc);
                }
            }
        }

        return null; // No backup available
    }

    /**
     * Sets backup NGOs for a request based on proximity and compatibility.
     */
    public void assignBackupPool(Long requestId, List<Long> backupNgoIds) {
        ResourceRequest request = requestRepo.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));

        String backupStr = backupNgoIds.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));

        request.setBackupNgoIds(backupStr);
        requestRepo.save(request);
    }
}
