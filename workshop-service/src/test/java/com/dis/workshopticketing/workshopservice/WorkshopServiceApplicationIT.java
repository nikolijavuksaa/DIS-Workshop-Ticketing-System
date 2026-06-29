package com.dis.workshopticketing.workshopservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "identity-service.url=http://localhost:8100",
        "reservation-service.url=http://localhost:8300"
})
class WorkshopServiceApplicationIT {

    @Test
    void contextLoadsWithMySqlContainer() {
    }
}
