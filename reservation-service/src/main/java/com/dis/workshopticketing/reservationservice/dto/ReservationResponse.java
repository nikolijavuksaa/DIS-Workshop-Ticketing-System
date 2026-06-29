package com.dis.workshopticketing.reservationservice.dto;

import com.dis.workshopticketing.reservationservice.model.Reservation;
import com.dis.workshopticketing.reservationservice.model.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long bookingId,
        Long userId,
        Long workshopSessionId,
        ReservationStatus status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getBookingId(),
                reservation.getUserId(),
                reservation.getWorkshopSessionId(),
                reservation.getStatus(),
                reservation.getExpiresAt(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}
