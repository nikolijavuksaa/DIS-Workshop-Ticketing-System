package com.dis.workshopticketing.workshopservice.repository;

import com.dis.workshopticketing.workshopservice.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndActiveTrue(Long id);

    boolean existsByNameIgnoreCase(String name);
}
