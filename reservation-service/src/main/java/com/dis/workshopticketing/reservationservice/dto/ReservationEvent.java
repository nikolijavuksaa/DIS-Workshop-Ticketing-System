package com.dis.workshopticketing.reservationservice.dto;

import com.dis.workshopticketing.reservationservice.model.Reservation;
import com.dis.workshopticketing.reservationservice.model.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationEvent(
        String eventType,
        Long reservationId,
        Long bookingId,
        Long userId,
        Long workshopSessionId,
        ReservationStatus status,
        LocalDateTime occurredAt
) {

    public static ReservationEvent from(String eventType, Reservation reservation, LocalDateTime occurredAt) {
        return new ReservationEvent(
                eventType,
                reservation.getId(),
                reservation.getBookingId(),
                reservation.getUserId(),
                reservation.getWorkshopSessionId(),
                reservation.getStatus(),
                occurredAt
        );
    }
}
