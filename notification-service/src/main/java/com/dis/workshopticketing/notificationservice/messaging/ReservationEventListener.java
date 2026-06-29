package com.dis.workshopticketing.notificationservice.messaging;

import com.dis.workshopticketing.notificationservice.dto.ReservationEvent;
import com.dis.workshopticketing.notificationservice.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReservationEventListener {

    private final NotificationService notificationService;

    public ReservationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${notification.reservation-events.queue:notification.reservation-events}")
    public void handle(ReservationEvent event) {
        notificationService.createFromReservationEvent(event);
    }
}
