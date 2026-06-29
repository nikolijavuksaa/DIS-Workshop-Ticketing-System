package com.dis.workshopticketing.notificationservice.client;

import com.dis.workshopticketing.notificationservice.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "identity-service",
        url = "${identity-service.url:http://localhost:8100}"
)
public interface IdentityUserClient {

    @GetMapping("/users/{id}")
    UserResponse getUser(@PathVariable Long id);
}
