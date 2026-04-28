package com.example.surplusconnect.service;

import com.example.surplusconnect.model.NGO;
import com.example.surplusconnect.repository.NGORepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * NGO Service for basic CRUD and management operations.
 */
@Service
public class NGOService {

    private final NGORepository ngoRepo;

    public NGOService(NGORepository ngoRepo) {
        this.ngoRepo = ngoRepo;
    }

    public List<NGO> getAll() {
        return ngoRepo.findAll();
    }

    public NGO save(NGO ngo) {
        return ngoRepo.save(ngo);
    }

    public Optional<NGO> findById(Long id) {
        return ngoRepo.findById(id);
    }

    public void delete(Long id) {
        ngoRepo.deleteById(id);
    }

    public List<NGO> findByCategory(String category) {
        return ngoRepo.findByNgoCategory(category);
    }

    public List<NGO> findByCluster(int clusterGroup) {
        return ngoRepo.findByClusterGroup(clusterGroup);
    }

    /**
     * Feature 9: Leaderboard using TreeMap (BST logic).
     * Sorts NGOs based on a calculated performance score.
     */
    public List<NGO> getNGOLeaderboard() {
        List<NGO> allNgos = ngoRepo.findAll();
        
        // TreeMap to sort by score in descending order
        TreeMap<Double, List<NGO>> leaderboardMap = new TreeMap<>(Collections.reverseOrder());

        for (NGO ngo : allNgos) {
            // Calculate a performance score based on rating and total received
            double score = (ngo.getRating() * 10) + (ngo.getTotalReceived() * 0.5);
            
            leaderboardMap.computeIfAbsent(score, k -> new ArrayList<>()).add(ngo);
        }

        // Flatten the map into a sorted list
        List<NGO> rankedNGOs = new ArrayList<>();
        for (List<NGO> ngosWithSameScore : leaderboardMap.values()) {
            rankedNGOs.addAll(ngosWithSameScore);
        }
        
        return rankedNGOs;
    }
}
