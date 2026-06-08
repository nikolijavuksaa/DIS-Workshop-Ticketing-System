package com.dis.workshopticketing.identityservice.repository;

import com.dis.workshopticketing.identityservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndActiveTrue(Long id);

    boolean existsByIdAndActiveTrue(Long id);
}
