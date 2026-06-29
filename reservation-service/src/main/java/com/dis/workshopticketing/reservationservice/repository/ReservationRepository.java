package com.dis.workshopticketing.reservationservice.repository;

import com.dis.workshopticketing.reservationservice.model.Reservation;
import com.dis.workshopticketing.reservationservice.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    List<Reservation> findAllByWorkshopSessionId(Long workshopSessionId);

    List<Reservation> findAllByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime expiresAt);

    Optional<Reservation> findFirstByWorkshopSessionIdAndStatusOrderByCreatedAtAsc(
            Long workshopSessionId,
            ReservationStatus status
    );
}
