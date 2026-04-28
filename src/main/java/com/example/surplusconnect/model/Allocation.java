package com.example.surplusconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Allocation entity (Transaction Tracking) supporting:
 * - Transaction Rollback (Feature 13): tracks state for undo operations (Command Pattern)
 * - Resource Lifecycle (Feature 16): links Item state transitions to allocations
 * - Time-Slot Based Allocation (Feature 8): scheduledPickup / scheduledDelivery
 * - Multi-Hop Allocation (Feature 4): hopPath records the transfer chain
 */
@Entity
public class Allocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long itemId;
    private Long ngoId;
    private Long requestId;

    private Integer allocatedQuantity;

    // --- Feature 16: Lifecycle tracking ---
    @Enumerated(EnumType.STRING)
    private LifecycleState currentState = LifecycleState.MATCHED;

    // --- Feature 13: Rollback support (previous state snapshot) ---
    private String previousItemStatus;
    private Integer previousRemainingQuantity;
    private Integer previousNgoLoad;
    private boolean rolledBack = false;

    // --- Feature 8: Time-Slot Scheduling ---
    private LocalDateTime scheduledPickup;
    private LocalDateTime scheduledDelivery;

    // --- Feature 4: Multi-Hop path ---
    private String hopPath; // e.g., "NGO-3 -> NGO-7 -> NGO-12"

    // --- Timestamps ---
    private LocalDateTime allocatedAt;
    private LocalDateTime completedAt;

    // --- Feature 6: Conflict resolution audit ---
    private Double matchScore;

    public Allocation() {
        this.allocatedAt = LocalDateTime.now();
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getNgoId() { return ngoId; }
    public void setNgoId(Long ngoId) { this.ngoId = ngoId; }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public Integer getAllocatedQuantity() { return allocatedQuantity != null ? allocatedQuantity : 0; }
    public void setAllocatedQuantity(Integer allocatedQuantity) { this.allocatedQuantity = allocatedQuantity; }

    public LifecycleState getCurrentState() { return currentState; }
    public void setCurrentState(LifecycleState currentState) { this.currentState = currentState; }

    public String getPreviousItemStatus() { return previousItemStatus; }
    public void setPreviousItemStatus(String previousItemStatus) { this.previousItemStatus = previousItemStatus; }

    public Integer getPreviousRemainingQuantity() { return previousRemainingQuantity != null ? previousRemainingQuantity : 0; }
    public void setPreviousRemainingQuantity(Integer previousRemainingQuantity) { this.previousRemainingQuantity = previousRemainingQuantity; }

    public Integer getPreviousNgoLoad() { return previousNgoLoad != null ? previousNgoLoad : 0; }
    public void setPreviousNgoLoad(Integer previousNgoLoad) { this.previousNgoLoad = previousNgoLoad; }

    public boolean isRolledBack() { return rolledBack; }
    public void setRolledBack(boolean rolledBack) { this.rolledBack = rolledBack; }

    public LocalDateTime getScheduledPickup() { return scheduledPickup; }
    public void setScheduledPickup(LocalDateTime scheduledPickup) { this.scheduledPickup = scheduledPickup; }

    public LocalDateTime getScheduledDelivery() { return scheduledDelivery; }
    public void setScheduledDelivery(LocalDateTime scheduledDelivery) { this.scheduledDelivery = scheduledDelivery; }

    public String getHopPath() { return hopPath; }
    public void setHopPath(String hopPath) { this.hopPath = hopPath; }

    public LocalDateTime getAllocatedAt() { return allocatedAt; }
    public void setAllocatedAt(LocalDateTime allocatedAt) { this.allocatedAt = allocatedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Double getMatchScore() { return matchScore != null ? matchScore : 0.0; }
    public void setMatchScore(Double matchScore) { this.matchScore = matchScore; }
}
