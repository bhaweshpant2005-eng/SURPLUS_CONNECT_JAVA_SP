package com.example.surplusconnect.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.surplusconnect.model.Item;
import com.example.surplusconnect.model.LifecycleState;
import com.example.surplusconnect.repository.UserRepository;
import com.example.surplusconnect.service.ItemService;
import com.example.surplusconnect.service.MatchingService;

/**
 * Enhanced Item Controller supporting:
 * - Standard CRUD operations
 * - Feature 14: Duplicate detection on donation submission
 * - Feature 22: Image upload for visual verification
 */
@RestController
@RequestMapping("/api/items")
@CrossOrigin
public class ItemController {

    private static final Logger log = LoggerFactory.getLogger(ItemController.class);

    private final ItemService service;
    private final MatchingService matchingService;
    private final UserRepository userRepository;

    private static final String UPLOAD_DIR = "uploads/";
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");

    public ItemController(ItemService service, MatchingService matchingService, UserRepository userRepository) {
        this.service = service;
        this.matchingService = matchingService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Item> getAll() {
        return service.getAll();
    }

    /**
     * Creates a new donation with duplicate detection (Feature 14)
     * and lifecycle initialization (Feature 16).
     * Triggers event-driven matching after save.
     */
    @PostMapping
    public ResponseEntity<?> add(@RequestBody Item item) {
        // Link to authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            userRepository.findByUsername(auth.getName()).ifPresent(user -> {
                item.setDonorId(user.getId());
                if (item.getDonorName() == null || item.getDonorName().isEmpty()) {
                    item.setDonorName(user.getUsername());
                }
            });
        }

        // Feature 16: Initialize lifecycle state
        item.setLifecycleState(LifecycleState.PENDING);
        if (item.getStatus() == null || item.getStatus().isEmpty()) {
            item.setStatus("Available");
        }

        // Default lat/lon to 0.0 if not provided (avoids NOT NULL DB constraint)
        if (item.getLatitude() == null || item.getLatitude() == 0.0) item.setLatitude(0.0);
        if (item.getLongitude() == null || item.getLongitude() == 0.0) item.setLongitude(0.0);

        // Feature 1: Initialize splitting quantities
        try {
            int qty = Integer.parseInt(item.getQuantity().replaceAll("[^0-9]", ""));
            item.setOriginalQuantityNum(qty);
            item.setRemainingQuantityNum(qty);
        } catch (Exception e) {
            item.setOriginalQuantityNum(1);
            item.setRemainingQuantityNum(1);
        }

        // Feature 14: Generate content hash for duplicate detection
        String hash = matchingService.generateDonationHash(item);
        item.setContentHash(hash);

        if (matchingService.isDuplicateDonation(item)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Duplicate donation detected. A similar item was already registered.");
        }

        // Save and trigger matching
        Item saved = service.saveAndMatch(item);
        log.info("Donation registered: id={}, type={}, qty={}", saved.getId(), saved.getItemType(), saved.getQuantity());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return service.findById(id)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Item not found: " + id));
    }

    /**
     * Feature 22: Image Upload for Donation Requests.
     * Validates file type before saving. Stores image on disk.
     */
    @PostMapping("/{id}/upload-image")
    public ResponseEntity<?> uploadImage(@PathVariable Long id,
                                          @RequestParam("file") MultipartFile file) throws IOException {
        Item item = service.findById(id)
            .orElse(null);
        if (item == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Item not found: " + id);
        }

        // Validate file extension (security: prevent executable uploads)
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String extension = originalName.contains(".")
            ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase()
            : "";

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Invalid file type. Allowed: jpg, jpeg, png, gif, webp");
        }

        // Create uploads directory if it doesn't exist
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Generate unique filename to avoid collisions
        String uniqueName = "item_" + id + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path filePath = Paths.get(UPLOAD_DIR + uniqueName);
        Files.write(filePath, file.getBytes());

        // Update item with image URL (use plain save — no re-matching needed)
        item.setImageUrl("/uploads/" + uniqueName);
        Item saved = service.save(item);
        log.info("Image uploaded for item id={}: {}", id, uniqueName);
        return ResponseEntity.ok(saved);
    }
}
