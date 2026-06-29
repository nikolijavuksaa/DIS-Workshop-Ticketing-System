package com.dis.workshopticketing.notificationservice.client;

import com.dis.workshopticketing.notificationservice.dto.WorkshopSessionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "workshop-service",
        url = "${workshop-service.url:http://localhost:8200}"
)
public interface WorkshopSessionClient {

    @GetMapping("/sessions/{id}")
    WorkshopSessionResponse getSession(@PathVariable Long id);
}
