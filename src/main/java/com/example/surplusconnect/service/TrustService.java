package com.example.surplusconnect.service;

import com.example.surplusconnect.model.NGO;
import com.example.surplusconnect.model.User;
import com.example.surplusconnect.repository.NGORepository;
import com.example.surplusconnect.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Trust & Credibility Score System
 * 
 * Maintains scores based on historical performance:
 * - Successful deliveries (+ points)
 * - Rejections (- points)
 * - Ratings (weighted contribution)
 */
@Service
public class TrustService {

    private final NGORepository ngoRepo;
    private final UserRepository userRepo;

    private static final double SUCCESS_INCREMENT = 5.0;
    private static final double REJECTION_DECREMENT = 10.0;
    private static final double RATING_WEIGHT = 0.5;

    public TrustService(NGORepository ngoRepo, UserRepository userRepo) {
        this.ngoRepo = ngoRepo;
        this.userRepo = userRepo;
    }

    public void updateNgoTrustOnSuccess(Long ngoId) {
        Optional<NGO> ngoOpt = ngoRepo.findById(ngoId);
        if (ngoOpt.isPresent()) {
            NGO ngo = ngoOpt.get();
            ngo.setAllocationCount(ngo.getAllocationCount() + 1);
            
            double newScore = ngo.getTrustScore() + SUCCESS_INCREMENT;
            // Contribution from rating: (rating/5.0) * score
            newScore = (newScore * (1 - RATING_WEIGHT)) + (ngo.getRating() * 20 * RATING_WEIGHT);
            
            ngo.setTrustScore(Math.min(100.0, newScore));
            ngoRepo.save(ngo);
        }
    }

    public void updateNgoTrustOnRejection(Long ngoId) {
        Optional<NGO> ngoOpt = ngoRepo.findById(ngoId);
        if (ngoOpt.isPresent()) {
            NGO ngo = ngoOpt.get();
            ngo.setRejectionCount(ngo.getRejectionCount() + 1);
            ngo.setTrustScore(Math.max(0.0, ngo.getTrustScore() - REJECTION_DECREMENT));
            ngoRepo.save(ngo);
        }
    }

    public void updateUserTrust(Long userId, boolean success) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (success) {
                user.setSuccessfulDonations(user.getSuccessfulDonations() + 1);
                user.setTrustScore(Math.min(100.0, user.getTrustScore() + SUCCESS_INCREMENT));
            } else {
                user.setCancelledDonations(user.getCancelledDonations() + 1);
                user.setTrustScore(Math.max(0.0, user.getTrustScore() - REJECTION_DECREMENT));
            }
            userRepo.save(user);
        }
    }
}
