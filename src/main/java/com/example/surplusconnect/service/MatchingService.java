package com.example.surplusconnect.service;

import com.example.surplusconnect.model.*;
import com.example.surplusconnect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core Matching Service implementing:
 * 
 * Feature 1:  Smart Resource Splitting (Greedy)
 * Feature 2:  Constraint-Based Matching (Filter pipeline)
 * Feature 3:  Dynamic Re-Prioritization (Aging algorithm)
 * Feature 5:  Capacity-Aware Allocation
 * Feature 6:  Conflict Resolution (Weighted scoring)
 * Feature 7:  Resource Compatibility Matrix
 * Feature 10: Multi-Level Queue System
 * Feature 12: Geographic Clustering (Simplified K-Means)
 * Feature 14: Duplicate Detection (SHA-256 hashing)
 * Feature 15: Weighted Fair Distribution
 * Feature 18: Emergency Mode override
 * Feature 20: Adaptive Threshold
 */
@Service
public class MatchingService {

    private final ItemRepository itemRepo;
    private final NGORepository ngoRepo;
    private final ResourceRequestRepository requestRepo;
    private final AllocationRepository allocationRepo;
    private final TrustService trustService;
    private final AnalyticsService analyticsService;
    private final FraudDetectionService fraudService;
    private final ResourceQualityService qualityService;
    private final List<NotificationObserver> observers = new ArrayList<>();

    // --- Feature 18: Emergency Mode flag ---
    private boolean emergencyMode = false;

    // --- Feature 7: Resource Compatibility Matrix ---
    private final Map<String, Set<String>> compatibilityMatrix = new HashMap<>();

    // --- NEW: Resource Dependency Map (Feature 12) ---
    private final Map<String, List<String>> dependencyMap = new HashMap<>();

    // --- Feature 14: Seen hashes for duplicate detection ---
    private final Set<String> seenDonationHashes = new HashSet<>();
    private final Set<String> seenRequestHashes = new HashSet<>();

    // --- Feature 20: Adaptive thresholds ---
    private final Map<String, Integer> adaptiveThresholds = new HashMap<>();

    // --- Priority weights for scoring ---
    private static final double URGENCY_WEIGHT = 0.5;
    private static final double WAIT_TIME_WEIGHT = 0.3;
    private static final double FAIRNESS_WEIGHT = 0.2; // Based on allocation_count

    public MatchingService(ItemRepository itemRepo, NGORepository ngoRepo,
                           ResourceRequestRepository requestRepo,
                           AllocationRepository allocationRepo,
                           TrustService trustService,
                           AnalyticsService analyticsService,
                           FraudDetectionService fraudService,
                           ResourceQualityService qualityService,
                           List<NotificationObserver> notificationObservers) {
        this.itemRepo = itemRepo;
        this.ngoRepo = ngoRepo;
        this.requestRepo = requestRepo;
        this.allocationRepo = allocationRepo;
        this.trustService = trustService;
        this.analyticsService = analyticsService;
        this.fraudService = fraudService;
        this.qualityService = qualityService;
        
        if (notificationObservers != null) {
            this.observers.addAll(notificationObservers);
        }
        initializeCompatibilityMatrix();
        initializeDependencyMap();
        initializeAdaptiveThresholds();
    }

    /**
     * Feature 7: Initialize Resource Compatibility Matrix.
     * 2D mapping from NGO categories to suitable resource types.
     * Allows O(1) lookup for compatibility checks.
     */
    private void initializeCompatibilityMatrix() {
        compatibilityMatrix.put("Children's Home", new HashSet<>(Arrays.asList(
            "Food", "Clothes", "Essentials"
        )));
        compatibilityMatrix.put("Shelter", new HashSet<>(Arrays.asList(
            "Food", "Clothes", "Essentials"
        )));
        compatibilityMatrix.put("Hospital", new HashSet<>(Arrays.asList(
            "Food", "Essentials"
        )));
        compatibilityMatrix.put("Food Bank", new HashSet<>(Arrays.asList(
            "Food"
        )));
        compatibilityMatrix.put("School", new HashSet<>(Arrays.asList(
            "Essentials", "Clothes"
        )));
    }

    /**
     * Feature 12: Resource Dependency Mapping.
     * Bundles related resources (e.g., Rice + Dal, Books + Bags).
     */
    private void initializeDependencyMap() {
        dependencyMap.put("Rice", Arrays.asList("Dal", "Oil", "Salt"));
        dependencyMap.put("Books", Arrays.asList("Stationery", "Bags"));
        dependencyMap.put("Clothes", Arrays.asList("Blankets", "Shoes"));
    }

    /**
     * Feature 20: Initialize adaptive thresholds with defaults.
     * These adjust dynamically based on supply/demand ratio.
     */
    private void initializeAdaptiveThresholds() {
        adaptiveThresholds.put("Food", 10);
        adaptiveThresholds.put("Clothes", 5);
        adaptiveThresholds.put("Essentials", 3);
    }

    // ===========================
    // Feature 18: EMERGENCY MODE
    // ===========================

    public void activateEmergencyMode() {
        this.emergencyMode = true;
    }

    public void deactivateEmergencyMode() {
        this.emergencyMode = false;
    }

    public boolean isEmergencyMode() {
        return emergencyMode;
    }

    // ========================================
    // Feature 14: DUPLICATE DETECTION (SHA-256)
    // ========================================

    /**
     * Generates SHA-256 hash for donation duplicate detection.
     * Hash input: donorName + itemType + category + quantity
     */
    public String generateDonationHash(Item item) {
        String name = item.getDonorName() != null ? item.getDonorName() : "anonymous";
        String type = item.getItemType() != null ? item.getItemType() : "unknown";
        String cat = item.getCategory() != null ? item.getCategory() : "none";
        String qty = item.getQuantity() != null ? item.getQuantity() : "0";
        
        String raw = name + "|" + type + "|" + cat + "|" + qty;
        return sha256(raw);
    }

    /**
     * Generates SHA-256 hash for request duplicate detection.
     * Hash input: ngoId + resourceType + category + quantityRequested
     */
    public String generateRequestHash(ResourceRequest request) {
        String raw = request.getNgoId() + "|" + request.getResourceType() + "|"
                   + request.getCategory() + "|" + request.getQuantityRequested();
        return sha256(raw);
    }

    /**
     * SHA-256 hashing utility.
     * Time complexity: O(n) where n = input length
     */
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

    public boolean isDuplicateDonation(Item item) {
        String hash = generateDonationHash(item);
        if (seenDonationHashes.contains(hash)) {
            return true;
        }
        // Check DB using indexed hash lookup (O(1) vs O(N) full scan)
        return itemRepo.findByContentHash(hash).isPresent();
    }

    public boolean isDuplicateRequest(ResourceRequest request) {
        String hash = generateRequestHash(request);
        if (seenRequestHashes.contains(hash)) {
            return true;
        }
        List<ResourceRequest> existing = requestRepo.findByRequestHash(hash);
        return !existing.isEmpty();
    }

    // ===========================
    // Feature 3: DYNAMIC RE-PRIORITIZATION (Aging Algorithm)
    // ===========================

    /**
     * Feature 1: Fairness-Aware Allocation Engine.
     * 
     * Formula: Final Score = urgency + waiting_time – allocation_count
     * 
     * Time complexity: O(N)
     */
    public void recalculatePriorities() {
        List<ResourceRequest> pending = requestRepo.findByStatusIn(Arrays.asList("Pending", "PartiallyFulfilled"));
        LocalDateTime now = LocalDateTime.now();

        for (ResourceRequest req : pending) {
            // 1. Urgency component (0-100)
            double urgencyScore = req.getUrgencyLevel().getWeight() * 25.0;

            // 2. Waiting time component (Aging)
            long minutesWaiting = ChronoUnit.MINUTES.between(req.getCreatedAt(), now);
            double waitScore = Math.min(minutesWaiting * 0.5, 100.0);

            // 3. Fairness/Allocation Count component (Penalty for frequent receivers)
            double fairnessPenalty = 0;
            if (req.getNgoId() != null) {
                Optional<NGO> ngo = ngoRepo.findById(req.getNgoId());
                if (ngo.isPresent()) {
                    fairnessPenalty = ngo.get().getAllocationCount() * 2.0; // Penalty per previous allocation
                }
            }

            double finalScore = (URGENCY_WEIGHT * urgencyScore)
                              + (WAIT_TIME_WEIGHT * waitScore)
                              - (FAIRNESS_WEIGHT * fairnessPenalty);

            // Feature 11: Emergency Mode override
            // During crises, priority is strictly urgency-based
            if (emergencyMode && req.getUrgencyLevel() == UrgencyLevel.CRITICAL) {
                finalScore = 500.0; 
            }

            req.setPriorityScore(finalScore);
            req.setLastUpdated(now);
        }

        requestRepo.saveAll(pending);
    }

    // ===========================
    // Feature 10: MULTI-LEVEL QUEUE SYSTEM
    // ===========================

    /**
     * Returns pending requests organized by urgency level.
     * Implements a 4-level priority queue structure.
     * 
     * Processing order: CRITICAL -> HIGH -> NORMAL -> LOW
     * Within each level, sorted by priorityScore descending.
     * 
     * Data Structure: Map<UrgencyLevel, PriorityQueue<ResourceRequest>>
     */
    public Map<UrgencyLevel, List<ResourceRequest>> getMultiLevelQueues() {
        List<ResourceRequest> all = requestRepo.findByStatusIn(Arrays.asList("Pending", "PartiallyFulfilled"));

        Map<UrgencyLevel, List<ResourceRequest>> queues = new LinkedHashMap<>();
        for (UrgencyLevel level : UrgencyLevel.values()) {
            queues.put(level, new ArrayList<>());
        }

        for (ResourceRequest req : all) {
            queues.get(req.getUrgencyLevel()).add(req);
        }

        // Sort each queue by priority score descending
        for (List<ResourceRequest> queue : queues.values()) {
            queue.sort((a, b) -> Double.compare(b.getPriorityScore(), a.getPriorityScore()));
        }

        return queues;
    }

    /**
     * Gets a flattened, ordered list of all pending requests
     * with CRITICAL first, then HIGH, NORMAL, LOW.
     */
    public List<ResourceRequest> getOrderedRequests() {
        Map<UrgencyLevel, List<ResourceRequest>> queues = getMultiLevelQueues();
        List<ResourceRequest> ordered = new ArrayList<>();

        // Process in reverse order (CRITICAL = 4 first)
        UrgencyLevel[] levels = {UrgencyLevel.CRITICAL, UrgencyLevel.HIGH,
                                  UrgencyLevel.NORMAL, UrgencyLevel.LOW};
        for (UrgencyLevel level : levels) {
            ordered.addAll(queues.getOrDefault(level, Collections.emptyList()));
        }

        return ordered;
    }

    // ===========================
    // Feature 12: GEOGRAPHIC CLUSTERING (Simplified K-Means)
    // ===========================

    /**
     * Clusters NGOs into K groups based on geographic coordinates.
     * 
     * Algorithm: Simplified K-Means Clustering
     * 1. Initialize K random centroids from existing NGO positions
     * 2. Assign each NGO to nearest centroid (Haversine distance)
     * 3. Recalculate centroids as mean of assigned NGOs
     * 4. Repeat until convergence or max iterations
     * 
     * Time complexity: O(N * K * I) where I = iterations
     */
    public void clusterNGOs(int k) {
        List<NGO> ngos = ngoRepo.findAll();
        if (ngos.isEmpty() || k <= 0) return;

        k = Math.min(k, ngos.size());

        // Step 1: Initialize centroids from random NGOs
        List<double[]> centroids = new ArrayList<>();
        Collections.shuffle(ngos);
        for (int i = 0; i < k; i++) {
            centroids.add(new double[]{ngos.get(i).getLatitude(), ngos.get(i).getLongitude()});
        }

        int maxIterations = 100;
        for (int iter = 0; iter < maxIterations; iter++) {
            // Step 2: Assign each NGO to nearest centroid
            Map<Integer, List<NGO>> clusters = new HashMap<>();
            for (int i = 0; i < k; i++) clusters.put(i, new ArrayList<>());

            for (NGO ngo : ngos) {
                int nearestCluster = 0;
                double minDist = Double.MAX_VALUE;
                for (int i = 0; i < k; i++) {
                    double dist = haversineDistance(
                        ngo.getLatitude(), ngo.getLongitude(),
                        centroids.get(i)[0], centroids.get(i)[1]
                    );
                    if (dist < minDist) {
                        minDist = dist;
                        nearestCluster = i;
                    }
                }
                ngo.setClusterGroup(nearestCluster);
                clusters.get(nearestCluster).add(ngo);
            }

            // Step 3: Recalculate centroids
            boolean converged = true;
            for (int i = 0; i < k; i++) {
                List<NGO> cluster = clusters.get(i);
                if (cluster.isEmpty()) continue;

                double newLat = cluster.stream().mapToDouble(NGO::getLatitude).average().orElse(0);
                double newLon = cluster.stream().mapToDouble(NGO::getLongitude).average().orElse(0);

                if (Math.abs(newLat - centroids.get(i)[0]) > 0.0001
                 || Math.abs(newLon - centroids.get(i)[1]) > 0.0001) {
                    converged = false;
                }
                centroids.get(i)[0] = newLat;
                centroids.get(i)[1] = newLon;
            }

            if (converged) break;
        }

        ngoRepo.saveAll(ngos);
    }

    /**
     * Haversine formula to calculate great-circle distance between two points.
     * Used for proximity-based matching and geographic clustering.
     * Returns distance in kilometers.
     */
    public double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // ======================================
    // Feature 2 + 7: CONSTRAINT & COMPATIBILITY FILTERING
    // ======================================

    /**
     * Filters NGOs that are compatible with a given item.
     * 
     * Pipeline: CompatibilityMatrix -> AcceptedTypes -> Distance -> Capacity
     * 
     * This implements a multi-stage filter-map-reduce pipeline.
     * Each stage eliminates ineligible NGOs, reducing the search space.
     */
    public List<NGO> filterCompatibleNGOs(Item item) {
        List<NGO> allNGOs = ngoRepo.findAll();

        return allNGOs.stream()
            // Stage 1: Resource Compatibility Matrix check (Feature 7)
            .filter(ngo -> {
                if (ngo.getNgoCategory() == null) return true;
                Set<String> compatible = compatibilityMatrix
                    .getOrDefault(ngo.getNgoCategory(), Collections.emptySet());
                return compatible.isEmpty() || compatible.contains(item.getItemType());
            })
            // Stage 2: Accepted types constraint (Feature 2)
            .filter(ngo -> {
                if (ngo.getAcceptedTypes() == null || ngo.getAcceptedTypes().isEmpty()) return true;
                return Arrays.asList(ngo.getAcceptedTypes().split(","))
                    .contains(item.getItemType());
            })
            // Stage 3: Distance constraint (Feature 2 + 12)
            .filter(ngo -> {
                if (ngo.getMaxDistance() <= 0) return true;
                double dist = haversineDistance(
                    item.getLatitude(), item.getLongitude(),
                    ngo.getLatitude(), ngo.getLongitude()
                );
                return dist <= ngo.getMaxDistance();
            })
            // Stage 4: Capacity check (Feature 5)
            .filter(ngo -> ngo.getAvailableCapacity() > 0)
            .collect(Collectors.toList());
    }

    // ===========================
    // Feature 6: CONFLICT RESOLUTION
    // ===========================

    /**
     * Resolves conflicts when multiple NGOs request the same resource.
     * 
     * Scoring formula:
     *   Score = w1 * ProximityScore + w2 * PriorityScore + w3 * FairnessScore + w4 * RatingScore
     * 
     * ProximityScore: Inversely proportional to distance (closer = higher)
     * PriorityScore:  From the dynamic re-prioritization engine
     * FairnessScore:  Inversely proportional to totalReceived (less received = higher)
     * RatingScore:    Direct mapping from NGO rating
     * 
     * Returns NGOs sorted by conflict resolution score (descending).
     */
    public List<NGO> resolveConflict(Item item, List<NGO> candidates) {
        if (candidates.isEmpty()) return candidates;

        // Calculate max values for normalization
        double maxTotalReceived = candidates.stream()
            .mapToInt(NGO::getTotalReceived).max().orElse(1);

        List<Map.Entry<NGO, Double>> scored = new ArrayList<>();

        for (NGO ngo : candidates) {
            // Proximity score (0-100, closer = higher)
            double dist = haversineDistance(
                item.getLatitude(), item.getLongitude(),
                ngo.getLatitude(), ngo.getLongitude()
            );
            double proximityScore = Math.max(0, 100.0 - dist);

            // Priority score from requests
            double priorityScore = 50.0; // default
            List<ResourceRequest> ngoRequests = requestRepo.findByNgoId(ngo.getId());
            if (!ngoRequests.isEmpty()) {
                priorityScore = ngoRequests.stream()
                    .mapToDouble(ResourceRequest::getPriorityScore)
                    .max().orElse(50.0);
            }

            // Fairness score (Feature 15: less received = higher score)
            double fairnessScore = maxTotalReceived > 0
                ? (1.0 - (double) ngo.getTotalReceived() / maxTotalReceived) * 100.0
                : 50.0;

            // NEW: Trust & Credibility Score (Feature 2)
            double trustScore = ngo.getTrustScore();

            double totalScore = (URGENCY_WEIGHT * priorityScore)
                              + (WAIT_TIME_WEIGHT * fairnessScore)
                              + (FAIRNESS_WEIGHT * trustScore);

            scored.add(new AbstractMap.SimpleEntry<>(ngo, totalScore));
        }

        // Sort by score descending
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        return scored.stream().map(Map.Entry::getKey).collect(Collectors.toList());
    }

    // ===========================================
    // Feature 1 + 15: SMART SPLITTING + FAIR DISTRIBUTION
    // ===========================================

    /**
     * Greedy Resource Splitting Algorithm with Fairness and Quality.
     */
    @Transactional
    public List<Allocation> splitAndAllocate(Item item) {
        List<Allocation> allocations = new ArrayList<>();

        if (item.getRemainingQuantityNum() <= 0) return allocations;

        // NEW: Fraud Detection & Quality Verification
        if (fraudService.isSuspiciousItem(item)) {
            item.setStatus("Flagged_Fraud");
            itemRepo.save(item);
            return allocations;
        }
        
        qualityService.verifyQuality(item);
        if (!item.isQualityVerified()) {
            item.setStatus("Rejected_Quality");
            itemRepo.save(item);
            return allocations;
        }

        // Step 1: Filter compatible NGOs
        List<NGO> compatible = filterCompatibleNGOs(item);
        if (compatible.isEmpty()) return allocations;

        // Step 2: Resolve conflicts to rank NGOs (Using Trust + Fairness)
        List<NGO> ranked = resolveConflict(item, compatible);

        // Step 3: Greedy priority-based distribution
        int remaining = item.getRemainingQuantityNum();

        for (NGO ngo : ranked) {
            if (remaining <= 0) break;

            // Load Balancing: Check if NGO is overloaded (Feature 5)
            if (ngo.getCurrentLoad() >= ngo.getMaxCapacity()) continue;

            List<ResourceRequest> ngoReqs = requestRepo.findByNgoId(ngo.getId())
                .stream()
                .filter(r -> "Pending".equals(r.getStatus()) || "PartiallyFulfilled".equals(r.getStatus()))
                .filter(r -> r.getResourceType() != null 
                    && r.getResourceType().equalsIgnoreCase(item.getItemType()))
                .collect(Collectors.toList());

            for (ResourceRequest req : ngoReqs) {
                if (remaining <= 0) break;

                int allocQty = Math.min(remaining, req.getRemainingNeed());
                allocQty = Math.min(allocQty, ngo.getAvailableCapacity());

                if (allocQty <= 0) continue;

                Allocation alloc = new Allocation();
                alloc.setItemId(item.getId());
                alloc.setNgoId(ngo.getId());
                alloc.setRequestId(req.getId());
                alloc.setAllocatedQuantity(allocQty);
                alloc.setCurrentState(LifecycleState.MATCHED);
                
                // Rollback snapshots
                alloc.setPreviousItemStatus(item.getStatus());
                alloc.setPreviousRemainingQuantity(item.getRemainingQuantityNum());
                alloc.setPreviousNgoLoad(ngo.getCurrentLoad());

                // Update entities
                remaining -= allocQty;
                item.setRemainingQuantityNum(remaining);
                ngo.setCurrentLoad(ngo.getCurrentLoad() + allocQty);
                ngo.setTotalReceived(ngo.getTotalReceived() + allocQty);

                int newNeed = req.getRemainingNeed() - allocQty;
                req.setRemainingNeed(newNeed);
                req.setStatus(newNeed <= 0 ? "Fulfilled" : "PartiallyFulfilled");

                // Feature 13: Track impact
                analyticsService.recordImpact(ngo.getId(), allocQty, item.getItemType());

                allocations.add(alloc);
                ngoRepo.save(ngo);
                requestRepo.save(req);

                for (NotificationObserver obs : observers) {
                    obs.onMatchFound(alloc, item);
                }
            }
        }

        item.setStatus(remaining <= 0 ? "FullyAllocated" : "PartiallyAllocated");
        if (remaining <= 0) item.setLifecycleState(LifecycleState.MATCHED);
        
        itemRepo.save(item);
        allocationRepo.saveAll(allocations);

        return allocations;
    }

    /**
     * Feature 14: Smart Reallocation Engine.
     * If an NGO rejects, automatically assign to next best match.
     */
    public void reallocate(Allocation failedAllocation) {
        Optional<Item> itemOpt = itemRepo.findById(failedAllocation.getItemId());
        if (itemOpt.isPresent()) {
            Item item = itemOpt.get();
            item.setRemainingQuantityNum(item.getRemainingQuantityNum() + failedAllocation.getAllocatedQuantity());
            
            // Mark NGO trust down
            trustService.updateNgoTrustOnRejection(failedAllocation.getNgoId());
            
            // Trigger fresh matching
            splitAndAllocate(item);
        }
    }

    // ===========================
    // Feature 20: ADAPTIVE THRESHOLD
    // ===========================

    /**
     * Adjusts minimum acceptable quantity thresholds based on
     * current supply-demand ratio.
     * 
     * If demand >> supply, thresholds are lowered to accept smaller donations.
     * If supply >> demand, thresholds are raised to reduce administrative overhead.
     */
    public void updateAdaptiveThresholds() {
        for (String type : Arrays.asList("Food", "Clothes", "Essentials")) {
            long supply = itemRepo.findAll().stream()
                .filter(i -> type.equals(i.getItemType()) && ("Available".equals(i.getStatus()) || "PartiallyAllocated".equals(i.getStatus())))
                .count();
            long demand = requestRepo.findByResourceType(type).stream()
                .filter(r -> "Pending".equals(r.getStatus()) || "PartiallyFulfilled".equals(r.getStatus()))
                .count();

            int threshold;
            if (demand > supply * 2) {
                threshold = 1;  // Accept any quantity
            } else if (demand > supply) {
                threshold = 3;
            } else {
                threshold = 10;
            }

            adaptiveThresholds.put(type, threshold);
        }
    }

    public Map<String, Integer> getAdaptiveThresholds() {
        return Collections.unmodifiableMap(adaptiveThresholds);
    }

    // ===========================
    // Feature 11: RECOMMENDATION ENGINE
    // ===========================

    /**
     * Rule-Based Donation Recommendation Engine.
     * 
     * Analyzes current demand vs supply to identify high-demand gaps.
     * Returns a map of resource type -> deficit count.
     * 
     * Logic: For each resource type, deficit = totalRequested - totalAvailable
     */
    public Map<String, Integer> getRecommendations() {
        Map<String, Integer> recommendations = new LinkedHashMap<>();

        for (String type : Arrays.asList("Food", "Clothes", "Essentials")) {
            int available = itemRepo.findAll().stream()
                .filter(i -> type.equals(i.getItemType()) && ("Available".equals(i.getStatus()) || "PartiallyAllocated".equals(i.getStatus())))
                .mapToInt(Item::getRemainingQuantityNum)
                .sum();

            int requested = requestRepo.findByResourceType(type).stream()
                .filter(r -> "Pending".equals(r.getStatus()) || "PartiallyFulfilled".equals(r.getStatus()))
                .mapToInt(ResourceRequest::getRemainingNeed)
                .sum();

            int deficit = requested - available;
            if (deficit > 0) {
                recommendations.put(type, deficit);
            }
        }

        // Sort by deficit descending (highest demand first)
        return recommendations.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .collect(Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue,
                (e1, e2) -> e1, LinkedHashMap::new
            ));
    }

    /**
     * Feature 7: Get the full compatibility matrix.
     */
    public Map<String, Set<String>> getCompatibilityMatrix() {
        return Collections.unmodifiableMap(compatibilityMatrix);
    }
}
