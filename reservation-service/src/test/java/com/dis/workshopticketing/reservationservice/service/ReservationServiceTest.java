package com.dis.workshopticketing.reservationservice.service;

import com.dis.workshopticketing.reservationservice.dto.CreateHoldRequest;
import com.dis.workshopticketing.reservationservice.dto.CreateInventoryRequest;
import com.dis.workshopticketing.reservationservice.dto.InventoryResponse;
import com.dis.workshopticketing.reservationservice.dto.ReservationResponse;
import com.dis.workshopticketing.reservationservice.dto.UpdateInventoryCapacityRequest;
import com.dis.workshopticketing.reservationservice.exception.BadRequestException;
import com.dis.workshopticketing.reservationservice.exception.DuplicateInventoryException;
import com.dis.workshopticketing.reservationservice.messaging.ReservationEventPublisher;
import com.dis.workshopticketing.reservationservice.model.Reservation;
import com.dis.workshopticketing.reservationservice.model.ReservationStatus;
import com.dis.workshopticketing.reservationservice.model.WorkshopSessionInventory;
import com.dis.workshopticketing.reservationservice.repository.ReservationRepository;
import com.dis.workshopticketing.reservationservice.repository.WorkshopSessionInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-29T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 29, 10, 0);

    @Mock
    private WorkshopSessionInventoryRepository inventoryRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationEventPublisher eventPublisher;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any(DefaultTransactionDefinition.class)))
                .thenReturn(transactionStatus);

        reservationService = new ReservationService(
                inventoryRepository,
                reservationRepository,
                eventPublisher,
                transactionManager,
                CLOCK,
                10
        );
    }

    @Test
    void createInventoryCreatesCountersAtZero() {
        CreateInventoryRequest request = new CreateInventoryRequest(100L, 20);
        when(inventoryRepository.existsByWorkshopSessionId(100L)).thenReturn(false);
        when(inventoryRepository.saveAndFlush(any(WorkshopSessionInventory.class))).thenAnswer(invocation -> {
            WorkshopSessionInventory inventory = invocation.getArgument(0);
            inventory.setId(1L);
            inventory.setVersion(0L);
            inventory.setCreatedAt(NOW);
            inventory.setUpdatedAt(NOW);
            return inventory;
        });

        InventoryResponse response = reservationService.createInventory(request);

        assertThat(response.workshopSessionId()).isEqualTo(100L);
        assertThat(response.totalCapacity()).isEqualTo(20);
        assertThat(response.heldCount()).isZero();
        assertThat(response.confirmedCount()).isZero();
        assertThat(response.waitlistedCount()).isZero();
        assertThat(response.availableCount()).isEqualTo(20);
    }

    @Test
    void createInventoryRejectsDuplicateSessionInventory() {
        when(inventoryRepository.existsByWorkshopSessionId(100L)).thenReturn(true);

        assertThatThrownBy(() -> reservationService.createInventory(new CreateInventoryRequest(100L, 20)))
                .isInstanceOf(DuplicateInventoryException.class)
                .hasMessageContaining("Inventory already exists for workshop session: 100");
    }

    @Test
    void updateCapacityRejectsCapacityLowerThanHeldAndConfirmedCount() {
        WorkshopSessionInventory inventory = inventory(100L, 10, 2, 4, 0);
        when(inventoryRepository.findByWorkshopSessionId(100L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> reservationService.updateCapacity(100L, new UpdateInventoryCapacityRequest(5)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("totalCapacity cannot be lower");
    }

    @Test
    void createHoldCreatesHeldReservationWhenSeatIsAvailable() {
        WorkshopSessionInventory inventory = inventory(100L, 2, 0, 0, 0);
        when(reservationRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(inventoryRepository.findByWorkshopSessionId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.saveAndFlush(inventory)).thenReturn(inventory);
        when(reservationRepository.saveAndFlush(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation reservation = invocation.getArgument(0);
            reservation.setId(1L);
            return reservation;
        });

        ReservationResponse response = reservationService.createHold(new CreateHoldRequest(10L, 50L, 100L));

        assertThat(response.status()).isEqualTo(ReservationStatus.HELD);
        assertThat(response.expiresAt()).isEqualTo(NOW.plusMinutes(10));
        assertThat(inventory.getHeldCount()).isEqualTo(1);
        assertThat(inventory.getWaitlistedCount()).isZero();
        verify(eventPublisher).record(eq(ReservationEventPublisher.RESERVATION_HELD), reservationWithId(1L));
    }

    @Test
    void createHoldCreatesWaitlistedReservationWhenSessionIsFull() {
        WorkshopSessionInventory inventory = inventory(100L, 1, 1, 0, 0);
        when(reservationRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(inventoryRepository.findByWorkshopSessionId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.saveAndFlush(inventory)).thenReturn(inventory);
        when(reservationRepository.saveAndFlush(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation reservation = invocation.getArgument(0);
            reservation.setId(1L);
            return reservation;
        });

        ReservationResponse response = reservationService.createHold(new CreateHoldRequest(10L, 50L, 100L));

        assertThat(response.status()).isEqualTo(ReservationStatus.WAITLISTED);
        assertThat(response.expiresAt()).isNull();
        assertThat(inventory.getHeldCount()).isEqualTo(1);
        assertThat(inventory.getWaitlistedCount()).isEqualTo(1);
        verify(eventPublisher).record(eq(ReservationEventPublisher.RESERVATION_WAITLISTED), reservationWithId(1L));
    }

    @Test
    void confirmMovesHeldReservationToConfirmed() {
        Reservation reservation = reservation(1L, 10L, 50L, 100L, ReservationStatus.HELD);
        reservation.setExpiresAt(NOW.plusMinutes(10));
        WorkshopSessionInventory inventory = inventory(100L, 2, 1, 0, 0);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findByWorkshopSessionId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.saveAndFlush(inventory)).thenReturn(inventory);
        when(reservationRepository.saveAndFlush(reservation)).thenReturn(reservation);

        ReservationResponse response = reservationService.confirm(1L);

        assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(response.expiresAt()).isNull();
        assertThat(inventory.getHeldCount()).isZero();
        assertThat(inventory.getConfirmedCount()).isEqualTo(1);
        verify(eventPublisher).record(ReservationEventPublisher.RESERVATION_CONFIRMED, reservation);
    }

    @Test
    void releaseConfirmedReservationPromotesFirstWaitlistedReservation() {
        Reservation confirmed = reservation(1L, 10L, 50L, 100L, ReservationStatus.CONFIRMED);
        Reservation waitlisted = reservation(2L, 11L, 51L, 100L, ReservationStatus.WAITLISTED);
        WorkshopSessionInventory inventory = inventory(100L, 1, 0, 1, 1);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(confirmed));
        when(inventoryRepository.findByWorkshopSessionId(100L)).thenReturn(Optional.of(inventory));
        when(reservationRepository.findFirstByWorkshopSessionIdAndStatusOrderByCreatedAtAsc(
                100L,
                ReservationStatus.WAITLISTED
        )).thenReturn(Optional.of(waitlisted));
        when(reservationRepository.saveAndFlush(waitlisted)).thenReturn(waitlisted);
        when(inventoryRepository.saveAndFlush(inventory)).thenReturn(inventory);
        when(reservationRepository.saveAndFlush(confirmed)).thenReturn(confirmed);

        ReservationResponse response = reservationService.release(1L);

        assertThat(response.status()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(waitlisted.getStatus()).isEqualTo(ReservationStatus.HELD);
        assertThat(waitlisted.getExpiresAt()).isEqualTo(NOW.plusMinutes(10));
        assertThat(inventory.getConfirmedCount()).isZero();
        assertThat(inventory.getWaitlistedCount()).isZero();
        assertThat(inventory.getHeldCount()).isEqualTo(1);
        verify(eventPublisher).record(ReservationEventPublisher.RESERVATION_RELEASED, confirmed);
        verify(eventPublisher).record(ReservationEventPublisher.WAITLIST_PROMOTED, waitlisted);
    }

    @Test
    void confirmRejectsNonHeldReservation() {
        Reservation reservation = reservation(1L, 10L, 50L, 100L, ReservationStatus.WAITLISTED);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.confirm(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only HELD reservations can be confirmed");

        verify(eventPublisher, never()).record(any(), any());
    }

    private WorkshopSessionInventory inventory(
            Long workshopSessionId,
            int totalCapacity,
            int heldCount,
            int confirmedCount,
            int waitlistedCount
    ) {
        return WorkshopSessionInventory.builder()
                .id(1L)
                .workshopSessionId(workshopSessionId)
                .totalCapacity(totalCapacity)
                .heldCount(heldCount)
                .confirmedCount(confirmedCount)
                .waitlistedCount(waitlistedCount)
                .version(0L)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    private Reservation reservation(
            Long id,
            Long bookingId,
            Long userId,
            Long workshopSessionId,
            ReservationStatus status
    ) {
        return Reservation.builder()
                .id(id)
                .bookingId(bookingId)
                .userId(userId)
                .workshopSessionId(workshopSessionId)
                .status(status)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    private Reservation reservationWithId(Long id) {
        return org.mockito.ArgumentMatchers.argThat(reservation -> reservation != null && id.equals(reservation.getId()));
    }
}
