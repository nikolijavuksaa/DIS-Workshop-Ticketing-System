package com.dis.workshopticketing.workshopservice.client;

import com.dis.workshopticketing.workshopservice.dto.ExistenceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "identity-service",
        url = "${identity-service.url:http://localhost:8100}",
        path = "/instructors"
)
public interface IdentityInstructorClient {

    @GetMapping("/{id}/exists")
    ExistenceResponse exists(@PathVariable Long id);
}
