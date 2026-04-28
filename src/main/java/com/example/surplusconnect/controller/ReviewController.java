package com.example.surplusconnect.controller;

import com.example.surplusconnect.model.Review;
import com.example.surplusconnect.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Review Controller (Feature 21).
 * Handles review submission with duplicate prevention and rating aggregation.
 */
@RestController
@RequestMapping("/api/reviews")
@CrossOrigin
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Submit a review with SHA-256 duplicate detection.
     */
    @PostMapping
    public ResponseEntity<?> submitReview(@RequestBody Review review) {
        try {
            Review saved = reviewService.submitReview(review);
            return ResponseEntity.ok(saved);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Get all reviews for a target (NGO or Donor).
     */
    @GetMapping("/target/{targetId}")
    public List<Review> getReviews(@PathVariable Long targetId,
                                    @RequestParam String targetType) {
        return reviewService.getReviewsForTarget(targetId, targetType);
    }

    /**
     * Get aggregate review statistics for a target.
     */
    @GetMapping("/stats/{targetId}")
    public Map<String, Object> getStats(@PathVariable Long targetId,
                                         @RequestParam String targetType) {
        return reviewService.getReviewStats(targetId, targetType);
    }

    /**
     * Get reviews by donationId
     */
    @GetMapping("/donation/{donationId}")
    public List<Review> getReviewsByDonation(@PathVariable Long donationId) {
        return reviewService.getReviewsForDonation(donationId);
    }

    /**
     * Upload an image for a review (with file type validation)
     */
    @PostMapping("/{id}/upload-image")
    public ResponseEntity<?> uploadImage(@PathVariable Long id,
                                          @RequestParam("file") MultipartFile file) throws IOException {
        Review review = reviewService.getReviewById(id);

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String extension = originalName.contains(".")
            ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase()
            : "";

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Invalid file type. Allowed: jpg, jpeg, png, gif, webp");
        }

        File uploadDir = new File("uploads/");
        if (!uploadDir.exists()) uploadDir.mkdirs();

        String uniqueName = "review_" + id + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path filePath = Paths.get("uploads/" + uniqueName);
        Files.write(filePath, file.getBytes());

        review.setImageUrl("/uploads/" + uniqueName);
        Review saved = reviewService.updateReview(review);
        log.info("Review image uploaded: review id={}, file={}", id, uniqueName);
        return ResponseEntity.ok(saved);
    }
}
