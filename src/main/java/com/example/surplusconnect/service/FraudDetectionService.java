package com.example.surplusconnect.service;

import com.example.surplusconnect.model.Item;
import com.example.surplusconnect.model.ResourceRequest;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fraud Detection System (Rule-Based)
 * 
 * Detects:
 * - Duplicate requests (using HashSet/Hashing)
 * - Rapid-fire submissions (Pattern tracking)
 * - Suspicious quantities
 */
@Service
public class FraudDetectionService {

    private final Set<String> seenHashes = new HashSet<>();
    private final Map<Long, Long> lastSubmissionTimes = new ConcurrentHashMap<>();
    
    private static final long MIN_SUBMISSION_INTERVAL_MS = 5000; // 5 seconds between submissions per user
    private static final int MAX_SUSPICIOUS_QUANTITY = 5000;

    public boolean isSuspiciousItem(Item item) {
        // Rule 1: Pattern tracking - Rate limiting
        if (isRateLimited(item.getDonorId())) {
            return true;
        }

        // Rule 2: Suspicious quantity
        if (item.getOriginalQuantityNum() > MAX_SUSPICIOUS_QUANTITY) {
            return true;
        }

        return false;
    }

    public boolean isSuspiciousRequest(ResourceRequest request) {
        // Rule 1: Rate limiting
        if (isRateLimited(request.getNgoId())) {
            return true;
        }

        // Rule 2: Suspicious quantity
        if (request.getQuantityRequested() > MAX_SUSPICIOUS_QUANTITY) {
            return true;
        }

        return false;
    }

    private boolean isRateLimited(Long entityId) {
        if (entityId == null) return false;
        long now = System.currentTimeMillis();
        Long lastTime = lastSubmissionTimes.get(entityId);
        
        if (lastTime != null && (now - lastTime) < MIN_SUBMISSION_INTERVAL_MS) {
            return true;
        }
        
        lastSubmissionTimes.put(entityId, now);
        return false;
    }

    public void recordHash(String hash) {
        seenHashes.add(hash);
    }

    public boolean hashExists(String hash) {
        return seenHashes.contains(hash);
    }
}
