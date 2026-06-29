package com.dis.workshopticketing.reservationservice.service;

import com.dis.workshopticketing.reservationservice.model.ScheduledJobLock;
import com.dis.workshopticketing.reservationservice.repository.ScheduledJobLockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class ScheduledJobLockService {

    private final ScheduledJobLockRepository scheduledJobLockRepository;
    private final Clock clock;

    public ScheduledJobLockService(ScheduledJobLockRepository scheduledJobLockRepository, Clock clock) {
        this.scheduledJobLockRepository = scheduledJobLockRepository;
        this.clock = clock;
    }

    @Transactional
    public boolean tryAcquire(String name, long lockSeconds) {
        LocalDateTime now = LocalDateTime.now(clock);
        ScheduledJobLock lock = scheduledJobLockRepository.findById(name)
                .orElseGet(() -> ScheduledJobLock.builder()
                        .name(name)
                        .lockedUntil(now.minusSeconds(1))
                        .build());

        if (lock.getLockedUntil().isAfter(now)) {
            return false;
        }

        lock.setLockedUntil(now.plusSeconds(lockSeconds));
        scheduledJobLockRepository.saveAndFlush(lock);
        return true;
    }
}
