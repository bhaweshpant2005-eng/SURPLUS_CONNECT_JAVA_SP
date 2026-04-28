package com.example.surplusconnect.service;

import com.example.surplusconnect.model.ResourceRequest;
import com.example.surplusconnect.repository.ResourceRequestRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Resource Request CRUD Service.
 */
@Service
public class ResourceRequestService {

    private final ResourceRequestRepository requestRepo;

    public ResourceRequestService(ResourceRequestRepository requestRepo) {
        this.requestRepo = requestRepo;
    }

    public List<ResourceRequest> getAll() {
        return requestRepo.findAll();
    }

    public ResourceRequest save(ResourceRequest request) {
        return requestRepo.save(request);
    }

    public Optional<ResourceRequest> findById(Long id) {
        return requestRepo.findById(id);
    }

    public void delete(Long id) {
        requestRepo.deleteById(id);
    }

    public List<ResourceRequest> findByStatus(String status) {
        return requestRepo.findByStatus(status);
    }

    public List<ResourceRequest> findByNgoId(Long ngoId) {
        return requestRepo.findByNgoId(ngoId);
    }
}
