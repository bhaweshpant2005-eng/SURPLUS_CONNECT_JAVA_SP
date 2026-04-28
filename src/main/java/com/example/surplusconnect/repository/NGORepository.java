package com.example.surplusconnect.repository;

import com.example.surplusconnect.model.NGO;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NGORepository extends JpaRepository<NGO, Long> {
    List<NGO> findByClusterGroup(int clusterGroup);
    List<NGO> findByNgoCategory(String ngoCategory);
    List<NGO> findByAcceptedTypesContaining(String type);
}
