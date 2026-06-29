package com.dis.workshopticketing.bookingservice.controller;

import com.dis.workshopticketing.bookingservice.dto.BookingResponse;
import com.dis.workshopticketing.bookingservice.dto.CreateBookingRequest;
import com.dis.workshopticketing.bookingservice.model.BookingStatus;
import com.dis.workshopticketing.bookingservice.service.BookingService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        "spring.datasource.url=jdbc:h2:mem:booking_controller_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "management.health.rabbit.enabled=false",
        "app.security.jwt.secret=change-me-change-me-change-me-change-me"
})
@AutoConfigureMockMvc
class BookingControllerSecurityTest {

    private static final String JWT_SECRET = "change-me-change-me-change-me-change-me";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @Test
    void rejectsBookingRequestsWithoutJwt() throws Exception {
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workshopSessionId\":123}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookingService);
    }

    @Test
    void keepsHealthEndpointPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsCreateRequestWithoutWorkshopSessionId() throws Exception {
        mockMvc.perform(post("/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("workshopSessionId: must not be null"));

        verifyNoInteractions(bookingService);
    }

    @Test
    void createsBookingForJwtSubjectAndIgnoresUserIdFromRequestBody() throws Exception {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        when(bookingService.create(eq(7L), any(CreateBookingRequest.class)))
                .thenReturn(new BookingResponse(
                        1L,
                        7L,
                        123L,
                        20L,
                        BookingStatus.PENDING_PAYMENT,
                        expiresAt,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":999,\"workshopSessionId\":123}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.workshopSessionId").value(123))
                .andExpect(jsonPath("$.reservationId").value(20))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        verify(bookingService).create(eq(7L), any(CreateBookingRequest.class));
    }

    @Test
    void confirmsPaymentForJwtSubject() throws Exception {
        when(bookingService.confirmPayment(7L, 1L))
                .thenReturn(new BookingResponse(
                        1L,
                        7L,
                        123L,
                        20L,
                        BookingStatus.CONFIRMED,
                        null,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/bookings/1/payment-confirmed")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(bookingService).confirmPayment(7L, 1L);
    }

    @Test
    void failsPaymentForJwtSubject() throws Exception {
        when(bookingService.failPayment(7L, 1L))
                .thenReturn(new BookingResponse(
                        1L,
                        7L,
                        123L,
                        20L,
                        BookingStatus.PAYMENT_FAILED,
                        null,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/bookings/1/payment-failed")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));

        verify(bookingService).failPayment(7L, 1L);
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
