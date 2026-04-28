package com.example.surplusconnect.repository;

import com.example.surplusconnect.model.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AllocationRepository extends JpaRepository<Allocation, Long> {
    List<Allocation> findByItemId(Long itemId);
    List<Allocation> findByNgoId(Long ngoId);
    List<Allocation> findByRequestId(Long requestId);
    List<Allocation> findByRolledBack(boolean rolledBack);
}
