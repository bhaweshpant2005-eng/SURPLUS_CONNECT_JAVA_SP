package com.example.surplusconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Enhanced Review entity supporting:
 * - Review & Rating System (Feature 21)
 * - Weighted moving average for credibility scoring
 * - Duplicate prevention using transaction-based unique hashing (Feature 14)
 * 
 * Rating calculation: R_new = (1 - α) * R_old + α * R_current
 * where α = 0.3 (smoothing factor)
 */
@Entity
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int rating;          // 1 to 5
    private String comment;

    // Link to transaction
    private Long allocationId;
    private Long donationId;

    private String imageUrl;

    // Reviewer info
    private Long reviewerId;     // Can be donor or NGO
    private String reviewerType; // "DONOR" or "NGO"
    private String reviewerName;

    // Target of review
    private Long targetId;
    private String targetType;   // "DONOR" or "NGO"

    // --- Feature 14: Duplicate prevention ---
    private String reviewHash;   // SHA-256 of (allocationId + reviewerId + targetId)

    private LocalDateTime createdAt;

    public Review() {
        this.createdAt = LocalDateTime.now();
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Long getAllocationId() { return allocationId; }
    public void setAllocationId(Long allocationId) { this.allocationId = allocationId; }

    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }

    public String getReviewerType() { return reviewerType; }
    public void setReviewerType(String reviewerType) { this.reviewerType = reviewerType; }

    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getReviewHash() { return reviewHash; }
    public void setReviewHash(String reviewHash) { this.reviewHash = reviewHash; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Long getDonationId() { return donationId; }
    public void setDonationId(Long donationId) { this.donationId = donationId; }
}
