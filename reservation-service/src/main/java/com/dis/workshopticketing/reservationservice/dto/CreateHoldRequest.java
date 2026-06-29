package com.dis.workshopticketing.reservationservice.dto;

import jakarta.validation.constraints.NotNull;

public record CreateHoldRequest(
        @NotNull Long bookingId,
        @NotNull Long userId,
        @NotNull Long workshopSessionId
) {
}
