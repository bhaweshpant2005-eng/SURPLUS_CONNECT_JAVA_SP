package com.example.surplusconnect.controller;

import com.example.surplusconnect.model.*;
import com.example.surplusconnect.service.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

import com.example.surplusconnect.service.ItemService;

/**
 * Matching Controller exposing all algorithmic features as REST APIs:
 * 
 * Feature 1:  POST /api/matching/split/{itemId}        - Smart Resource Splitting
 * Feature 3:  POST /api/matching/reprioritize           - Dynamic Re-Prioritization
 * Feature 4:  POST /api/matching/multi-hop              - Multi-Hop Transfer
 * Feature 7:  GET  /api/matching/compatibility-matrix   - Compatibility Matrix
 * Feature 8:  POST /api/matching/schedule/{vehicleId}   - Time-Slot Scheduling
 * Feature 9:  POST /api/matching/backup/{requestId}     - Backup Matching
 * Feature 10: GET  /api/matching/queues                 - Multi-Level Queues
 * Feature 11: GET  /api/matching/recommendations        - Donation Recommendations
 * Feature 13: POST /api/matching/rollback/{allocationId}- Transaction Rollback
 * Feature 16: PUT  /api/matching/lifecycle/{itemId}     - Lifecycle Transition
 * Feature 18: POST /api/matching/emergency/activate     - Emergency Mode
 * Feature 19: GET  /api/matching/dependency-groups       - Dependency Groups
 * Feature 20: GET  /api/matching/thresholds             - Adaptive Thresholds
 */
@RestController
@RequestMapping("/api/matching")
@CrossOrigin
public class MatchingController {

    private final MatchingService matchingService;
    private final GraphAllocationService graphService;
    private final LifecycleManager lifecycleManager;
    private final SchedulingService schedulingService;
    private final ItemService itemService;

    public MatchingController(MatchingService matchingService,
                              GraphAllocationService graphService,
                              LifecycleManager lifecycleManager,
                              SchedulingService schedulingService,
                              ItemService itemService) {
        this.matchingService = matchingService;
        this.graphService = graphService;
        this.lifecycleManager = lifecycleManager;
        this.schedulingService = schedulingService;
        this.itemService = itemService;
    }

    // ===========================
    // Feature 1: Smart Splitting
    // ===========================

    /**
     * Feature 1: Splits a donation across multiple NGOs using the Greedy algorithm.
     * Fetches the actual item from DB, then delegates to MatchingService.
     */
    @PostMapping("/split/{itemId}")
    public List<Allocation> splitAndAllocate(@PathVariable Long itemId) {
        Item item = itemService.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));
        return matchingService.splitAndAllocate(item);
    }

    // ===========================
    // Feature 3: Re-Prioritization
    // ===========================

    @PostMapping("/reprioritize")
    public String reprioritize() {
        matchingService.recalculatePriorities();
        return "All pending request priorities recalculated using aging algorithm.";
    }

    // ===========================
    // Feature 4: Multi-Hop Allocation
    // ===========================

    /**
     * Adds a transfer link between two NGOs in the graph.
     */
    @PostMapping("/graph/link")
    public String addTransferLink(@RequestParam Long ngoId1, @RequestParam Long ngoId2) {
        graphService.addTransferLink(ngoId1, ngoId2);
        return "Transfer link added: NGO-" + ngoId1 + " <-> NGO-" + ngoId2;
    }

    /**
     * Finds the shortest transfer path using BFS.
     */
    @GetMapping("/graph/path")
    public List<Long> findPath(@RequestParam Long source, @RequestParam Long target) {
        return graphService.findTransferPath(source, target);
    }

    /**
     * Executes a multi-hop transfer.
     */
    @PostMapping("/multi-hop")
    public List<Allocation> multiHopTransfer(@RequestParam Long itemId,
                                             @RequestParam Long sourceNgoId,
                                             @RequestParam Long targetNgoId,
                                             @RequestParam int quantity) {
        return graphService.executeMultiHopTransfer(itemId, sourceNgoId, targetNgoId, quantity);
    }

    /**
     * Gets the full transfer graph.
     */
    @GetMapping("/graph")
    public Map<Long, Set<Long>> getTransferGraph() {
        return graphService.getTransferGraph();
    }

    // ===========================
    // Feature 7: Compatibility Matrix
    // ===========================

    @GetMapping("/compatibility-matrix")
    public Map<String, Set<String>> getCompatibilityMatrix() {
        return matchingService.getCompatibilityMatrix();
    }

    // ===========================
    // Feature 8: Time-Slot Scheduling
    // ===========================

    @PostMapping("/schedule/create")
    public TimeSlot createTimeSlot(@RequestParam Long allocationId,
                                    @RequestParam Long ngoId,
                                    @RequestParam String start,
                                    @RequestParam String end,
                                    @RequestParam String taskType) {
        return schedulingService.createTimeSlot(
            allocationId, ngoId,
            LocalDateTime.parse(start), LocalDateTime.parse(end),
            taskType
        );
    }

    @GetMapping("/schedule/{vehicleId}")
    public List<TimeSlot> getVehicleSchedule(@PathVariable String vehicleId) {
        return schedulingService.getVehicleSchedule(vehicleId);
    }

    // ===========================
    // Feature 9: Backup Matching
    // ===========================

    @PostMapping("/backup/{requestId}")
    public Allocation tryBackup(@PathVariable Long requestId) {
        Allocation result = schedulingService.tryBackupAllocation(requestId);
        if (result == null) {
            throw new RuntimeException("No backup NGO available for request: " + requestId);
        }
        return result;
    }

    // ===========================
    // Feature 10: Multi-Level Queues
    // ===========================

    @GetMapping("/queues")
    public Map<UrgencyLevel, List<ResourceRequest>> getMultiLevelQueues() {
        return matchingService.getMultiLevelQueues();
    }

    @GetMapping("/queues/ordered")
    public List<ResourceRequest> getOrderedRequests() {
        return matchingService.getOrderedRequests();
    }

    // ===========================
    // Feature 11: Recommendations
    // ===========================

    @GetMapping("/recommendations")
    public Map<String, Integer> getRecommendations() {
        return matchingService.getRecommendations();
    }

    // ===========================
    // Feature 13: Rollback
    // ===========================

    @PostMapping("/rollback/{allocationId}")
    public Allocation rollback(@PathVariable Long allocationId) {
        return lifecycleManager.rollbackAllocation(allocationId);
    }

    // ===========================
    // Feature 16: Lifecycle Management
    // ===========================

    @PutMapping("/lifecycle/{itemId}")
    public Item transitionLifecycle(@PathVariable Long itemId,
                                    @RequestParam String newState) {
        LifecycleState state = LifecycleState.valueOf(newState.toUpperCase());
        return lifecycleManager.transitionState(itemId, state);
    }

    @GetMapping("/lifecycle/{itemId}")
    public Map<String, Object> getLifecycle(@PathVariable Long itemId) {
        return lifecycleManager.getItemLifecycle(itemId);
    }

    @PostMapping("/lifecycle/expire-check")
    public List<Item> checkExpiredItems() {
        return lifecycleManager.checkAndExpireItems();
    }

    // ===========================
    // Feature 18: Emergency Mode
    // ===========================

    @PostMapping("/emergency/activate")
    public String activateEmergency() {
        matchingService.activateEmergencyMode();
        return "EMERGENCY MODE ACTIVATED. All CRITICAL requests will be prioritized.";
    }

    @PostMapping("/emergency/deactivate")
    public String deactivateEmergency() {
        matchingService.deactivateEmergencyMode();
        return "Emergency mode deactivated. Normal matching logic restored.";
    }

    @GetMapping("/emergency/status")
    public Map<String, Boolean> emergencyStatus() {
        return Map.of("emergencyMode", matchingService.isEmergencyMode());
    }

    // ===========================
    // Feature 19: Dependency Groups
    // ===========================

    @GetMapping("/dependency-groups")
    public Map<String, List<Item>> getDependencyGroups() {
        return graphService.getDependencyGroups();
    }

    // ===========================
    // Feature 20: Adaptive Thresholds
    // ===========================

    @GetMapping("/thresholds")
    public Map<String, Integer> getThresholds() {
        return matchingService.getAdaptiveThresholds();
    }

    @PostMapping("/thresholds/update")
    public Map<String, Integer> updateThresholds() {
        matchingService.updateAdaptiveThresholds();
        return matchingService.getAdaptiveThresholds();
    }
}
