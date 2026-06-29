package com.dis.workshopticketing.paymentservice.dto;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long userId,
        Long workshopSessionId,
        Long reservationId,
        BookingStatus status,
        LocalDateTime reservationExpiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
