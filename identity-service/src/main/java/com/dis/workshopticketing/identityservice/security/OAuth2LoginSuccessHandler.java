package com.dis.workshopticketing.identityservice.security;

import com.dis.workshopticketing.identityservice.model.User;
import com.dis.workshopticketing.identityservice.model.UserRole;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2LoginSuccessHandler implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final String successRedirectUrl;

    public OAuth2LoginSuccessHandler(
            JwtService jwtService,
            @Value("${app.security.oauth2.success-redirect-url:http://localhost:3000/oauth2/success}") String successRedirectUrl
    ) {
        this.jwtService = jwtService;
        this.successRedirectUrl = successRedirectUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        User user = User.builder()
                .id(Long.valueOf(principal.getAttribute("localUserId").toString()))
                .email(principal.getAttribute("localEmail"))
                .role(UserRole.valueOf(principal.getAttribute("localRole")))
                .build();

        String token = jwtService.createToken(user);
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        response.sendRedirect(successRedirectUrl + "?token=" + encodedToken);
    }
}
