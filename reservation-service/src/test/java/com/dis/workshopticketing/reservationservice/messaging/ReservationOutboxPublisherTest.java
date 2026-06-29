package com.dis.workshopticketing.reservationservice.messaging;

import com.dis.workshopticketing.reservationservice.dto.ReservationEvent;
import com.dis.workshopticketing.reservationservice.model.OutboxEventStatus;
import com.dis.workshopticketing.reservationservice.model.ReservationOutboxEvent;
import com.dis.workshopticketing.reservationservice.model.ReservationStatus;
import com.dis.workshopticketing.reservationservice.repository.ReservationOutboxEventRepository;
import com.dis.workshopticketing.reservationservice.service.ScheduledJobLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationOutboxPublisherTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-29T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ReservationOutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ScheduledJobLockService scheduledJobLockService;

    @Test
    void doesNothingWhenLockCannotBeAcquired() {
        ReservationOutboxPublisher publisher = publisher();
        when(scheduledJobLockService.tryAcquire("reservation-outbox-publisher", 20)).thenReturn(false);

        publisher.publishPendingEvents();

        verifyNoInteractions(rabbitTemplate);
        verify(outboxEventRepository, never()).findTop50ByStatusInOrderByCreatedAtAsc(any());
    }

    @Test
    void publishesPendingEventAndMarksItSent() {
        ReservationOutboxPublisher publisher = publisher();
        ReservationOutboxEvent outboxEvent = outboxEvent(OutboxEventStatus.PENDING);
        when(scheduledJobLockService.tryAcquire("reservation-outbox-publisher", 20)).thenReturn(true);
        when(outboxEventRepository.findTop50ByStatusInOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(outboxEvent));

        publisher.publishPendingEvents();

        ArgumentCaptor<ReservationEvent> eventCaptor = ArgumentCaptor.forClass(ReservationEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("reservation.events"), eq("reservation.held"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().reservationId()).isEqualTo(20L);
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEventStatus.SENT);
        assertThat(outboxEvent.getAttempts()).isEqualTo(1);
        assertThat(outboxEvent.getSentAt()).isEqualTo(LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC));
        assertThat(outboxEvent.getLastError()).isNull();
        verify(outboxEventRepository).save(outboxEvent);
    }

    @Test
    void leavesEventFailedWhenRabbitPublishFails() {
        ReservationOutboxPublisher publisher = publisher();
        ReservationOutboxEvent outboxEvent = outboxEvent(OutboxEventStatus.PENDING);
        when(scheduledJobLockService.tryAcquire("reservation-outbox-publisher", 20)).thenReturn(true);
        when(outboxEventRepository.findTop50ByStatusInOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(outboxEvent));
        doThrow(new AmqpException("broker unavailable"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(ReservationEvent.class));

        publisher.publishPendingEvents();

        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(outboxEvent.getAttempts()).isEqualTo(1);
        assertThat(outboxEvent.getSentAt()).isNull();
        assertThat(outboxEvent.getLastError()).isEqualTo("broker unavailable");
        verify(outboxEventRepository).save(outboxEvent);
    }

    private ReservationOutboxPublisher publisher() {
        return new ReservationOutboxPublisher(
                outboxEventRepository,
                rabbitTemplate,
                scheduledJobLockService,
                CLOCK,
                "reservation.events"
        );
    }

    private ReservationOutboxEvent outboxEvent(OutboxEventStatus status) {
        return ReservationOutboxEvent.builder()
                .id(1L)
                .eventType("reservation.held")
                .reservationId(20L)
                .bookingId(10L)
                .userId(7L)
                .workshopSessionId(123L)
                .reservationStatus(ReservationStatus.HELD)
                .occurredAt(LocalDateTime.of(2026, 6, 29, 9, 55))
                .status(status)
                .attempts(0)
                .build();
    }
}
