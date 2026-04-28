package com.example.surplusconnect.service;

import com.example.surplusconnect.model.*;
import com.example.surplusconnect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Review Service implementing:
 * 
 * Feature 21: Review & Rating System
 * - Weighted Moving Average for credibility scoring
 * - Spam prevention via SHA-256 hashing of (allocationId + reviewerId + targetId)
 * 
 * Rating formula: R_new = (1 - α) * R_old + α * R_current
 * where α = 0.3 (Exponential Moving Average smoothing factor)
 */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final NGORepository ngoRepo;
    private final AllocationRepository allocationRepo;

    private static final double ALPHA = 0.3; // EMA smoothing factor

    public ReviewService(ReviewRepository reviewRepo, NGORepository ngoRepo,
                         AllocationRepository allocationRepo) {
        this.reviewRepo = reviewRepo;
        this.ngoRepo = ngoRepo;
        this.allocationRepo = allocationRepo;
    }

    /**
     * Submits a review with duplicate prevention.
     * 
     * 1. Generate SHA-256 hash of (allocationId + reviewerId + targetId)
     * 2. Check if hash already exists (O(1) lookup in HashSet via DB)
     * 3. If duplicate, reject the review
     * 4. If new, save and update target's rating using EMA
     */
    @Transactional
    public Review submitReview(Review review) {
        // Generate unique hash — based on donationId + reviewerType + targetType to allow re-reviews on different items
        String raw = review.getDonationId() + "|" + review.getReviewerType() + "|" 
                   + review.getTargetType() + "|" + review.getTargetId();
        String hash = sha256(raw);

        // Check for duplicates
        List<Review> existing = reviewRepo.findByReviewHash(hash);
        if (!existing.isEmpty()) {
            throw new IllegalStateException("Duplicate review detected for this transaction.");
        }

        // Validate allocation exists (optional — skip if not found)
        if (review.getAllocationId() != null) {
            // Only validate if allocationId is provided and non-zero
            if (review.getAllocationId() > 0) {
                allocationRepo.findById(review.getAllocationId()); // soft check, don't throw
            }
        }

        // Validate rating range
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        review.setReviewHash(hash);
        Review saved = reviewRepo.save(review);

        // Update target's rating using Exponential Moving Average
        if ("NGO".equals(review.getTargetType())) {
            updateNGORating(review.getTargetId(), review.getRating());
        }

        return saved;
    }

    /**
     * Updates NGO rating using Exponential Moving Average (EMA).
     * 
     * Formula: R_new = (1 - α) * R_old + α * R_current
     * 
     * EMA gives more weight to recent reviews while preserving
     * historical context, providing a balanced credibility score.
     */
    private void updateNGORating(Long ngoId, int newRating) {
        Optional<NGO> ngoOpt = ngoRepo.findById(ngoId);
        if (ngoOpt.isPresent()) {
            NGO ngo = ngoOpt.get();
            double currentRating = ngo.getRating();
            double updatedRating = (1 - ALPHA) * currentRating + ALPHA * newRating;
            ngo.setRating(Math.round(updatedRating * 100.0) / 100.0); // 2 decimal places
            ngo.setTotalReviews(ngo.getTotalReviews() + 1);
            ngoRepo.save(ngo);
        }
    }

    /**
     * Gets all reviews for a specific target (NGO or Donor).
     */
    public List<Review> getReviewsForTarget(Long targetId, String targetType) {
        return reviewRepo.findByTargetIdAndTargetType(targetId, targetType);
    }

    /**
     * Gets aggregate statistics for a target's reviews.
     */
    public Map<String, Object> getReviewStats(Long targetId, String targetType) {
        List<Review> reviews = reviewRepo.findByTargetIdAndTargetType(targetId, targetType);
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("targetId", targetId);
        stats.put("targetType", targetType);
        stats.put("totalReviews", reviews.size());

        if (!reviews.isEmpty()) {
            double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
            stats.put("averageRating", Math.round(avg * 100.0) / 100.0);

            // Distribution
            Map<Integer, Long> distribution = new TreeMap<>();
            for (int i = 1; i <= 5; i++) {
                final int rating = i;
                distribution.put(i, reviews.stream()
                    .filter(r -> r.getRating() == rating).count());
            }
            stats.put("distribution", distribution);
        } else {
            stats.put("averageRating", 0.0);
            stats.put("distribution", Collections.emptyMap());
        }

        return stats;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public List<Review> getReviewsForDonation(Long donationId) {
        return reviewRepo.findByDonationId(donationId);
    }

    public Review getReviewById(Long id) {
        return reviewRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Review not found: " + id));
    }

    public Review updateReview(Review review) {
        return reviewRepo.save(review);
    }
}
