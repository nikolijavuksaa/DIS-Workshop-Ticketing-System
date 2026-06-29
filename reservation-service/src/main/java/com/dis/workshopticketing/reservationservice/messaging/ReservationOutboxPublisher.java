package com.dis.workshopticketing.reservationservice.messaging;

import com.dis.workshopticketing.reservationservice.dto.ReservationEvent;
import com.dis.workshopticketing.reservationservice.model.OutboxEventStatus;
import com.dis.workshopticketing.reservationservice.model.ReservationOutboxEvent;
import com.dis.workshopticketing.reservationservice.repository.ReservationOutboxEventRepository;
import com.dis.workshopticketing.reservationservice.service.ScheduledJobLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Component
public class ReservationOutboxPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationOutboxPublisher.class);
    private static final String LOCK_NAME = "reservation-outbox-publisher";

    private final ReservationOutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ScheduledJobLockService scheduledJobLockService;
    private final Clock clock;
    private final String exchangeName;

    public ReservationOutboxPublisher(
            ReservationOutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            ScheduledJobLockService scheduledJobLockService,
            Clock clock,
            @Value("${reservation.events.exchange:reservation.events}") String exchangeName
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.scheduledJobLockService = scheduledJobLockService;
        this.clock = clock;
        this.exchangeName = exchangeName;
    }

    @Scheduled(fixedDelayString = "${reservation.outbox-publish-interval-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        if (!scheduledJobLockService.tryAcquire(LOCK_NAME, 20)) {
            return;
        }

        List<ReservationOutboxEvent> events = outboxEventRepository.findTop50ByStatusInOrderByCreatedAtAsc(
                EnumSet.of(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED)
        );

        for (ReservationOutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(ReservationOutboxEvent outboxEvent) {
        ReservationEvent event = new ReservationEvent(
                outboxEvent.getEventType(),
                outboxEvent.getReservationId(),
                outboxEvent.getBookingId(),
                outboxEvent.getUserId(),
                outboxEvent.getWorkshopSessionId(),
                outboxEvent.getReservationStatus(),
                outboxEvent.getOccurredAt()
        );

        try {
            rabbitTemplate.convertAndSend(exchangeName, outboxEvent.getEventType(), event);
            outboxEvent.setStatus(OutboxEventStatus.SENT);
            outboxEvent.setSentAt(LocalDateTime.now(clock));
            outboxEvent.setLastError(null);
        } catch (AmqpException exception) {
            outboxEvent.setStatus(OutboxEventStatus.FAILED);
            outboxEvent.setLastError(exception.getMessage());
            LOGGER.warn("Reservation outbox event could not be published: {}", outboxEvent.getId(), exception);
        }

        outboxEvent.setAttempts(outboxEvent.getAttempts() + 1);
        outboxEventRepository.save(outboxEvent);
    }
}
