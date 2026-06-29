package com.dis.workshopticketing.paymentservice.client;

import com.dis.workshopticketing.paymentservice.dto.BookingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "booking-service",
        url = "${booking-service.url:http://localhost:8200}"
)
public interface BookingClient {

    @PostMapping("/bookings/{id}/payment-confirmed")
    BookingResponse confirmPayment(@PathVariable("id") Long id);

    @PostMapping("/bookings/{id}/payment-failed")
    BookingResponse failPayment(@PathVariable("id") Long id);
}
