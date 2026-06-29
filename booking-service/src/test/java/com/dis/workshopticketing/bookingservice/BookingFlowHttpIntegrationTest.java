package com.dis.workshopticketing.bookingservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.config.import=",
                "spring.cloud.config.enabled=false",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.service-registry.auto-registration.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:booking_flow_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "spring.rabbitmq.listener.direct.auto-startup=false",
                "management.health.rabbit.enabled=false",
                "app.security.jwt.secret=change-me-change-me-change-me-change-me"
        }
)
class BookingFlowHttpIntegrationTest {

    private static final String JWT_SECRET = "change-me-change-me-change-me-change-me";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final AtomicInteger RESERVATION_STATUS = new AtomicInteger(201);
    private static final AtomicReference<String> RESERVATION_RESPONSE = new AtomicReference<>();
    private static final AtomicReference<RecordedReservationRequest> LAST_RESERVATION_REQUEST = new AtomicReference<>();

    private static HttpServer reservationServer;

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void reservationServiceProperties(DynamicPropertyRegistry registry) {
        startReservationServer();
        registry.add("reservation-service.url", () -> "http://localhost:" + reservationServer.getAddress().getPort());
    }

    @AfterAll
    static void stopReservationServer() {
        if (reservationServer != null) {
            reservationServer.stop(0);
        }
    }

    @BeforeEach
    void resetReservationServer() {
        RESERVATION_STATUS.set(201);
        RESERVATION_RESPONSE.set(null);
        LAST_RESERVATION_REQUEST.set(null);
    }

    @Test
    void createBookingStoresPendingPaymentWhenReservationHoldIsHeldAndForwardsAuthorizationHeader() throws Exception {
        String token = bearerToken("7");
        RESERVATION_RESPONSE.set("""
                {
                  "id": 501,
                  "bookingId": 1,
                  "userId": 7,
                  "workshopSessionId": 123,
                  "status": "HELD",
                  "expiresAt": "2026-06-29T15:30:00",
                  "createdAt": "2026-06-29T15:15:00",
                  "updatedAt": "2026-06-29T15:15:00"
                }
                """);

        HttpResponse<String> response = postBooking(token, 123L);
        JsonNode body = objectMapper.readTree(response.body());
        long bookingId = body.path("id").asLong();

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(bookingId).isPositive();
        assertThat(body.path("userId").asLong()).isEqualTo(7L);
        assertThat(body.path("workshopSessionId").asLong()).isEqualTo(123L);
        assertThat(body.path("reservationId").asLong()).isEqualTo(501L);
        assertThat(body.path("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(body.path("reservationExpiresAt").asText()).isEqualTo("2026-06-29T15:30:00");

        RecordedReservationRequest reservationRequest = LAST_RESERVATION_REQUEST.get();
        assertThat(reservationRequest).isNotNull();
        assertThat(reservationRequest.method()).isEqualTo("POST");
        assertThat(reservationRequest.path()).isEqualTo("/reservations/holds");
        assertThat(reservationRequest.authorization()).isEqualTo(token);
        JsonNode reservationRequestBody = objectMapper.readTree(reservationRequest.body());
        assertThat(reservationRequestBody.path("bookingId").asLong()).isEqualTo(bookingId);
        assertThat(reservationRequestBody.path("userId").asLong()).isEqualTo(7L);
        assertThat(reservationRequestBody.path("workshopSessionId").asLong()).isEqualTo(123L);
    }

    @Test
    void createBookingStoresWaitlistedWhenReservationHoldIsWaitlisted() throws Exception {
        RESERVATION_RESPONSE.set("""
                {
                  "id": 502,
                  "bookingId": 2,
                  "userId": 8,
                  "workshopSessionId": 124,
                  "status": "WAITLISTED",
                  "expiresAt": null,
                  "createdAt": "2026-06-29T15:16:00",
                  "updatedAt": "2026-06-29T15:16:00"
                }
                """);

        HttpResponse<String> response = postBooking(bearerToken("8"), 124L);
        JsonNode body = objectMapper.readTree(response.body());
        long bookingId = body.path("id").asLong();

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(bookingId).isPositive();
        assertThat(body.path("userId").asLong()).isEqualTo(8L);
        assertThat(body.path("workshopSessionId").asLong()).isEqualTo(124L);
        assertThat(body.path("reservationId").asLong()).isEqualTo(502L);
        assertThat(body.path("status").asText()).isEqualTo("WAITLISTED");
        assertThat(body.path("reservationExpiresAt").isNull()).isTrue();

        RecordedReservationRequest reservationRequest = LAST_RESERVATION_REQUEST.get();
        assertThat(reservationRequest).isNotNull();
        JsonNode reservationRequestBody = objectMapper.readTree(reservationRequest.body());
        assertThat(reservationRequestBody.path("bookingId").asLong()).isEqualTo(bookingId);
        assertThat(reservationRequestBody.path("userId").asLong()).isEqualTo(8L);
        assertThat(reservationRequestBody.path("workshopSessionId").asLong()).isEqualTo(124L);
    }

    @Test
    void confirmPaymentConfirmsReservationAndStoresConfirmedBooking() throws Exception {
        String token = bearerToken("7");
        JsonNode booking = createPendingPaymentBooking(token, 123L);
        long bookingId = booking.path("id").asLong();

        LAST_RESERVATION_REQUEST.set(null);
        RESERVATION_RESPONSE.set("""
                {
                  "id": 501,
                  "bookingId": 1,
                  "userId": 7,
                  "workshopSessionId": 123,
                  "status": "CONFIRMED",
                  "expiresAt": null,
                  "createdAt": "2026-06-29T15:15:00",
                  "updatedAt": "2026-06-29T15:20:00"
                }
                """);

        HttpResponse<String> response = postPaymentOutcome(token, bookingId, "payment-confirmed");
        JsonNode body = objectMapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.path("id").asLong()).isEqualTo(bookingId);
        assertThat(body.path("status").asText()).isEqualTo("CONFIRMED");
        assertThat(body.path("reservationExpiresAt").isNull()).isTrue();

        RecordedReservationRequest reservationRequest = LAST_RESERVATION_REQUEST.get();
        assertThat(reservationRequest).isNotNull();
        assertThat(reservationRequest.method()).isEqualTo("POST");
        assertThat(reservationRequest.path()).isEqualTo("/reservations/501/confirm");
        assertThat(reservationRequest.authorization()).isEqualTo(token);
    }

    @Test
    void failPaymentReleasesReservationAndStoresPaymentFailedBooking() throws Exception {
        String token = bearerToken("7");
        JsonNode booking = createPendingPaymentBooking(token, 123L);
        long bookingId = booking.path("id").asLong();

        LAST_RESERVATION_REQUEST.set(null);
        RESERVATION_RESPONSE.set("""
                {
                  "id": 501,
                  "bookingId": 1,
                  "userId": 7,
                  "workshopSessionId": 123,
                  "status": "RELEASED",
                  "expiresAt": null,
                  "createdAt": "2026-06-29T15:15:00",
                  "updatedAt": "2026-06-29T15:20:00"
                }
                """);

        HttpResponse<String> response = postPaymentOutcome(token, bookingId, "payment-failed");
        JsonNode body = objectMapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.path("id").asLong()).isEqualTo(bookingId);
        assertThat(body.path("status").asText()).isEqualTo("PAYMENT_FAILED");
        assertThat(body.path("reservationExpiresAt").isNull()).isTrue();

        RecordedReservationRequest reservationRequest = LAST_RESERVATION_REQUEST.get();
        assertThat(reservationRequest).isNotNull();
        assertThat(reservationRequest.method()).isEqualTo("POST");
        assertThat(reservationRequest.path()).isEqualTo("/reservations/501/release");
        assertThat(reservationRequest.authorization()).isEqualTo(token);
    }

    private HttpResponse<String> postBooking(String bearerToken, Long workshopSessionId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/bookings"))
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString("{\"workshopSessionId\":" + workshopSessionId + "}"))
                .build();

        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postPaymentOutcome(String bearerToken, Long bookingId, String outcome)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/bookings/" + bookingId + "/" + outcome))
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode createPendingPaymentBooking(String token, Long workshopSessionId) throws Exception {
        RESERVATION_RESPONSE.set("""
                {
                  "id": 501,
                  "bookingId": 1,
                  "userId": 7,
                  "workshopSessionId": 123,
                  "status": "HELD",
                  "expiresAt": "2026-06-29T15:30:00",
                  "createdAt": "2026-06-29T15:15:00",
                  "updatedAt": "2026-06-29T15:15:00"
                }
                """);

        HttpResponse<String> createResponse = postBooking(token, workshopSessionId);
        assertThat(createResponse.statusCode()).isEqualTo(201);
        return objectMapper.readTree(createResponse.body());
    }

    private static void startReservationServer() {
        if (reservationServer != null) {
            return;
        }

        try {
            reservationServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            reservationServer.createContext("/reservations/holds", BookingFlowHttpIntegrationTest::handleReservationHold);
            reservationServer.createContext("/reservations", BookingFlowHttpIntegrationTest::handleReservationAction);
            reservationServer.setExecutor(Executors.newSingleThreadExecutor());
            reservationServer.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start mock reservation server", exception);
        }
    }

    private static void handleReservationHold(HttpExchange exchange) throws IOException {
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        LAST_RESERVATION_REQUEST.set(new RecordedReservationRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION),
                requestBody
        ));

        String response = RESERVATION_RESPONSE.get();
        if (response == null) {
            response = "{\"message\":\"No reservation response configured\"}";
            RESERVATION_STATUS.set(500);
        }

        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(RESERVATION_STATUS.get(), responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private static void handleReservationAction(HttpExchange exchange) throws IOException {
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        LAST_RESERVATION_REQUEST.set(new RecordedReservationRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION),
                requestBody
        ));

        String response = RESERVATION_RESPONSE.get();
        if (response == null) {
            response = "{\"message\":\"No reservation response configured\"}";
            RESERVATION_STATUS.set(500);
        }

        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(RESERVATION_STATUS.get(), responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private static String bearerToken(String subject) throws JOSEException {
        return "Bearer " + jwt(subject);
    }

    private static String jwt(String subject) throws JOSEException {
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

    private record RecordedReservationRequest(
            String method,
            String path,
            String authorization,
            String body
    ) {
    }
}
