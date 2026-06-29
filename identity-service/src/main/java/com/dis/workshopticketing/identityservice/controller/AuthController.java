package com.dis.workshopticketing.identityservice.controller;

import com.dis.workshopticketing.identityservice.dto.AuthUserResponse;
import com.dis.workshopticketing.identityservice.model.UserRole;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new AuthUserResponse(
                Long.valueOf(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                UserRole.valueOf(jwt.getClaimAsString("role"))
        );
    }
}
