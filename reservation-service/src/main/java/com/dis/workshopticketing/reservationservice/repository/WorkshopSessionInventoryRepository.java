package com.dis.workshopticketing.reservationservice.repository;

import com.dis.workshopticketing.reservationservice.model.WorkshopSessionInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkshopSessionInventoryRepository extends JpaRepository<WorkshopSessionInventory, Long> {

    Optional<WorkshopSessionInventory> findByWorkshopSessionId(Long workshopSessionId);

    boolean existsByWorkshopSessionId(Long workshopSessionId);
}
