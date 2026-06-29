package com.dis.workshopticketing.bookingservice.dto;

public record CreateReservationHoldRequest(
        Long bookingId,
        Long userId,
        Long workshopSessionId
) {
}
