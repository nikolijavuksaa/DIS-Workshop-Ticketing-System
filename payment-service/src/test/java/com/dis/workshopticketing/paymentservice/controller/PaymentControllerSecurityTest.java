package com.dis.workshopticketing.paymentservice.controller;

import com.dis.workshopticketing.paymentservice.dto.CreatePaymentRequest;
import com.dis.workshopticketing.paymentservice.dto.PaymentResponse;
import com.dis.workshopticketing.paymentservice.model.PaymentStatus;
import com.dis.workshopticketing.paymentservice.service.PaymentService;
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

import java.math.BigDecimal;
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
        "spring.datasource.url=jdbc:h2:mem:payment_controller_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "management.health.rabbit.enabled=false",
        "app.security.jwt.secret=change-me-change-me-change-me-change-me"
})
@AutoConfigureMockMvc
class PaymentControllerSecurityTest {

    private static final String JWT_SECRET = "change-me-change-me-change-me-change-me";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void rejectsPaymentRequestsWithoutJwt() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":10,\"amount\":50.00,\"currency\":\"EUR\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
    }

    @Test
    void keepsHealthEndpointPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsCreateRequestWithoutBookingId() throws Exception {
        mockMvc.perform(post("/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50.00,\"currency\":\"EUR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("bookingId: must not be null"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void createsPaymentForJwtSubject() throws Exception {
        when(paymentService.create(eq(7L), any(CreatePaymentRequest.class)))
                .thenReturn(new PaymentResponse(
                        1L,
                        10L,
                        7L,
                        new BigDecimal("50.00"),
                        "EUR",
                        PaymentStatus.SUCCEEDED,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(post("/payments")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":10,\"amount\":50.00,\"currency\":\"EUR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(10))
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        verify(paymentService).create(eq(7L), any(CreatePaymentRequest.class));
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
