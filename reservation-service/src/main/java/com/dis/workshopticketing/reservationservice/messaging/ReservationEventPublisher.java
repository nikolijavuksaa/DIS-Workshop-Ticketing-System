package com.dis.workshopticketing.reservationservice.messaging;

import com.dis.workshopticketing.reservationservice.model.Reservation;
import com.dis.workshopticketing.reservationservice.model.ReservationOutboxEvent;
import com.dis.workshopticketing.reservationservice.repository.ReservationOutboxEventRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class ReservationEventPublisher {

    public static final String RESERVATION_HELD = "reservation.held";
    public static final String RESERVATION_WAITLISTED = "reservation.waitlisted";
    public static final String RESERVATION_CONFIRMED = "reservation.confirmed";
    public static final String RESERVATION_EXPIRED = "reservation.expired";
    public static final String RESERVATION_RELEASED = "reservation.released";
    public static final String WAITLIST_PROMOTED = "waitlist.promoted";

    private final ReservationOutboxEventRepository outboxEventRepository;
    private final Clock clock;

    public ReservationEventPublisher(
            ReservationOutboxEventRepository outboxEventRepository,
            Clock clock
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.clock = clock;
    }

    public void record(String eventType, Reservation reservation) {
        outboxEventRepository.save(ReservationOutboxEvent.builder()
                .eventType(eventType)
                .reservationId(reservation.getId())
                .bookingId(reservation.getBookingId())
                .userId(reservation.getUserId())
                .workshopSessionId(reservation.getWorkshopSessionId())
                .reservationStatus(reservation.getStatus())
                .occurredAt(LocalDateTime.now(clock))
                .build());
    }
}
