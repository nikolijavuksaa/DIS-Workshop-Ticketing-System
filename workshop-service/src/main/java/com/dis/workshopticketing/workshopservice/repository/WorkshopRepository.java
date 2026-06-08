package com.dis.workshopticketing.workshopservice.repository;

import com.dis.workshopticketing.workshopservice.model.Workshop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkshopRepository extends JpaRepository<Workshop, Long> {

    Optional<Workshop> findByIdAndActiveTrue(Long id);

    boolean existsByCategoryIdAndActiveTrue(Long categoryId);
}
