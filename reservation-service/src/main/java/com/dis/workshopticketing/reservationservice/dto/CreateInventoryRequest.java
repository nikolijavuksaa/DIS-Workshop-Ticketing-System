package com.dis.workshopticketing.reservationservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateInventoryRequest(
        @NotNull Long workshopSessionId,
        @NotNull @Positive Integer totalCapacity
) {
}
