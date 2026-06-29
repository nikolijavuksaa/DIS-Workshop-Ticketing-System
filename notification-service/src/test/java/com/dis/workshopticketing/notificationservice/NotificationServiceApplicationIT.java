package com.dis.workshopticketing.notificationservice;

import com.dis.workshopticketing.notificationservice.client.IdentityUserClient;
import com.dis.workshopticketing.notificationservice.client.WorkshopSessionClient;
import com.dis.workshopticketing.notificationservice.dto.ReservationEvent;
import com.dis.workshopticketing.notificationservice.dto.UserResponse;
import com.dis.workshopticketing.notificationservice.dto.WorkshopSessionResponse;
import com.dis.workshopticketing.notificationservice.model.ReservationStatus;
import com.dis.workshopticketing.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NotificationServiceApplicationIT {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockitoBean
    private IdentityUserClient identityUserClient;

    @MockitoBean
    private WorkshopSessionClient workshopSessionClient;

    @Test
    void contextLoadsWithMySqlAndRabbitContainers() {
    }

    @Test
    void createsNotificationWhenReservationEventIsPublished() throws Exception {
        notificationRepository.deleteAll();
        when(identityUserClient.getUser(7L)).thenReturn(new UserResponse(
                7L,
                "Ana",
                "Example",
                "ana@example.com",
                null,
                true,
                null,
                null
        ));
        when(workshopSessionClient.getSession(123L)).thenReturn(new WorkshopSessionResponse(
                123L,
                44L,
                "Spring Basics",
                LocalDateTime.of(2026, 7, 1, 18, 0),
                LocalDateTime.of(2026, 7, 1, 20, 0),
                "Room 1",
                new BigDecimal("50.00"),
                20,
                true,
                null,
                null
        ));

        rabbitTemplate.convertAndSend("reservation.events", "reservation.held", event("reservation.held"));

        assertEventuallyNotificationCount(1);
        var notification = notificationRepository.findAll().get(0);
        assertThat(notification.getUserId()).isEqualTo(7L);
        assertThat(notification.getEventType()).isEqualTo("reservation.held");
        assertThat(notification.getTitle()).isEqualTo("Reservation held");
        assertThat(notification.getMessage()).contains("Ana Example", "Spring Basics");
    }

    @Test
    void ignoresReleasedReservationEvents() throws Exception {
        notificationRepository.deleteAll();

        rabbitTemplate.convertAndSend("reservation.events", "reservation.released", event("reservation.released"));

        Thread.sleep(1000);
        assertThat(notificationRepository.count()).isZero();
    }

    private ReservationEvent event(String eventType) {
        return new ReservationEvent(
                eventType,
                501L,
                101L,
                7L,
                123L,
                ReservationStatus.HELD,
                LocalDateTime.of(2026, 6, 29, 9, 30)
        );
    }

    private void assertEventuallyNotificationCount(int expectedCount) throws InterruptedException {
        for (int attempt = 0; attempt < 20; attempt++) {
            if (notificationRepository.count() == expectedCount) {
                return;
            }
            Thread.sleep(250);
        }

        assertThat(notificationRepository.count()).isEqualTo(expectedCount);
    }
}
