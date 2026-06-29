package com.dis.workshopticketing.reservationservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateInventoryCapacityRequest(
        @NotNull @Positive Integer totalCapacity
) {
}
