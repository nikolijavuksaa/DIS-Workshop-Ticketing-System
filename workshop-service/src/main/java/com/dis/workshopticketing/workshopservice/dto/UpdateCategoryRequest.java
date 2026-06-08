package com.dis.workshopticketing.workshopservice.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCategoryRequest(
        @NotBlank String name,
        String description
) {
}
