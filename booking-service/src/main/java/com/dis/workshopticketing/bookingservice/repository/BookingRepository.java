package com.dis.workshopticketing.bookingservice.repository;

import com.dis.workshopticketing.bookingservice.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    List<Booking> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
