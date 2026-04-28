package com.example.surplusconnect.repository;

import com.example.surplusconnect.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByVehicleIdOrderByStartTimeAsc(String vehicleId);
    List<TimeSlot> findByNgoId(Long ngoId);
    List<TimeSlot> findByStatus(String status);
}
