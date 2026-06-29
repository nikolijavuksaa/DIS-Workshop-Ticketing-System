package com.dis.workshopticketing.reservationservice.service;

import com.dis.workshopticketing.reservationservice.model.ScheduledJobLock;
import com.dis.workshopticketing.reservationservice.repository.ScheduledJobLockRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledJobLockServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-29T10:00:00Z"), ZoneOffset.UTC);

    private final ScheduledJobLockRepository repository = mock(ScheduledJobLockRepository.class);
    private final ScheduledJobLockService service = new ScheduledJobLockService(repository, CLOCK);

    @Test
    void createsLockWhenItDoesNotExist() {
        when(repository.findById("job")).thenReturn(Optional.empty());

        boolean acquired = service.tryAcquire("job", 20);

        assertThat(acquired).isTrue();
        verify(repository).saveAndFlush(lockWith("job", LocalDateTime.of(2026, 6, 29, 10, 0, 20)));
    }

    @Test
    void acquiresExpiredLock() {
        ScheduledJobLock lock = ScheduledJobLock.builder()
                .name("job")
                .lockedUntil(LocalDateTime.of(2026, 6, 29, 9, 59, 59))
                .build();
        when(repository.findById("job")).thenReturn(Optional.of(lock));

        boolean acquired = service.tryAcquire("job", 30);

        assertThat(acquired).isTrue();
        assertThat(lock.getLockedUntil()).isEqualTo(LocalDateTime.of(2026, 6, 29, 10, 0, 30));
        verify(repository).saveAndFlush(lock);
    }

    @Test
    void doesNotAcquireActiveLock() {
        ScheduledJobLock lock = ScheduledJobLock.builder()
                .name("job")
                .lockedUntil(LocalDateTime.of(2026, 6, 29, 10, 0, 1))
                .build();
        when(repository.findById("job")).thenReturn(Optional.of(lock));

        boolean acquired = service.tryAcquire("job", 30);

        assertThat(acquired).isFalse();
        assertThat(lock.getLockedUntil()).isEqualTo(LocalDateTime.of(2026, 6, 29, 10, 0, 1));
    }

    private ScheduledJobLock lockWith(String name, LocalDateTime lockedUntil) {
        return org.mockito.ArgumentMatchers.argThat(lock ->
                lock != null
                        && name.equals(lock.getName())
                        && lockedUntil.equals(lock.getLockedUntil())
        );
    }
}
