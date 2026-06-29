package com.dis.workshopticketing.notificationservice.service;

import com.dis.workshopticketing.notificationservice.client.IdentityUserClient;
import com.dis.workshopticketing.notificationservice.client.WorkshopSessionClient;
import com.dis.workshopticketing.notificationservice.dto.ReservationEvent;
import com.dis.workshopticketing.notificationservice.dto.UserResponse;
import com.dis.workshopticketing.notificationservice.dto.WorkshopSessionResponse;
import com.dis.workshopticketing.notificationservice.exception.ResourceNotFoundException;
import com.dis.workshopticketing.notificationservice.model.Notification;
import com.dis.workshopticketing.notificationservice.model.ReservationStatus;
import com.dis.workshopticketing.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-29T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private IdentityUserClient identityUserClient;

    @Mock
    private WorkshopSessionClient workshopSessionClient;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                identityUserClient,
                workshopSessionClient,
                CLOCK
        );
    }

    @ParameterizedTest
    @CsvSource({
            "reservation.held,Reservation held,temporary hold",
            "reservation.waitlisted,Added to waitlist,waitlist",
            "reservation.confirmed,Reservation confirmed,confirmed",
            "reservation.expired,Reservation expired,expired",
            "waitlist.promoted,Waitlist spot available,spot opened"
    })
    void createsNotificationForSupportedReservationEvents(String eventType, String title, String messageFragment) {
        when(identityUserClient.getUser(7L)).thenReturn(user());
        when(workshopSessionClient.getSession(123L)).thenReturn(session());
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(1L);
            return notification;
        });

        var response = notificationService.createFromReservationEvent(event(eventType));

        assertThat(response).isPresent();
        assertThat(response.get().title()).isEqualTo(title);
        assertThat(response.get().message()).contains("Ana Example", "Spring Basics on 2026-07-01 18:00", messageFragment);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getEventType()).isEqualTo(eventType);
        assertThat(captor.getValue().getReservationId()).isEqualTo(501L);
        assertThat(captor.getValue().getBookingId()).isEqualTo(101L);
        assertThat(captor.getValue().getWorkshopSessionId()).isEqualTo(123L);
    }

    @Test
    void savesFallbackMessageWhenEnrichmentFails() {
        when(identityUserClient.getUser(7L)).thenThrow(new RuntimeException("identity unavailable"));
        when(workshopSessionClient.getSession(123L)).thenThrow(new RuntimeException("workshop unavailable"));
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = notificationService.createFromReservationEvent(event("reservation.held"));

        assertThat(response).isPresent();
        assertThat(response.get().message()).contains("session 123");
    }

    @Test
    void ignoresUnsupportedReservationEvents() {
        var response = notificationService.createFromReservationEvent(event("reservation.released"));

        assertThat(response).isEmpty();
        verify(notificationRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsUserNotificationsNewestFirst() {
        Notification notification = notification(1L, 7L);
        when(notificationRepository.findAllByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(notification));

        var responses = notificationService.getAll(7L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
    }

    @Test
    void marksOwnedNotificationRead() {
        Notification notification = notification(1L, 7L);
        when(notificationRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(notification));
        when(notificationRepository.saveAndFlush(notification)).thenReturn(notification);

        var response = notificationService.markRead(7L, 1L);

        assertThat(response.read()).isTrue();
        assertThat(response.readAt()).isEqualTo(LocalDateTime.of(2026, 6, 29, 10, 0));
    }

    @Test
    void rejectsMarkReadForAnotherUsersNotification() {
        when(notificationRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(7L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notification not found with id: 1");
    }

    @Test
    void marksAllOwnedUnreadNotificationsRead() {
        Notification first = notification(1L, 7L);
        Notification second = notification(2L, 7L);
        when(notificationRepository.findAllByUserIdAndReadAtIsNull(7L)).thenReturn(List.of(first, second));
        when(notificationRepository.saveAll(List.of(first, second))).thenReturn(List.of(first, second));

        var responses = notificationService.markAllRead(7L);

        assertThat(responses).hasSize(2);
        assertThat(responses).allSatisfy(response -> assertThat(response.read()).isTrue());
    }

    private ReservationEvent event(String eventType) {
        return new ReservationEvent(
                eventType,
                501L,
                101L,
                7L,
                123L,
                ReservationStatus.HELD,
                LocalDateTime.of(2026, 6, 29, 9, 30)
        );
    }

    private UserResponse user() {
        return new UserResponse(7L, "Ana", "Example", "ana@example.com", null, true, null, null);
    }

    private WorkshopSessionResponse session() {
        return new WorkshopSessionResponse(
                123L,
                44L,
                "Spring Basics",
                LocalDateTime.of(2026, 7, 1, 18, 0),
                LocalDateTime.of(2026, 7, 1, 20, 0),
                "Room 1",
                new BigDecimal("50.00"),
                20,
                true,
                null,
                null
        );
    }

    private Notification notification(Long id, Long userId) {
        return Notification.builder()
                .id(id)
                .userId(userId)
                .eventType("reservation.held")
                .title("Reservation held")
                .message("Complete payment.")
                .reservationId(501L)
                .bookingId(101L)
                .workshopSessionId(123L)
                .build();
    }
}
