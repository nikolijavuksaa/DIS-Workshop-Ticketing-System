package com.dis.workshopticketing.workshopservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWorkshopRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotNull Long instructorId,
        @NotNull Long categoryId
) {
}
