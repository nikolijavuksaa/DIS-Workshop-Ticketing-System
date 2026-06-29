package com.dis.workshopticketing.bookingservice.client;

import com.dis.workshopticketing.bookingservice.dto.CreateReservationHoldRequest;
import com.dis.workshopticketing.bookingservice.dto.ReservationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "reservation-service",
        url = "${reservation-service.url:http://localhost:8300}"
)
public interface ReservationClient {

    @PostMapping("/reservations/holds")
    ReservationResponse createHold(@RequestBody CreateReservationHoldRequest request);

    @PostMapping("/reservations/{id}/release")
    ReservationResponse release(@PathVariable("id") Long id);

    @PostMapping("/reservations/{id}/confirm")
    ReservationResponse confirm(@PathVariable("id") Long id);
}
