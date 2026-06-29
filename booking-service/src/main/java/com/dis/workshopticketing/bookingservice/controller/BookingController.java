package com.dis.workshopticketing.bookingservice.controller;

import com.dis.workshopticketing.bookingservice.dto.BookingResponse;
import com.dis.workshopticketing.bookingservice.dto.CreateBookingRequest;
import com.dis.workshopticketing.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(userId(jwt), request));
    }

    @GetMapping
    public List<BookingResponse> getAll(@AuthenticationPrincipal Jwt jwt) {
        return bookingService.getAll(userId(jwt));
    }

    @GetMapping("/{id}")
    public BookingResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.get(userId(jwt), id);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.cancel(userId(jwt), id);
    }

    @PostMapping("/{id}/payment-confirmed")
    public BookingResponse confirmPayment(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.confirmPayment(userId(jwt), id);
    }

    @PostMapping("/{id}/payment-failed")
    public BookingResponse failPayment(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return bookingService.failPayment(userId(jwt), id);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
