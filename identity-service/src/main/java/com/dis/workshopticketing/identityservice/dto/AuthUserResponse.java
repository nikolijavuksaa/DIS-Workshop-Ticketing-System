package com.dis.workshopticketing.identityservice.dto;

import com.dis.workshopticketing.identityservice.model.UserRole;

public record AuthUserResponse(
        Long id,
        String email,
        UserRole role
) {
}
