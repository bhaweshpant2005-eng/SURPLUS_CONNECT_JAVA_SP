package com.example.surplusconnect.service;

import com.example.surplusconnect.model.*;
import com.example.surplusconnect.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulation and Stress Testing Module (Feature 17).
 * 
 * Uses Monte Carlo-style random generation to create synthetic
 * donations and requests for load testing the matching algorithms.
 * 
 * Measures:
 * - Matching algorithm execution time
 * - Memory usage under load
 * - Throughput (allocations per second)
 */
@Service
public class SimulationService {

    private final ItemRepository itemRepo;
    private final NGORepository ngoRepo;
    private final ResourceRequestRepository requestRepo;
    private final MatchingService matchingService;

    private static final String[] ITEM_TYPES = {"Food", "Clothes", "Essentials"};
    private static final String[] FOOD_CATEGORIES = {"Cooked Meals", "Fresh Vegetables", "Packaged Food", "Dairy Products"};
    private static final String[] CLOTHES_CATEGORIES = {"Winter Wear", "Summer Clothes", "Formal Wear", "Children Clothes"};
    private static final String[] ESSENTIAL_CATEGORIES = {"Medicine", "Books", "Electronics", "Household Items"};
    private static final String[] LOCATIONS = {"Mumbai", "Delhi", "Bangalore", "Chennai", "Pune", "Hyderabad", "Kolkata"};
    private static final String[] NGO_CATEGORIES = {"Children's Home", "Shelter", "Hospital", "Food Bank", "School"};
    private static final String[] DONOR_NAMES = {"Rajesh", "Priya", "Amit", "Sneha", "Vikram", "Neha", "Arjun"};

    public SimulationService(ItemRepository itemRepo, NGORepository ngoRepo,
                             ResourceRequestRepository requestRepo,
                             MatchingService matchingService) {
        this.itemRepo = itemRepo;
        this.ngoRepo = ngoRepo;
        this.requestRepo = requestRepo;
        this.matchingService = matchingService;
    }

    /**
     * Generates synthetic data for load testing.
     * 
     * @param numDonations Number of donation items to generate
     * @param numNGOs      Number of NGOs to generate
     * @param numRequests  Number of resource requests to generate
     * @return Summary of generated data
     */
    public Map<String, Object> generateTestData(int numDonations, int numNGOs, int numRequests) {
        List<Item> items = new ArrayList<>();
        List<NGO> ngos = new ArrayList<>();
        List<ResourceRequest> requests = new ArrayList<>();

        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Generate NGOs
        for (int i = 0; i < numNGOs; i++) {
            NGO ngo = new NGO();
            ngo.setName("Test NGO " + (i + 1));
            ngo.setContactPerson("Contact " + (i + 1));
            ngo.setPhone("9" + String.format("%09d", rng.nextInt(100000000, 999999999)));
            ngo.setLocation(LOCATIONS[rng.nextInt(LOCATIONS.length)]);
            ngo.setLatitude(18.0 + rng.nextDouble() * 10);
            ngo.setLongitude(72.0 + rng.nextDouble() * 10);
            ngo.setMaxCapacity(rng.nextInt(50, 500));
            ngo.setCurrentLoad(0);
            ngo.setNgoCategory(NGO_CATEGORIES[rng.nextInt(NGO_CATEGORIES.length)]);
            ngo.setAcceptedTypes(String.join(",", ITEM_TYPES));
            ngo.setMaxDistance(rng.nextDouble(10, 100));
            ngo.setRating(3.0 + rng.nextDouble() * 2);
            ngos.add(ngo);
        }
        ngoRepo.saveAll(ngos);

        // Generate Donations
        for (int i = 0; i < numDonations; i++) {
            Item item = new Item();
            String type = ITEM_TYPES[rng.nextInt(ITEM_TYPES.length)];
            item.setDonorName(DONOR_NAMES[rng.nextInt(DONOR_NAMES.length)]);
            item.setDonorPhone("9" + String.format("%09d", rng.nextInt(100000000, 999999999)));
            item.setItemType(type);
            item.setCategory(getCategoryForType(type, rng));
            int qty = rng.nextInt(5, 200);
            item.setQuantity(String.valueOf(qty));
            item.setOriginalQuantityNum(qty);
            item.setRemainingQuantityNum(qty);
            item.setLocation(LOCATIONS[rng.nextInt(LOCATIONS.length)]);
            item.setLatitude(18.0 + rng.nextDouble() * 10);
            item.setLongitude(72.0 + rng.nextDouble() * 10);
            item.setStatus("Available");
            item.setLifecycleState(LifecycleState.PENDING);
            item.setTimestamp(LocalDateTime.now().toString());
            item.setPriority(type.equals("Food") ? "High" : "Medium");
            item.setImageUrl("https://example.com/item" + i + ".jpg"); // For quality verification
            item.setContentHash(matchingService.generateDonationHash(item));
            items.add(item);
        }
        itemRepo.saveAll(items);

        // Generate Requests (linked to created NGOs)
        List<NGO> savedNGOs = ngoRepo.findAll();
        for (int i = 0; i < numRequests; i++) {
            ResourceRequest req = new ResourceRequest();
            NGO randomNgo = savedNGOs.get(rng.nextInt(savedNGOs.size()));
            req.setNgoId(randomNgo.getId());
            req.setNgoName(randomNgo.getName());
            req.setContactPerson(randomNgo.getContactPerson());
            req.setPhone(randomNgo.getPhone());
            String type = ITEM_TYPES[rng.nextInt(ITEM_TYPES.length)];
            req.setResourceType(type);
            req.setCategory(getCategoryForType(type, rng));
            req.setQuantityRequested(rng.nextInt(5, 100));
            req.setLocation(randomNgo.getLocation());
            UrgencyLevel[] levels = UrgencyLevel.values();
            req.setUrgencyLevel(levels[rng.nextInt(levels.length)]);
            req.setStatus("Pending");
            req.setRequestHash(matchingService.generateRequestHash(req));
            requests.add(req);
        }
        requestRepo.saveAll(requests);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("donationsGenerated", numDonations);
        summary.put("ngosGenerated", numNGOs);
        summary.put("requestsGenerated", numRequests);
        return summary;
    }

    /**
     * Runs a stress test on the matching algorithm.
     * Measures execution time for priority recalculation and matching.
     */
    public Map<String, Object> runStressTest() {
        Map<String, Object> results = new LinkedHashMap<>();

        // Test 1: Priority Recalculation
        long startPriority = System.nanoTime();
        matchingService.recalculatePriorities();
        long endPriority = System.nanoTime();
        results.put("priorityRecalculation_ms", (endPriority - startPriority) / 1_000_000.0);

        // Test 2: Geographic Clustering
        long startCluster = System.nanoTime();
        matchingService.clusterNGOs(5);
        long endCluster = System.nanoTime();
        results.put("geographicClustering_ms", (endCluster - startCluster) / 1_000_000.0);

        // Test 3: Matching (split and allocate for all available items)
        long startMatch = System.nanoTime();
        List<Item> availableItems = itemRepo.findAll().stream()
            .filter(i -> "Available".equals(i.getStatus()) && i.getRemainingQuantityNum() > 0)
            .toList();
        int totalAllocations = 0;
        for (Item item : availableItems) {
            List<Allocation> allocs = matchingService.splitAndAllocate(item);
            totalAllocations += allocs.size();
        }
        long endMatch = System.nanoTime();
        results.put("matching_ms", (endMatch - startMatch) / 1_000_000.0);
        results.put("totalAllocationsCreated", totalAllocations);
        results.put("itemsProcessed", availableItems.size());

        // Test 4: Recommendation generation
        long startRec = System.nanoTime();
        matchingService.getRecommendations();
        long endRec = System.nanoTime();
        results.put("recommendations_ms", (endRec - startRec) / 1_000_000.0);

        // Memory usage
        Runtime runtime = Runtime.getRuntime();
        results.put("usedMemory_MB", (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0));
        results.put("totalMemory_MB", runtime.totalMemory() / (1024.0 * 1024.0));

        return results;
    }

    private String getCategoryForType(String type, ThreadLocalRandom rng) {
        return switch (type) {
            case "Food" -> FOOD_CATEGORIES[rng.nextInt(FOOD_CATEGORIES.length)];
            case "Clothes" -> CLOTHES_CATEGORIES[rng.nextInt(CLOTHES_CATEGORIES.length)];
            case "Essentials" -> ESSENTIAL_CATEGORIES[rng.nextInt(ESSENTIAL_CATEGORIES.length)];
            default -> "General";
        };
    }
}
