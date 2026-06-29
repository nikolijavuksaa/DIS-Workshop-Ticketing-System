package com.dis.workshopticketing.workshopservice.client;

import com.dis.workshopticketing.workshopservice.dto.CreateReservationInventoryRequest;
import com.dis.workshopticketing.workshopservice.dto.UpdateReservationInventoryCapacityRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "reservation-service",
        url = "${reservation-service.url:http://localhost:8300}"
)
public interface ReservationInventoryClient {

    @PostMapping("/inventories")
    void createInventory(@RequestBody CreateReservationInventoryRequest request);

    @PatchMapping("/inventories/sessions/{sessionId}/capacity")
    void updateCapacity(
            @PathVariable Long sessionId,
            @RequestBody UpdateReservationInventoryCapacityRequest request
    );
}
