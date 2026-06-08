package com.dis.workshopticketing.identityservice.repository;

import com.dis.workshopticketing.identityservice.model.IdentityEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityEmailRepository extends JpaRepository<IdentityEmail, Long> {

    boolean existsByEmail(String email);
}
