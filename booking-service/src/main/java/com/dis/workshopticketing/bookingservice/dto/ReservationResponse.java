package com.dis.workshopticketing.bookingservice.dto;

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
}
