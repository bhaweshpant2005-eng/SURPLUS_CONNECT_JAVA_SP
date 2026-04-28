package com.example.surplusconnect.service;

import com.example.surplusconnect.model.*;
import com.example.surplusconnect.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Graph-based Allocation Service implementing:
 * 
 * Feature 4:  Multi-Hop Allocation using BFS graph traversal
 * Feature 19: Dependency-Based Allocation using adjacency grouping
 * 
 * Data Structure: Adjacency List representation of NGO transfer network
 * Algorithm: BFS (Breadth-First Search) for shortest hop path
 */
@Service
public class GraphAllocationService {

    private final NGORepository ngoRepo;
    private final AllocationRepository allocationRepo;
    private final ItemRepository itemRepo;

    // Adjacency list: NGO ID -> Set of connected NGO IDs (transfer partners)
    private final Map<Long, Set<Long>> transferGraph = new HashMap<>();

    public GraphAllocationService(NGORepository ngoRepo,
                                   AllocationRepository allocationRepo,
                                   ItemRepository itemRepo) {
        this.ngoRepo = ngoRepo;
        this.allocationRepo = allocationRepo;
        this.itemRepo = itemRepo;
    }

    /**
     * Adds a bidirectional transfer link between two NGOs.
     * This builds the adjacency list for graph traversal.
     */
    public void addTransferLink(Long ngoId1, Long ngoId2) {
        transferGraph.computeIfAbsent(ngoId1, k -> new HashSet<>()).add(ngoId2);
        transferGraph.computeIfAbsent(ngoId2, k -> new HashSet<>()).add(ngoId1);
    }

    /**
     * Feature 4: Multi-Hop Allocation using BFS.
     * 
     * When direct matching is not possible (NGO A has surplus but
     * NGO B needs it and they're not directly connected), BFS finds
     * the shortest transfer chain.
     * 
     * Algorithm: Breadth-First Search
     * - Start from sourceNgoId
     * - Explore neighbors level by level
     * - First to reach targetNgoId gives shortest path
     * 
     * Time complexity: O(V + E) where V = NGOs, E = transfer links
     * Space complexity: O(V) for visited set and queue
     * 
     * @return Ordered list of NGO IDs representing the hop path, or empty if unreachable
     */
    public List<Long> findTransferPath(Long sourceNgoId, Long targetNgoId) {
        if (sourceNgoId.equals(targetNgoId)) {
            return Collections.singletonList(sourceNgoId);
        }

        // BFS with parent tracking for path reconstruction
        Queue<Long> queue = new LinkedList<>();
        Map<Long, Long> parent = new HashMap<>();
        Set<Long> visited = new HashSet<>();

        queue.add(sourceNgoId);
        visited.add(sourceNgoId);
        parent.put(sourceNgoId, null);

        while (!queue.isEmpty()) {
            Long current = queue.poll();

            Set<Long> neighbors = transferGraph.getOrDefault(current, Collections.emptySet());
            for (Long neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parent.put(neighbor, current);
                    queue.add(neighbor);

                    if (neighbor.equals(targetNgoId)) {
                        // Reconstruct path
                        return reconstructPath(parent, targetNgoId);
                    }
                }
            }
        }

        return Collections.emptyList(); // No path found
    }

    /**
     * Reconstructs the BFS path from parent map.
     */
    private List<Long> reconstructPath(Map<Long, Long> parent, Long target) {
        List<Long> path = new ArrayList<>();
        Long current = target;
        while (current != null) {
            path.add(0, current);
            current = parent.get(current);
        }
        return path;
    }

    /**
     * Executes a multi-hop transfer, creating allocation records along the chain.
     * The hop path is stored in each allocation for audit purposes.
     */
    public List<Allocation> executeMultiHopTransfer(Long itemId, Long sourceNgoId, Long targetNgoId, int quantity) {
        List<Long> path = findTransferPath(sourceNgoId, targetNgoId);
        if (path.isEmpty()) {
            return Collections.emptyList();
        }

        List<Allocation> allocations = new ArrayList<>();
        String hopPathStr = path.stream()
            .map(id -> "NGO-" + id)
            .reduce((a, b) -> a + " -> " + b)
            .orElse("");

        // Create allocation for final destination
        Allocation alloc = new Allocation();
        alloc.setItemId(itemId);
        alloc.setNgoId(targetNgoId);
        alloc.setAllocatedQuantity(quantity);
        alloc.setCurrentState(LifecycleState.DISPATCHED);
        alloc.setHopPath(hopPathStr);
        allocationRepo.save(alloc);
        allocations.add(alloc);

        return allocations;
    }

    /**
     * Feature 19: Dependency-Based Allocation.
     * 
     * Groups items by their dependencyGroup field and allocates
     * related items together to the same NGO.
     * 
     * Example: "Rice" and "Pulses" in group "DailyMeal" are sent together.
     * 
     * @return Map of dependencyGroup -> List of Items
     */
    public Map<String, List<Item>> getDependencyGroups() {
        List<Item> allItems = itemRepo.findAll();
        Map<String, List<Item>> groups = new HashMap<>();

        for (Item item : allItems) {
            if (item.getDependencyGroup() != null && !item.getDependencyGroup().isEmpty()) {
                groups.computeIfAbsent(item.getDependencyGroup(), k -> new ArrayList<>())
                      .add(item);
            }
        }

        return groups;
    }

    /**
     * Gets the full transfer graph for visualization/debugging.
     */
    public Map<Long, Set<Long>> getTransferGraph() {
        return Collections.unmodifiableMap(transferGraph);
    }
}
