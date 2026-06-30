package com.dis.workshopticketing.notificationservice.controller;

import com.dis.workshopticketing.notificationservice.dto.NotificationResponse;
import com.dis.workshopticketing.notificationservice.dto.UnreadCountResponse;
import com.dis.workshopticketing.notificationservice.service.NotificationService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:notification_controller_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "management.health.rabbit.enabled=false",
        "management.endpoints.web.exposure.include=health,prometheus",
        "app.security.jwt.secret=change-me-change-me-change-me-change-me"
})
@AutoConfigureMockMvc
class NotificationControllerSecurityTest {

    private static final String JWT_SECRET = "change-me-change-me-change-me-change-me";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void rejectsNotificationRequestsWithoutJwt() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(notificationService);
    }

    @Test
    void keepsHealthEndpointPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void keepsPrometheusEndpointPublic() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    @Test
    void listsNotificationsForJwtSubject() throws Exception {
        when(notificationService.getAll(7L)).thenReturn(List.of(notification()));

        mockMvc.perform(get("/notifications")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(7))
                .andExpect(jsonPath("$[0].title").value("Reservation held"))
                .andExpect(jsonPath("$[0].read").value(false));

        verify(notificationService).getAll(7L);
    }

    @Test
    void returnsUnreadCountForJwtSubject() throws Exception {
        when(notificationService.unreadCount(7L)).thenReturn(new UnreadCountResponse(3));

        mockMvc.perform(get("/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));

        verify(notificationService).unreadCount(7L);
    }

    @Test
    void marksNotificationReadForJwtSubject() throws Exception {
        when(notificationService.markRead(7L, 1L)).thenReturn(readNotification());

        mockMvc.perform(post("/notifications/1/read")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.read").value(true));

        verify(notificationService).markRead(7L, 1L);
    }

    @Test
    void marksAllNotificationsReadForJwtSubject() throws Exception {
        when(notificationService.markAllRead(7L)).thenReturn(List.of(readNotification()));

        mockMvc.perform(post("/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].read").value(true));

        verify(notificationService).markAllRead(7L);
    }

    private NotificationResponse notification() {
        return new NotificationResponse(
                1L,
                7L,
                "reservation.held",
                "Reservation held",
                "Complete payment.",
                501L,
                101L,
                123L,
                false,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private NotificationResponse readNotification() {
        LocalDateTime readAt = LocalDateTime.of(2026, 6, 29, 10, 0);
        return new NotificationResponse(
                1L,
                7L,
                "reservation.held",
                "Reservation held",
                "Complete payment.",
                501L,
                101L,
                123L,
                true,
                readAt,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private String bearerToken(String subject) throws JOSEException {
        return "Bearer " + jwt(subject);
    }

    private String jwt(String subject) throws JOSEException {
        JWSSigner signer = new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .build();

        SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJwt.sign(signer);
        return signedJwt.serialize();
    }
}
