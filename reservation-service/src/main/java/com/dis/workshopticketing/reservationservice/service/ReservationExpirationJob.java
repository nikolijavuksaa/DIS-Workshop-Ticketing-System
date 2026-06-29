package com.dis.workshopticketing.reservationservice.service;

import com.dis.workshopticketing.reservationservice.dto.ExpirationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpirationJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationExpirationJob.class);
    private static final String LOCK_NAME = "reservation-expiration-job";

    private final ReservationService reservationService;
    private final ScheduledJobLockService scheduledJobLockService;

    public ReservationExpirationJob(
            ReservationService reservationService,
            ScheduledJobLockService scheduledJobLockService
    ) {
        this.reservationService = reservationService;
        this.scheduledJobLockService = scheduledJobLockService;
    }

    @Scheduled(fixedDelayString = "${reservation.expiration-check-interval-ms:30000}")
    public void expireHolds() {
        if (!scheduledJobLockService.tryAcquire(LOCK_NAME, 60)) {
            return;
        }

        ExpirationResponse response = reservationService.expireHolds();
        if (response.expiredCount() > 0) {
            LOGGER.info(
                    "Expired {} reservation holds and promoted {} waitlisted reservations",
                    response.expiredCount(),
                    response.promotedReservations().size()
            );
        }
    }
}
