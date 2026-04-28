package com.example.surplusconnect.repository;

import com.example.surplusconnect.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByTargetIdAndTargetType(Long targetId, String targetType);
    List<Review> findByReviewHash(String reviewHash);
    List<Review> findByAllocationId(Long allocationId);
    List<Review> findByDonationId(Long donationId);
}
