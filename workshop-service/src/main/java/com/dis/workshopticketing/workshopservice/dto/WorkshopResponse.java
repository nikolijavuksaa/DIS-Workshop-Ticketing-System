package com.dis.workshopticketing.workshopservice.dto;

import com.dis.workshopticketing.workshopservice.model.Workshop;

import java.time.LocalDateTime;

public record WorkshopResponse(
        Long id,
        String title,
        String description,
        Long instructorId,
        Long categoryId,
        String categoryName,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static WorkshopResponse from(Workshop workshop) {
        return new WorkshopResponse(
                workshop.getId(),
                workshop.getTitle(),
                workshop.getDescription(),
                workshop.getInstructorId(),
                workshop.getCategory().getId(),
                workshop.getCategory().getName(),
                workshop.isActive(),
                workshop.getCreatedAt(),
                workshop.getUpdatedAt()
        );
    }
}
