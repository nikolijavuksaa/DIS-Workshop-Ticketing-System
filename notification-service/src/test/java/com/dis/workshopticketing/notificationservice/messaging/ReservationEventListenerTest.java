package com.dis.workshopticketing.notificationservice.messaging;

import com.dis.workshopticketing.notificationservice.dto.ReservationEvent;
import com.dis.workshopticketing.notificationservice.model.ReservationStatus;
import com.dis.workshopticketing.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReservationEventListenerTest {

    @Test
    void delegatesReservationEventToNotificationService() {
        NotificationService notificationService = mock(NotificationService.class);
        ReservationEventListener listener = new ReservationEventListener(notificationService);
        ReservationEvent event = new ReservationEvent(
                "reservation.held",
                501L,
                101L,
                7L,
                123L,
                ReservationStatus.HELD,
                LocalDateTime.now()
        );

        listener.handle(event);

        verify(notificationService).createFromReservationEvent(event);
    }
}
