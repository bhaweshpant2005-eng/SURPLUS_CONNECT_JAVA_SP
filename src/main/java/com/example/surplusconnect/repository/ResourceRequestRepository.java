package com.example.surplusconnect.repository;

import com.example.surplusconnect.model.ResourceRequest;
import com.example.surplusconnect.model.UrgencyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResourceRequestRepository extends JpaRepository<ResourceRequest, Long> {
    List<ResourceRequest> findByStatus(String status);
    List<ResourceRequest> findByStatusIn(List<String> statuses);
    List<ResourceRequest> findByUrgencyLevel(UrgencyLevel urgencyLevel);
    List<ResourceRequest> findByNgoId(Long ngoId);
    List<ResourceRequest> findByRequestHash(String requestHash);
    List<ResourceRequest> findByResourceType(String resourceType);
}
