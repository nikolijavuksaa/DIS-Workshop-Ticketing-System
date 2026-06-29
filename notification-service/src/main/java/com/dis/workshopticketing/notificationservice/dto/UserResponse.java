package com.dis.workshopticketing.notificationservice.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public String displayName() {
        String name = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        return name.isBlank() ? "User " + id : name;
    }
}
