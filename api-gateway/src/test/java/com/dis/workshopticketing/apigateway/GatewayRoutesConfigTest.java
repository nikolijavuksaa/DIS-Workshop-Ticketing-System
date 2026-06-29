package com.dis.workshopticketing.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "spring.cloud.gateway.server.webflux.discovery.locator.enabled=false"
})
class GatewayRoutesConfigTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void exposesApiBookingsRouteToBookingService() {
        var routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes)
                .isNotNull()
                .anySatisfy(route -> {
                    assertThat(route.getId()).isEqualTo("booking-service-api");
                    assertThat(route.getUri().toString()).isEqualTo("lb://booking-service");
                    assertThat(route.getFilters()).isNotEmpty();
                });
    }

    @Test
    void exposesApiPaymentsRouteToPaymentService() {
        var routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes)
                .isNotNull()
                .anySatisfy(route -> {
                    assertThat(route.getId()).isEqualTo("payment-service-api");
                    assertThat(route.getUri().toString()).isEqualTo("lb://payment-service");
                    assertThat(route.getFilters()).isNotEmpty();
                });
    }

    @Test
    void exposesApiNotificationsRouteToNotificationService() {
        var routes = routeLocator.getRoutes().collectList().block();

        assertThat(routes)
                .isNotNull()
                .anySatisfy(route -> {
                    assertThat(route.getId()).isEqualTo("notification-service-api");
                    assertThat(route.getUri().toString()).isEqualTo("lb://notification-service");
                    assertThat(route.getFilters()).isNotEmpty();
                });
    }
}
