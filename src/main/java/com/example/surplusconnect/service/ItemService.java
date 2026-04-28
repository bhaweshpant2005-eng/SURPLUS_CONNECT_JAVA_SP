package com.example.surplusconnect.service;

import com.example.surplusconnect.model.Item;
import com.example.surplusconnect.model.LifecycleState;
import com.example.surplusconnect.repository.ItemRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ItemService {

    private final ItemRepository repo;
    private final MatchingService matchingService;

    public ItemService(ItemRepository repo, MatchingService matchingService) {
        this.repo = repo;
        this.matchingService = matchingService;
    }

    public List<Item> getAll() {
        return repo.findAll();
    }

    /**
     * Saves an item. Does NOT trigger matching — matching is triggered
     * explicitly from ItemController.add() after the initial save.
     * This prevents infinite loops when saving during allocation.
     */
    public Item save(Item item) {
        if (item.getLifecycleState() == null) {
            item.setLifecycleState(LifecycleState.PENDING);
        }
        return repo.save(item);
    }

    /**
     * Saves an item and triggers event-driven matching.
     * Only called on new donation creation, not on updates.
     */
    public Item saveAndMatch(Item item) {
        if (item.getLifecycleState() == null) {
            item.setLifecycleState(LifecycleState.PENDING);
        }
        Item saved = repo.save(item);
        // Feature 9: Event-Driven matching trigger
        matchingService.splitAndAllocate(saved);
        return saved;
    }

    public Optional<Item> findById(Long id) {
        return repo.findById(id);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public List<Item> findByStatus(String status) {
        return repo.findByStatus(status);
    }

    public List<Item> findByItemType(String itemType) {
        return repo.findByItemType(itemType);
    }

    public List<Item> findByLifecycleState(LifecycleState state) {
        return repo.findByLifecycleState(state);
    }

    public Optional<Item> findByContentHash(String hash) {
        return repo.findByContentHash(hash);
    }
}
