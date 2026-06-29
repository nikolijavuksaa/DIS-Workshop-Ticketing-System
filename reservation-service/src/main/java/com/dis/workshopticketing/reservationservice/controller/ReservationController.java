package com.dis.workshopticketing.reservationservice.controller;

import com.dis.workshopticketing.reservationservice.dto.AvailabilityResponse;
import com.dis.workshopticketing.reservationservice.dto.CreateHoldRequest;
import com.dis.workshopticketing.reservationservice.dto.CreateInventoryRequest;
import com.dis.workshopticketing.reservationservice.dto.ExpirationResponse;
import com.dis.workshopticketing.reservationservice.dto.InventoryResponse;
import com.dis.workshopticketing.reservationservice.dto.ReservationResponse;
import com.dis.workshopticketing.reservationservice.dto.UpdateInventoryCapacityRequest;
import com.dis.workshopticketing.reservationservice.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/inventories")
    public ResponseEntity<InventoryResponse> createInventory(@Valid @RequestBody CreateInventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createInventory(request));
    }

    @GetMapping("/sessions/{sessionId}/availability")
    public AvailabilityResponse getAvailability(@PathVariable Long sessionId) {
        return reservationService.getAvailability(sessionId);
    }

    @PatchMapping("/inventories/sessions/{sessionId}/capacity")
    public InventoryResponse updateCapacity(
            @PathVariable Long sessionId,
            @Valid @RequestBody UpdateInventoryCapacityRequest request
    ) {
        return reservationService.updateCapacity(sessionId, request);
    }

    @PostMapping("/reservations/holds")
    public ResponseEntity<ReservationResponse> createHold(@Valid @RequestBody CreateHoldRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createHold(request));
    }

    @PostMapping("/reservations/expire")
    public ExpirationResponse expireHolds() {
        return reservationService.expireHolds();
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse get(@PathVariable Long id) {
        return reservationService.get(id);
    }

    @GetMapping("/reservations/bookings/{bookingId}")
    public ReservationResponse getByBooking(@PathVariable Long bookingId) {
        return reservationService.getByBooking(bookingId);
    }

    @GetMapping("/reservations/sessions/{sessionId}")
    public List<ReservationResponse> getAllBySession(@PathVariable Long sessionId) {
        return reservationService.getAllBySession(sessionId);
    }

    @PostMapping("/reservations/{id}/confirm")
    public ReservationResponse confirm(@PathVariable Long id) {
        return reservationService.confirm(id);
    }

    @PostMapping("/reservations/{id}/release")
    public ReservationResponse release(@PathVariable Long id) {
        return reservationService.release(id);
    }
}
