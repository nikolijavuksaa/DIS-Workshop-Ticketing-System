package com.dis.workshopticketing.reservationservice.repository;

import com.dis.workshopticketing.reservationservice.model.OutboxEventStatus;
import com.dis.workshopticketing.reservationservice.model.ReservationOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReservationOutboxEventRepository extends JpaRepository<ReservationOutboxEvent, Long> {

    List<ReservationOutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

    List<ReservationOutboxEvent> findTop50ByStatusInOrderByCreatedAtAsc(Collection<OutboxEventStatus> statuses);
}
