package com.dis.workshopticketing.paymentservice;

import com.dis.workshopticketing.paymentservice.repository.PaymentRepository;
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
                "spring.datasource.url=jdbc:h2:mem:payment_flow_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "spring.rabbitmq.listener.direct.auto-startup=false",
                "management.health.rabbit.enabled=false",
                "app.security.jwt.secret=change-me-change-me-change-me-change-me"
        }
)
class PaymentFlowHttpIntegrationTest {

    private static final String JWT_SECRET = "change-me-change-me-change-me-change-me";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final AtomicInteger BOOKING_STATUS = new AtomicInteger(200);
    private static final AtomicReference<String> BOOKING_RESPONSE = new AtomicReference<>();
    private static final AtomicReference<RecordedBookingRequest> LAST_BOOKING_REQUEST = new AtomicReference<>();

    private static HttpServer bookingServer;

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @DynamicPropertySource
    static void bookingServiceProperties(DynamicPropertyRegistry registry) {
        startBookingServer();
        registry.add("booking-service.url", () -> "http://localhost:" + bookingServer.getAddress().getPort());
    }

    @AfterAll
    static void stopBookingServer() {
        if (bookingServer != null) {
            bookingServer.stop(0);
        }
    }

    @BeforeEach
    void resetBookingServer() {
        paymentRepository.deleteAll();
        BOOKING_STATUS.set(200);
        BOOKING_RESPONSE.set(null);
        LAST_BOOKING_REQUEST.set(null);
    }

    @Test
    void createPaymentStoresSucceededPaymentAndConfirmsBookingWithForwardedAuthorizationHeader() throws Exception {
        String token = bearerToken("7");
        BOOKING_RESPONSE.set("""
                {
                  "id": 101,
                  "userId": 7,
                  "workshopSessionId": 123,
                  "reservationId": 501,
                  "status": "CONFIRMED",
                  "reservationExpiresAt": null,
                  "createdAt": "2026-06-29T15:15:00",
                  "updatedAt": "2026-06-29T15:30:00"
                }
                """);

        HttpResponse<String> response = postPayment(token, """
                {
                  "bookingId": 101,
                  "amount": 50.00,
                  "currency": "rsd"
                }
                """);
        JsonNode body = objectMapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(body.path("id").asLong()).isPositive();
        assertThat(body.path("bookingId").asLong()).isEqualTo(101L);
        assertThat(body.path("userId").asLong()).isEqualTo(7L);
        assertThat(body.path("amount").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(body.path("currency").asText()).isEqualTo("RSD");
        assertThat(body.path("status").asText()).isEqualTo("SUCCEEDED");

        RecordedBookingRequest bookingRequest = LAST_BOOKING_REQUEST.get();
        assertThat(bookingRequest).isNotNull();
        assertThat(bookingRequest.method()).isEqualTo("POST");
        assertThat(bookingRequest.path()).isEqualTo("/bookings/101/payment-confirmed");
        assertThat(bookingRequest.authorization()).isEqualTo(token);
        assertThat(bookingRequest.body()).isEmpty();
    }

    @Test
    void createPaymentStoresFailedPaymentAndFailsBookingWithForwardedAuthorizationHeader() throws Exception {
        String token = bearerToken("8");
        BOOKING_RESPONSE.set("""
                {
                  "id": 202,
                  "userId": 8,
                  "workshopSessionId": 124,
                  "reservationId": 502,
                  "status": "PAYMENT_FAILED",
                  "reservationExpiresAt": null,
                  "createdAt": "2026-06-29T15:16:00",
                  "updatedAt": "2026-06-29T15:31:00"
                }
                """);

        HttpResponse<String> response = postPayment(token, """
                {
                  "bookingId": 202,
                  "amount": 75.50,
                  "currency": "eur",
                  "simulateFailure": true
                }
                """);
        JsonNode body = objectMapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(body.path("id").asLong()).isPositive();
        assertThat(body.path("bookingId").asLong()).isEqualTo(202L);
        assertThat(body.path("userId").asLong()).isEqualTo(8L);
        assertThat(body.path("amount").decimalValue()).isEqualByComparingTo("75.50");
        assertThat(body.path("currency").asText()).isEqualTo("EUR");
        assertThat(body.path("status").asText()).isEqualTo("FAILED");

        RecordedBookingRequest bookingRequest = LAST_BOOKING_REQUEST.get();
        assertThat(bookingRequest).isNotNull();
        assertThat(bookingRequest.method()).isEqualTo("POST");
        assertThat(bookingRequest.path()).isEqualTo("/bookings/202/payment-failed");
        assertThat(bookingRequest.authorization()).isEqualTo(token);
        assertThat(bookingRequest.body()).isEmpty();
    }

    private HttpResponse<String> postPayment(String bearerToken, String requestBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/payments"))
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void startBookingServer() {
        if (bookingServer != null) {
            return;
        }

        try {
            bookingServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            bookingServer.createContext("/bookings", PaymentFlowHttpIntegrationTest::handleBookingAction);
            bookingServer.setExecutor(Executors.newSingleThreadExecutor());
            bookingServer.start();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start mock booking server", exception);
        }
    }

    private static void handleBookingAction(HttpExchange exchange) throws IOException {
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        LAST_BOOKING_REQUEST.set(new RecordedBookingRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION),
                requestBody
        ));

        String response = BOOKING_RESPONSE.get();
        if (response == null) {
            response = "{\"message\":\"No booking response configured\"}";
            BOOKING_STATUS.set(500);
        }

        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(BOOKING_STATUS.get(), responseBytes.length);
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

    private record RecordedBookingRequest(
            String method,
            String path,
            String authorization,
            String body
    ) {
    }
}
