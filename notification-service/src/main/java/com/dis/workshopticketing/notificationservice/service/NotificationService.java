package com.dis.workshopticketing.notificationservice.service;

import com.dis.workshopticketing.notificationservice.client.IdentityUserClient;
import com.dis.workshopticketing.notificationservice.client.WorkshopSessionClient;
import com.dis.workshopticketing.notificationservice.config.NotificationRabbitConfiguration;
import com.dis.workshopticketing.notificationservice.dto.NotificationResponse;
import com.dis.workshopticketing.notificationservice.dto.ReservationEvent;
import com.dis.workshopticketing.notificationservice.dto.UnreadCountResponse;
import com.dis.workshopticketing.notificationservice.dto.UserResponse;
import com.dis.workshopticketing.notificationservice.dto.WorkshopSessionResponse;
import com.dis.workshopticketing.notificationservice.exception.ResourceNotFoundException;
import com.dis.workshopticketing.notificationservice.model.Notification;
import com.dis.workshopticketing.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter SESSION_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationRepository notificationRepository;
    private final IdentityUserClient identityUserClient;
    private final WorkshopSessionClient workshopSessionClient;
    private final Clock clock;

    public NotificationService(
            NotificationRepository notificationRepository,
            IdentityUserClient identityUserClient,
            WorkshopSessionClient workshopSessionClient,
            Clock clock
    ) {
        this.notificationRepository = notificationRepository;
        this.identityUserClient = identityUserClient;
        this.workshopSessionClient = workshopSessionClient;
        this.clock = clock;
    }

    @Transactional
    public Optional<NotificationResponse> createFromReservationEvent(ReservationEvent event) {
        NotificationMessage notificationMessage = notificationMessage(event);
        if (notificationMessage == null) {
            LOGGER.debug("Ignoring reservation event type {}", event.eventType());
            return Optional.empty();
        }

        UserResponse user = user(event.userId()).orElse(null);
        WorkshopSessionResponse session = session(event.workshopSessionId()).orElse(null);

        Notification notification = Notification.builder()
                .userId(event.userId())
                .eventType(event.eventType())
                .title(notificationMessage.title())
                .message(notificationMessage.message(event, user, session))
                .reservationId(event.reservationId())
                .bookingId(event.bookingId())
                .workshopSessionId(event.workshopSessionId())
                .build();

        return Optional.of(NotificationResponse.from(notificationRepository.saveAndFlush(notification)));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAll(Long userId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(Long userId) {
        return new UnreadCountResponse(notificationRepository.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long id) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        markRead(notification);
        return NotificationResponse.from(notificationRepository.saveAndFlush(notification));
    }

    @Transactional
    public List<NotificationResponse> markAllRead(Long userId) {
        List<Notification> notifications = notificationRepository.findAllByUserIdAndReadAtIsNull(userId);
        notifications.forEach(this::markRead);
        return notificationRepository.saveAll(notifications)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    private void markRead(Notification notification) {
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now(clock));
        }
    }

    private Optional<UserResponse> user(Long userId) {
        try {
            return Optional.ofNullable(identityUserClient.getUser(userId));
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not enrich notification with user {}: {}", userId, exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<WorkshopSessionResponse> session(Long workshopSessionId) {
        try {
            return Optional.ofNullable(workshopSessionClient.getSession(workshopSessionId));
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not enrich notification with workshop session {}: {}", workshopSessionId, exception.getMessage());
            return Optional.empty();
        }
    }

    private NotificationMessage notificationMessage(ReservationEvent event) {
        return switch (event.eventType()) {
            case NotificationRabbitConfiguration.RESERVATION_HELD -> new NotificationMessage(
                    "Reservation held",
                    "You have a temporary hold for %s. Complete payment before the hold expires."
            );
            case NotificationRabbitConfiguration.RESERVATION_WAITLISTED -> new NotificationMessage(
                    "Added to waitlist",
                    "%s is full, so you have been added to the waitlist."
            );
            case NotificationRabbitConfiguration.RESERVATION_CONFIRMED -> new NotificationMessage(
                    "Reservation confirmed",
                    "Your place for %s is confirmed."
            );
            case NotificationRabbitConfiguration.RESERVATION_EXPIRED -> new NotificationMessage(
                    "Reservation expired",
                    "Your hold for %s expired before payment confirmation."
            );
            case NotificationRabbitConfiguration.WAITLIST_PROMOTED -> new NotificationMessage(
                    "Waitlist spot available",
                    "A spot opened for %s. Complete payment to confirm your place."
            );
            default -> null;
        };
    }

    private record NotificationMessage(String title, String messageTemplate) {

        String message(ReservationEvent event, UserResponse user, WorkshopSessionResponse session) {
            String greeting = user == null ? "" : user.displayName() + ", ";
            return greeting + messageTemplate.formatted(sessionLabel(event, session));
        }

        private String sessionLabel(ReservationEvent event, WorkshopSessionResponse session) {
            if (session == null) {
                return "session " + event.workshopSessionId();
            }

            String title = session.workshopTitle() == null || session.workshopTitle().isBlank()
                    ? "session " + session.id()
                    : session.workshopTitle();
            if (session.startsAt() == null) {
                return title;
            }

            return title + " on " + SESSION_TIME_FORMAT.format(session.startsAt());
        }
    }
}
