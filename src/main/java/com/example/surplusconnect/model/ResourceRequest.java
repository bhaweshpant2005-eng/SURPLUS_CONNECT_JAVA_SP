package com.example.surplusconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Resource Request entity supporting:
 * - Multi-Level Queue System (Feature 10): urgencyLevel determines queue placement
 * - Dynamic Re-Prioritization (Feature 3): priorityScore is recalculated over time
 * - Constraint-Based Matching (Feature 2): constraints on type, quantity, location
 * - Backup Matching Pool (Feature 9): backupNgoIds for failover
 * - Adaptive Threshold (Feature 20): minAcceptableQuantity adjusts dynamically
 */
@Entity
public class ResourceRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ngoId;
    private String ngoName;
    private String contactPerson;
    private String phone;
    private String resourceType;
    private String category;
    private Integer quantityRequested;
    private Integer remainingNeed; // Feature: Partial Allocation Carry-Forward
    private String location;

    // --- Feature 10: Multi-Level Queue ---
    @Enumerated(EnumType.STRING)
    private UrgencyLevel urgencyLevel = UrgencyLevel.NORMAL;

    // --- Feature 3: Dynamic Re-Prioritization ---
    @Column(columnDefinition = "DOUBLE DEFAULT 0.0")
    private Double priorityScore = 0.0;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    // --- Feature 20: Adaptive Threshold ---
    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer minAcceptableQuantity = 0;

    // --- Feature 9: Backup Matching Pool ---
    private String backupNgoIds;

    // --- Status tracking ---
    private String status = "Pending"; // Pending, Matched, Fulfilled, Expired

    // --- Feature 14: Duplicate Detection ---
    private String requestHash;

    public ResourceRequest() {
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNgoId() { return ngoId; }
    public void setNgoId(Long ngoId) { this.ngoId = ngoId; }

    public String getNgoName() { return ngoName; }
    public void setNgoName(String ngoName) { this.ngoName = ngoName; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getQuantityRequested() { return quantityRequested != null ? quantityRequested : 0; }
    public void setQuantityRequested(Integer quantityRequested) { 
        this.quantityRequested = quantityRequested; 
        if (getRemainingNeed() == 0 && "Pending".equals(this.status)) {
            this.remainingNeed = quantityRequested;
        }
    }

    public Integer getRemainingNeed() { return remainingNeed != null ? remainingNeed : 0; }
    public void setRemainingNeed(Integer remainingNeed) { this.remainingNeed = remainingNeed; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public UrgencyLevel getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(UrgencyLevel urgencyLevel) { this.urgencyLevel = urgencyLevel; }

    public Double getPriorityScore() { return priorityScore != null ? priorityScore : 0.0; }
    public void setPriorityScore(Double priorityScore) { this.priorityScore = priorityScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public Integer getMinAcceptableQuantity() { return minAcceptableQuantity != null ? minAcceptableQuantity : 0; }
    public void setMinAcceptableQuantity(Integer minAcceptableQuantity) { this.minAcceptableQuantity = minAcceptableQuantity; }

    public String getBackupNgoIds() { return backupNgoIds; }
    public void setBackupNgoIds(String backupNgoIds) { this.backupNgoIds = backupNgoIds; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
}
