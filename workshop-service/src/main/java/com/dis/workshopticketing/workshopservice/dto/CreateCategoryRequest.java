package com.dis.workshopticketing.workshopservice.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
        @NotBlank String name,
        String description
) {
}
