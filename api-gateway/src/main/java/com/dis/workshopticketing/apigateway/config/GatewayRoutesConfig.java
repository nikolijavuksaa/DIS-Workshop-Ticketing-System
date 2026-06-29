package com.dis.workshopticketing.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    RouteLocator applicationRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("booking-service-api", route -> route
                        .path("/api/bookings", "/api/bookings/**")
                        .filters(filters -> filters.stripPrefix(1))
                        .uri("lb://booking-service"))
                .route("payment-service-api", route -> route
                        .path("/api/payments", "/api/payments/**")
                        .filters(filters -> filters.stripPrefix(1))
                        .uri("lb://payment-service"))
                .route("notification-service-api", route -> route
                        .path("/api/notifications", "/api/notifications/**")
                        .filters(filters -> filters.stripPrefix(1))
                        .uri("lb://notification-service"))
                .build();
    }
}
