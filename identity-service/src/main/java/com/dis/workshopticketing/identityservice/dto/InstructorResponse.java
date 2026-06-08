package com.dis.workshopticketing.identityservice.dto;

import com.dis.workshopticketing.identityservice.model.Instructor;

import java.time.LocalDateTime;

public record InstructorResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String bio,
        String expertise,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static InstructorResponse from(Instructor instructor) {
        return new InstructorResponse(
                instructor.getId(),
                instructor.getFirstName(),
                instructor.getLastName(),
                instructor.getEmail(),
                instructor.getBio(),
                instructor.getExpertise(),
                instructor.isActive(),
                instructor.getCreatedAt(),
                instructor.getUpdatedAt()
        );
    }
}
