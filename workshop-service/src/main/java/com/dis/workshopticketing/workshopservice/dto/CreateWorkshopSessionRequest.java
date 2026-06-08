package com.dis.workshopticketing.workshopservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateWorkshopSessionRequest(
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt,
        @NotBlank String location,
        @NotNull @DecimalMin("0.00") BigDecimal price
) {
}
