package com.dis.workshopticketing.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class GatewaySecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(authorize -> authorize
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/prometheus",
                                "/identity-service/oauth2/**",
                                "/identity-service/login/oauth2/**",
                                "/identity-service/users/*/exists",
                                "/identity-service/instructors/*/exists",
                                "/api/oauth2/**",
                                "/api/login/oauth2/**",
                                "/api/users/*/exists",
                                "/api/instructors/*/exists"
                        ).permitAll()
                        .pathMatchers(
                                "/api/bookings/**",
                                "/api/inventories/**",
                                "/api/reservations/**",
                                "/api/sessions/*/availability",
                                "/api/payments/**",
                                "/api/notifications/**",
                                "/booking-service/**",
                                "/reservation-service/inventories/**",
                                "/reservation-service/**",
                                "/payment-service/**",
                                "/notification-service/**"
                        ).authenticated()
                        .anyExchange().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder(@Value("${app.security.jwt.secret}") String secret) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
