package com.example.surplusconnect.controller;

import com.example.surplusconnect.model.NGO;
import com.example.surplusconnect.repository.AllocationRepository;
import com.example.surplusconnect.repository.ItemRepository;
import com.example.surplusconnect.repository.ResourceRequestRepository;
import com.example.surplusconnect.service.NGOService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    private final ItemRepository itemRepository;
    private final ResourceRequestRepository requestRepository;
    private final AllocationRepository allocationRepository;
    private final NGOService ngoService;

    public AdminController(ItemRepository itemRepository,
                           ResourceRequestRepository requestRepository,
                           AllocationRepository allocationRepository,
                           NGOService ngoService) {
        this.itemRepository = itemRepository;
        this.requestRepository = requestRepository;
        this.allocationRepository = allocationRepository;
        this.ngoService = ngoService;
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        Map<String, Object> stats = new HashMap<>();

        long totalItems = itemRepository.count();
        long totalRequests = requestRepository.count();
        long successfulMatches = allocationRepository.count();

        stats.put("totalDonations", totalItems);
        stats.put("totalRequests", totalRequests);
        stats.put("successfulMatches", successfulMatches);

        // Category distribution — single findAll() call, then stream
        Map<String, Long> categoryDistribution = new HashMap<>();
        itemRepository.findAll().forEach(item -> {
            String type = item.getItemType() != null ? item.getItemType() : "Unknown";
            categoryDistribution.merge(type, 1L, Long::sum);
        });
        stats.put("categoryDistribution", categoryDistribution);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<NGO>> getNGOLeaderboard() {
        return ResponseEntity.ok(ngoService.getNGOLeaderboard());
    }
}
