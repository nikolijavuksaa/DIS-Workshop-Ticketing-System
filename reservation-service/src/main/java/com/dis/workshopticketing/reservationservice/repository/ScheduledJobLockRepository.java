package com.dis.workshopticketing.reservationservice.repository;

import com.dis.workshopticketing.reservationservice.model.ScheduledJobLock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduledJobLockRepository extends JpaRepository<ScheduledJobLock, String> {
}
