package com.example.surplusconnect.service;

import com.example.surplusconnect.model.NGO;
import com.example.surplusconnect.model.ResourceRequest;
import com.example.surplusconnect.repository.NGORepository;
import com.example.surplusconnect.repository.ResourceRequestRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics Service implementing:
 * 
 * Feature 3: Predictive Demand (Rule-Based HashMap)
 * Feature 7: Impact Score Engine (TreeMap for ranking)
 * Feature 13: Donation Lifecycle Analytics
 */
@Service
public class AnalyticsService {

    private final NGORepository ngoRepo;
    private final ResourceRequestRepository requestRepo;

    public AnalyticsService(NGORepository ngoRepo, ResourceRequestRepository requestRepo) {
        this.ngoRepo = ngoRepo;
        this.requestRepo = requestRepo;
    }

    /**
     * Feature 3: Predictive Demand System
     * Uses HashMap and frequency counting to track frequently requested items.
     */
    public Map<String, Integer> getFrequentlyRequestedItems() {
        List<ResourceRequest> allRequests = requestRepo.findAll();
        Map<String, Integer> frequencyMap = new HashMap<>();

        for (ResourceRequest req : allRequests) {
            String key = req.getResourceType();
            frequencyMap.put(key, frequencyMap.getOrDefault(key, 0) + 1);
        }

        return frequencyMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }

    /**
     * Feature 7: Impact Score Engine
     * Uses TreeMap for ranking NGOs by impact score.
     */
    public TreeMap<Double, List<NGO>> getNgoImpactRankings() {
        List<NGO> allNgos = ngoRepo.findAll();
        TreeMap<Double, List<NGO>> rankings = new TreeMap<>(Collections.reverseOrder());

        for (NGO ngo : allNgos) {
            // Calculate dynamic impact score: (peopleHelped * 0.6) + (wasteReduced * 0.4)
            double score = (ngo.getPeopleHelped() * 0.6) + (ngo.getWasteReducedKg() * 0.4);
            ngo.setImpactScore(score);
            
            rankings.computeIfAbsent(score, k -> new ArrayList<>()).add(ngo);
        }

        return rankings;
    }

    /**
     * Updates NGO impact metrics after a successful delivery.
     */
    public void recordImpact(Long ngoId, int quantity, String itemType) {
        Optional<NGO> ngoOpt = ngoRepo.findById(ngoId);
        if (ngoOpt.isPresent()) {
            NGO ngo = ngoOpt.get();
            
            // Heuristic for people helped based on resource type
            int peopleHelped = 0;
            double wasteReduced = 0;

            switch (itemType) {
                case "Food":
                    peopleHelped = quantity / 2; // Assume 2 units per person
                    wasteReduced = quantity * 0.5; // kg
                    break;
                case "Clothes":
                    peopleHelped = quantity;
                    wasteReduced = quantity * 0.2;
                    break;
                default:
                    peopleHelped = quantity / 3;
                    wasteReduced = quantity * 0.3;
                    break;
            }

            ngo.setPeopleHelped(ngo.getPeopleHelped() + peopleHelped);
            ngo.setWasteReducedKg(ngo.getWasteReducedKg() + wasteReduced);
            ngoRepo.save(ngo);
        }
    }
}
