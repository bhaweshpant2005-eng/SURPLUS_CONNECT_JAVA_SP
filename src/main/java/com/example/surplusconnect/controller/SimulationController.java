package com.example.surplusconnect.controller;

import com.example.surplusconnect.service.SimulationService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Simulation Controller (Feature 17).
 * Exposes endpoints for generating test data and running stress tests.
 */
@RestController
@RequestMapping("/api/simulation")
@CrossOrigin
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /**
     * Generate synthetic test data.
     * 
     * Example: POST /api/simulation/generate?donations=100&ngos=20&requests=200
     */
    @PostMapping("/generate")
    public Map<String, Object> generateData(
            @RequestParam(defaultValue = "100") int donations,
            @RequestParam(defaultValue = "20") int ngos,
            @RequestParam(defaultValue = "200") int requests) {
        return simulationService.generateTestData(donations, ngos, requests);
    }

    /**
     * Run stress test on all matching algorithms.
     * Returns execution times in milliseconds.
     */
    @PostMapping("/stress-test")
    public Map<String, Object> stressTest() {
        return simulationService.runStressTest();
    }
}
