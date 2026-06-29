package com.dis.workshopticketing.bookingservice.dto;

import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
        @NotNull Long workshopSessionId
) {
}
