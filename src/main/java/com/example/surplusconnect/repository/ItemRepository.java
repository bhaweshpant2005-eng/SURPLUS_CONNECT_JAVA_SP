package com.example.surplusconnect.repository;

import com.example.surplusconnect.model.Item;
import com.example.surplusconnect.model.LifecycleState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByStatus(String status);
    List<Item> findByItemType(String itemType);
    List<Item> findByLifecycleState(LifecycleState state);
    Optional<Item> findByContentHash(String contentHash);
    List<Item> findByDependencyGroup(String dependencyGroup);
}
