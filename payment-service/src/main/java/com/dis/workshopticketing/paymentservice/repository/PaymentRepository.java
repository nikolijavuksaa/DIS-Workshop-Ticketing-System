package com.dis.workshopticketing.paymentservice.repository;

import com.dis.workshopticketing.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdAndUserId(Long id, Long userId);

    List<Payment> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
