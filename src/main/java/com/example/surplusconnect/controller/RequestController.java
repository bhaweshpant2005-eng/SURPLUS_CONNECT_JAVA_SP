package com.example.surplusconnect.controller;

import com.example.surplusconnect.model.*;
import com.example.surplusconnect.repository.ItemRepository;
import com.example.surplusconnect.repository.NGORepository;
import com.example.surplusconnect.repository.UserRepository;
import com.example.surplusconnect.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Request Controller handling:
 * - ResourceRequest CRUD
 * - Duplicate detection (Feature 14)
 * - Priority recalculation trigger (Feature 3)
 * 
 * NOTE: Frontend sends fields as { itemType, quantity, urgency }.
 * These are mapped to { resourceType, quantityRequested, urgencyLevel }.
 */
@RestController
@RequestMapping("/api/requests")
@CrossOrigin
public class RequestController {

    private static final Logger log = LoggerFactory.getLogger(RequestController.class);

    private final ResourceRequestService requestService;
    private final MatchingService matchingService;
    private final UserRepository userRepository;
    private final NGORepository ngoRepository;
    private final ItemRepository itemRepository;

    public RequestController(ResourceRequestService requestService,
                             MatchingService matchingService,
                             UserRepository userRepository,
                             NGORepository ngoRepository,
                             ItemRepository itemRepository) {
        this.requestService = requestService;
        this.matchingService = matchingService;
        this.userRepository = userRepository;
        this.ngoRepository = ngoRepository;
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public List<ResourceRequest> getAll() {
        return requestService.getAll();
    }

    /**
     * Creates a new request with field mapping from frontend format.
     * Frontend sends: itemType, quantity, urgency
     * Backend model:  resourceType, quantityRequested, urgencyLevel
     */
    @PostMapping
    public ResponseEntity<?> add(@RequestBody Map<String, Object> body) {
        ResourceRequest request = new ResourceRequest();

        // Map frontend fields to backend model
        request.setNgoName(getString(body, "ngoName"));
        request.setContactPerson(getString(body, "contactPerson"));
        request.setPhone(getString(body, "phone"));
        request.setLocation(getString(body, "location"));
        request.setCategory(getString(body, "category"));

        // itemType -> resourceType
        String itemType = getString(body, "itemType");
        if (itemType == null) itemType = getString(body, "resourceType");
        request.setResourceType(itemType);

        // quantity -> quantityRequested
        int qty = 1;
        try {
            Object qtyObj = body.get("quantity");
            if (qtyObj == null) qtyObj = body.get("quantityRequested");
            if (qtyObj != null) {
                qty = Integer.parseInt(qtyObj.toString().replaceAll("[^0-9]", ""));
            }
        } catch (Exception ignored) {}
        request.setQuantityRequested(qty);
        request.setRemainingNeed(qty);

        // urgency -> urgencyLevel
        String urgency = getString(body, "urgency");
        if (urgency == null) urgency = getString(body, "urgencyLevel");
        try {
            if (urgency != null) {
                request.setUrgencyLevel(UrgencyLevel.valueOf(urgency.toUpperCase()));
            }
        } catch (IllegalArgumentException e) {
            request.setUrgencyLevel(UrgencyLevel.NORMAL);
        }

        request.setStatus("Pending");

        // Link to authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            userRepository.findByUsername(auth.getName()).ifPresent(user -> {
                request.setNgoId(user.getId());
                if (request.getNgoName() == null || request.getNgoName().isEmpty()) {
                    request.setNgoName(user.getUsername());
                }
            });
        }

        // Generate and check hash for duplicates
        String hash = matchingService.generateRequestHash(request);
        request.setRequestHash(hash);

        if (matchingService.isDuplicateRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Duplicate request detected. A similar request already exists.");
        }

        ResourceRequest saved = requestService.save(request);
        log.info("Request saved: id={}, type={}, qty={}", saved.getId(), saved.getResourceType(), saved.getQuantityRequested());

        // Auto-create NGO record if it doesn't exist for this user, then trigger matching
        if (saved.getNgoId() != null) {
            final Long ngoUserId = saved.getNgoId();
            final ResourceRequest savedReq = saved;

            // Find NGO by ngoId or create one
            NGO ngo = ngoRepository.findById(ngoUserId).orElse(null);
            if (ngo == null) {
                // Check if any NGO has this as a reference
                ngo = new NGO();
                ngo.setName(saved.getNgoName() != null ? saved.getNgoName() : "NGO-" + ngoUserId);
                ngo.setLocation(saved.getLocation());
                ngo.setLatitude(0.0);
                ngo.setLongitude(0.0);
                ngo.setMaxCapacity(10000);
                ngo.setCurrentLoad(0);
                ngo.setTotalReceived(0);
                ngo.setAcceptedTypes("Food,Clothes,Essentials");
                ngo.setMaxDistance(0.0);
                ngo.setTrustScore(75.0);
                ngo.setRating(5.0);
                NGO savedNgo = ngoRepository.save(ngo);
                // Update the request to point to the real NGO id
                saved.setNgoId(savedNgo.getId());
                requestService.save(saved);
                log.info("Auto-created NGO id={} for user id={}", savedNgo.getId(), ngoUserId);
            }

            // Trigger matching on all available items of the requested type
            final String reqType = savedReq.getResourceType();
            itemRepository.findAll().stream()
                .filter(item -> item.getStatus() != null
                    && (item.getStatus().equals("Available") || item.getStatus().equals("PartiallyAllocated"))
                    && reqType != null
                    && reqType.equalsIgnoreCase(item.getItemType())
                    && item.getRemainingQuantityNum() > 0)
                .forEach(matchingService::splitAndAllocate);
        }

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return requestService.findById(id)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Request not found: " + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        requestService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{status}")
    public List<ResourceRequest> getByStatus(@PathVariable String status) {
        return requestService.findByStatus(status);
    }

    @GetMapping("/ngo/{ngoId}")
    public List<ResourceRequest> getByNgoId(@PathVariable Long ngoId) {
        return requestService.findByNgoId(ngoId);
    }

    /**
     * Feature 3: Trigger dynamic re-prioritization for all pending requests.
     */
    @PostMapping("/recalculate-priorities")
    public String recalculatePriorities() {
        matchingService.recalculatePriorities();
        return "Priorities recalculated for all pending requests.";
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
