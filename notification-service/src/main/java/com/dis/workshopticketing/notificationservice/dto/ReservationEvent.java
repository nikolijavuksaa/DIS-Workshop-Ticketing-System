package com.dis.workshopticketing.notificationservice.dto;

import com.dis.workshopticketing.notificationservice.model.ReservationStatus;

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
}
