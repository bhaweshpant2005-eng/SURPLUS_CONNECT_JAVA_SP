package com.example.surplusconnect.service;

import com.example.surplusconnect.model.*;
import com.example.surplusconnect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lifecycle Manager implementing:
 * 
 * Feature 13: Transaction Rollback Mechanism (Command/Saga Pattern)
 * Feature 16: Resource Lifecycle Management (Finite State Machine)
 * 
 * Valid FSM transitions:
 *   REGISTERED -> MATCHED -> IN_TRANSIT -> DELIVERED -> CONSUMED
 *   Any state  -> EXPIRED
 *   MATCHED    -> REGISTERED  (rollback)
 */
@Service
public class LifecycleManager {

    private final ItemRepository itemRepo;
    private final NGORepository ngoRepo;
    private final AllocationRepository allocationRepo;
    private final ResourceRequestRepository requestRepo;
    private final TrustService trustService;

    // Valid state transitions map (FSM definition)
    private final Map<LifecycleState, Set<LifecycleState>> validTransitions = new HashMap<>();

    public LifecycleManager(ItemRepository itemRepo, NGORepository ngoRepo,
                            AllocationRepository allocationRepo,
                            ResourceRequestRepository requestRepo,
                            TrustService trustService) {
        this.itemRepo = itemRepo;
        this.ngoRepo = ngoRepo;
        this.allocationRepo = allocationRepo;
        this.requestRepo = requestRepo;
        this.trustService = trustService;
        initializeTransitions();
    }

    /**
     * Defines the FSM transition table.
     * Each entry maps a source state to its valid target states.
     */
    private void initializeTransitions() {
        validTransitions.put(LifecycleState.PENDING,
            new HashSet<>(Arrays.asList(LifecycleState.MATCHED, LifecycleState.EXPIRED)));
        validTransitions.put(LifecycleState.MATCHED,
            new HashSet<>(Arrays.asList(LifecycleState.CONFIRMED, LifecycleState.REJECTED, LifecycleState.PENDING, LifecycleState.EXPIRED)));
        validTransitions.put(LifecycleState.CONFIRMED,
            new HashSet<>(Arrays.asList(LifecycleState.DISPATCHED, LifecycleState.REJECTED, LifecycleState.EXPIRED)));
        validTransitions.put(LifecycleState.DISPATCHED,
            new HashSet<>(Arrays.asList(LifecycleState.DELIVERED, LifecycleState.EXPIRED)));
        validTransitions.put(LifecycleState.DELIVERED, Collections.emptySet());
        validTransitions.put(LifecycleState.REJECTED,
            new HashSet<>(Arrays.asList(LifecycleState.PENDING)));
        validTransitions.put(LifecycleState.EXPIRED, Collections.emptySet());
    }

    /**
     * Feature 16: Transition an item to a new lifecycle state.
     * Validates the transition against the FSM before applying.
     * 
     * @throws IllegalStateException if the transition is invalid
     */
    public Item transitionState(Long itemId, LifecycleState newState) {
        Item item = itemRepo.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

        LifecycleState currentState = item.getLifecycleState();
        Set<LifecycleState> allowed = validTransitions.getOrDefault(currentState, Collections.emptySet());

        if (!allowed.contains(newState)) {
            throw new IllegalStateException(
                "Invalid transition: " + currentState + " -> " + newState
                + ". Allowed: " + allowed
            );
        }

        item.setLifecycleState(newState);

        // Update status string to match lifecycle
        switch (newState) {
            case MATCHED:    item.setStatus("Matched"); break;
            case CONFIRMED:  item.setStatus("Confirmed"); break;
            case DISPATCHED: item.setStatus("Dispatched"); break;
            case DELIVERED:  
                item.setStatus("Delivered"); 
                // Track success
                if (item.getDonorId() != null) trustService.updateUserTrust(item.getDonorId(), true);
                break;
            case REJECTED:   item.setStatus("Rejected"); break;
            case EXPIRED:    item.setStatus("Expired"); break;
            default:         item.setStatus("Available"); break;
        }

        return itemRepo.save(item);
    }

    /**
     * Feature 13: Transaction Rollback Mechanism.
     */
    @Transactional
    public Allocation rollbackAllocation(Long allocationId) {
        Allocation alloc = allocationRepo.findById(allocationId)
            .orElseThrow(() -> new RuntimeException("Allocation not found: " + allocationId));

        if (alloc.isRolledBack()) {
            throw new IllegalStateException("Allocation already rolled back: " + allocationId);
        }

        // Restore Item state
        Item item = itemRepo.findById(alloc.getItemId())
            .orElseThrow(() -> new RuntimeException("Item not found: " + alloc.getItemId()));
        item.setRemainingQuantityNum(alloc.getPreviousRemainingQuantity());
        item.setStatus(alloc.getPreviousItemStatus() != null ? alloc.getPreviousItemStatus() : "Available");
        item.setLifecycleState(LifecycleState.PENDING);
        itemRepo.save(item);

        // Restore NGO capacity
        Optional<NGO> ngoOpt = ngoRepo.findById(alloc.getNgoId());
        if (ngoOpt.isPresent()) {
            NGO ngo = ngoOpt.get();
            ngo.setCurrentLoad(alloc.getPreviousNgoLoad());
            ngo.setTotalReceived(Math.max(0, ngo.getTotalReceived() - alloc.getAllocatedQuantity()));
            ngoRepo.save(ngo);
            
            // Mark NGO trust down
            trustService.updateNgoTrustOnRejection(ngo.getId());
        }

        // Restore Request status
        if (alloc.getRequestId() != null) {
            Optional<ResourceRequest> reqOpt = requestRepo.findById(alloc.getRequestId());
            if (reqOpt.isPresent()) {
                ResourceRequest req = reqOpt.get();
                req.setStatus("Pending");
                requestRepo.save(req);
            }
        }

        // Mark allocation as rolled back
        alloc.setRolledBack(true);
        alloc.setCurrentState(LifecycleState.REJECTED);
        return allocationRepo.save(alloc);
    }

    /**
     * Get the lifecycle history of an item based on its allocations.
     */
    public Map<String, Object> getItemLifecycle(Long itemId) {
        Item item = itemRepo.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

        List<Allocation> allocations = allocationRepo.findByItemId(itemId);

        Map<String, Object> lifecycle = new LinkedHashMap<>();
        lifecycle.put("itemId", item.getId());
        lifecycle.put("currentState", item.getLifecycleState());
        lifecycle.put("status", item.getStatus());
        lifecycle.put("totalAllocations", allocations.size());
        lifecycle.put("activeAllocations", allocations.stream()
            .filter(a -> !a.isRolledBack()).count());
        lifecycle.put("rolledBackAllocations", allocations.stream()
            .filter(Allocation::isRolledBack).count());

        return lifecycle;
    }

    /**
     * Checks for expired items based on expiry date and transitions them.
     */
    public List<Item> checkAndExpireItems() {
        List<Item> expired = new ArrayList<>();
        List<Item> allItems = itemRepo.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Item item : allItems) {
            if (item.getExpiryDate() != null && !item.getExpiryDate().isEmpty()
                && item.getLifecycleState() != LifecycleState.EXPIRED
                && item.getLifecycleState() != LifecycleState.DELIVERED) {
                try {
                    LocalDateTime expiry = LocalDateTime.parse(item.getExpiryDate() + "T23:59:59");
                    if (now.isAfter(expiry)) {
                        item.setLifecycleState(LifecycleState.EXPIRED);
                        item.setStatus("Expired");
                        itemRepo.save(item);
                        expired.add(item);
                    }
                } catch (Exception e) {
                    // Skip items with invalid date formats
                }
            }
        }

        return expired;
    }
}
