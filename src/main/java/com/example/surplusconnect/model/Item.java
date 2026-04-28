package com.example.surplusconnect.model;

import jakarta.persistence.*;

/**
 * Enhanced Item entity supporting:
 * - Smart Resource Splitting (Feature 1): originalQuantity vs remainingQuantity
 * - Resource Lifecycle Management (Feature 16): lifecycleState FSM
 * - Geographic Clustering (Feature 12): latitude/longitude
 * - Image Upload (Feature 22): imageUrl
 * - Dependency-Based Allocation (Feature 19): dependencyGroup
 */
@Entity
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String donorName;
    private String donorPhone;
    private String itemType;
    private String category;
    private String quantity;
    private String expiryDate;
    private String location;
    private String status;
    private Long donorId; // Linked to User.id
    private String timestamp;
    private String priority;

    // --- Feature 1: Dynamic Quantity Splitting ---
    private Integer originalQuantityNum;
    private Integer remainingQuantityNum;

    // --- Feature 12: Geographic Clustering ---
    @Column(columnDefinition = "DOUBLE DEFAULT 0.0")
    private Double latitude = 0.0;

    @Column(columnDefinition = "DOUBLE DEFAULT 0.0")
    private Double longitude = 0.0;

    // --- Feature 16: Resource Lifecycle FSM ---
    @Enumerated(EnumType.STRING)
    private LifecycleState lifecycleState = LifecycleState.PENDING;

    // --- Feature 22: Image Upload ---
    private String imageUrl;

    // --- Feature 19: Dependency-Based Allocation ---
    private String dependencyGroup;

    // --- Feature 14: Duplicate Detection Hash ---
    private String contentHash;

    // --- Feature 6: Quality Verification ---
    private boolean isQualityVerified;
    private Double verificationScore;
    private String verificationNotes;

    public Item() {}

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }

    public String getDonorPhone() { return donorPhone; }
    public void setDonorPhone(String donorPhone) { this.donorPhone = donorPhone; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getDonorId() { return donorId; }
    public void setDonorId(Long donorId) { this.donorId = donorId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Integer getOriginalQuantityNum() { return originalQuantityNum != null ? originalQuantityNum : 0; }
    public void setOriginalQuantityNum(Integer originalQuantityNum) { this.originalQuantityNum = originalQuantityNum; }

    public Integer getRemainingQuantityNum() { return remainingQuantityNum != null ? remainingQuantityNum : 0; }
    public void setRemainingQuantityNum(Integer remainingQuantityNum) { this.remainingQuantityNum = remainingQuantityNum; }

    public Double getLatitude() { return latitude != null ? latitude : 0.0; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude != null ? longitude : 0.0; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LifecycleState getLifecycleState() { return lifecycleState; }
    public void setLifecycleState(LifecycleState lifecycleState) { this.lifecycleState = lifecycleState; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDependencyGroup() { return dependencyGroup; }
    public void setDependencyGroup(String dependencyGroup) { this.dependencyGroup = dependencyGroup; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public boolean isQualityVerified() { return isQualityVerified; }
    public void setQualityVerified(boolean qualityVerified) { isQualityVerified = qualityVerified; }

    public double getVerificationScore() { return verificationScore != null ? verificationScore : 0.0; }
    public void setVerificationScore(double verificationScore) { this.verificationScore = verificationScore; }

    public String getVerificationNotes() { return verificationNotes; }
    public void setVerificationNotes(String verificationNotes) { this.verificationNotes = verificationNotes; }
}
