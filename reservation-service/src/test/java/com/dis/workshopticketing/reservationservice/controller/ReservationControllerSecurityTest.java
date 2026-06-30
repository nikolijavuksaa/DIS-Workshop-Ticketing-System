package com.dis.workshopticketing.reservationservice.controller;

import com.dis.workshopticketing.reservationservice.dto.AvailabilityResponse;
import com.dis.workshopticketing.reservationservice.dto.CreateHoldRequest;
import com.dis.workshopticketing.reservationservice.dto.InventoryResponse;
import com.dis.workshopticketing.reservationservice.dto.ReservationResponse;
import com.dis.workshopticketing.reservationservice.dto.UpdateInventoryCapacityRequest;
import com.dis.workshopticketing.reservationservice.model.ReservationStatus;
import com.dis.workshopticketing.reservationservice.service.ReservationService;
import com.dis.workshopticketing.reservationservice.service.ScheduledJobLockService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:reservation_controller_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "management.health.rabbit.enabled=false",
        "management.endpoints.web.exposure.include=health,prometheus",
        "app.security.jwt.secret=change-me-change-me-change-me-change-me"
})
@AutoConfigureMockMvc
class ReservationControllerSecurityTest {

    private static final String JWT_SECRET = "change-me-change-me-change-me-change-me";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private ScheduledJobLockService scheduledJobLockService;

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
    void rejectsReservationRequestsWithoutJwt() throws Exception {
        mockMvc.perform(post("/reservations/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":10,\"userId\":7,\"workshopSessionId\":123}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservationService);
    }

    @Test
    void rejectsInventoryRequestsWithoutJwt() throws Exception {
        mockMvc.perform(post("/inventories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workshopSessionId\":123,\"totalCapacity\":20}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservationService);
    }

    @Test
    void rejectsCreateHoldWithoutBookingId() throws Exception {
        mockMvc.perform(post("/reservations/holds")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"workshopSessionId\":123}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("bookingId: must not be null"));

        verifyNoInteractions(reservationService);
    }

    @Test
    void rejectsInventoryWithoutPositiveCapacity() throws Exception {
        mockMvc.perform(post("/inventories")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workshopSessionId\":123,\"totalCapacity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("totalCapacity: must be greater than 0"));

        verifyNoInteractions(reservationService);
    }

    @Test
    void rejectsCapacityUpdateWithoutPositiveCapacity() throws Exception {
        mockMvc.perform(patch("/inventories/sessions/123/capacity")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalCapacity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("totalCapacity: must be greater than 0"));

        verifyNoInteractions(reservationService);
    }

    @Test
    void createsHoldWithJwt() throws Exception {
        when(reservationService.createHold(any(CreateHoldRequest.class))).thenReturn(reservation());

        mockMvc.perform(post("/reservations/holds")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":10,\"userId\":7,\"workshopSessionId\":123}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(10))
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.workshopSessionId").value(123))
                .andExpect(jsonPath("$.status").value("HELD"));

        verify(reservationService).createHold(any(CreateHoldRequest.class));
    }

    @Test
    void confirmsReservationWithJwt() throws Exception {
        when(reservationService.confirm(20L)).thenReturn(reservation());

        mockMvc.perform(post("/reservations/20/confirm")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.status").value("HELD"));

        verify(reservationService).confirm(20L);
    }

    @Test
    void releasesReservationWithJwt() throws Exception {
        when(reservationService.release(20L)).thenReturn(releasedReservation());

        mockMvc.perform(post("/reservations/20/release")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.status").value("RELEASED"));

        verify(reservationService).release(20L);
    }

    @Test
    void returnsAvailabilityWithJwt() throws Exception {
        when(reservationService.getAvailability(123L))
                .thenReturn(new AvailabilityResponse(123L, 20, 1, 2, 3, 17));

        mockMvc.perform(get("/sessions/123/availability")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workshopSessionId").value(123))
                .andExpect(jsonPath("$.availableCount").value(17));

        verify(reservationService).getAvailability(123L);
    }

    @Test
    void updatesCapacityWithJwt() throws Exception {
        when(reservationService.updateCapacity(eq(123L), any(UpdateInventoryCapacityRequest.class)))
                .thenReturn(new InventoryResponse(
                        1L,
                        123L,
                        30,
                        1,
                        2,
                        3,
                        27,
                        0L,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(patch("/inventories/sessions/123/capacity")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalCapacity\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workshopSessionId").value(123))
                .andExpect(jsonPath("$.totalCapacity").value(30));

        verify(reservationService).updateCapacity(eq(123L), any(UpdateInventoryCapacityRequest.class));
    }

    private ReservationResponse reservation() {
        return new ReservationResponse(
                20L,
                10L,
                7L,
                123L,
                ReservationStatus.HELD,
                LocalDateTime.now().plusMinutes(15),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private ReservationResponse releasedReservation() {
        return new ReservationResponse(
                20L,
                10L,
                7L,
                123L,
                ReservationStatus.RELEASED,
                null,
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
