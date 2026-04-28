package com.example.surplusconnect.controller;

import com.example.surplusconnect.model.NGO;
import com.example.surplusconnect.service.NGOService;
import com.example.surplusconnect.service.MatchingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ngos")
@CrossOrigin
public class NGOController {

    private final NGOService ngoService;
    private final MatchingService matchingService;

    public NGOController(NGOService ngoService, MatchingService matchingService) {
        this.ngoService = ngoService;
        this.matchingService = matchingService;
    }

    @GetMapping
    public List<NGO> getAll() {
        return ngoService.getAll();
    }

    @PostMapping
    public NGO add(@RequestBody NGO ngo) {
        return ngoService.save(ngo);
    }

    @GetMapping("/{id}")
    public NGO getById(@PathVariable Long id) {
        return ngoService.findById(id)
            .orElseThrow(() -> new RuntimeException("NGO not found: " + id));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        ngoService.delete(id);
    }

    @GetMapping("/category/{category}")
    public List<NGO> getByCategory(@PathVariable String category) {
        return ngoService.findByCategory(category);
    }

    @GetMapping("/cluster/{clusterGroup}")
    public List<NGO> getByCluster(@PathVariable int clusterGroup) {
        return ngoService.findByCluster(clusterGroup);
    }

    /**
     * Feature 12: Trigger K-Means geographic clustering.
     */
    @PostMapping("/cluster/{k}")
    public String clusterNGOs(@PathVariable int k) {
        matchingService.clusterNGOs(k);
        return "NGOs clustered into " + k + " groups successfully.";
    }
}
