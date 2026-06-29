package com.dis.workshopticketing.bookingservice.service;

import com.dis.workshopticketing.bookingservice.client.ReservationClient;
import com.dis.workshopticketing.bookingservice.dto.CreateBookingRequest;
import com.dis.workshopticketing.bookingservice.dto.CreateReservationHoldRequest;
import com.dis.workshopticketing.bookingservice.dto.ReservationResponse;
import com.dis.workshopticketing.bookingservice.dto.ReservationStatus;
import com.dis.workshopticketing.bookingservice.exception.BookingCreationException;
import com.dis.workshopticketing.bookingservice.exception.ResourceNotFoundException;
import com.dis.workshopticketing.bookingservice.model.Booking;
import com.dis.workshopticketing.bookingservice.model.BookingStatus;
import com.dis.workshopticketing.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ReservationClient reservationClient;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepository, reservationClient);
    }

    @Test
    void createSetsPendingPaymentWhenReservationIsHeld() {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        stubSaveAndFlush();
        when(reservationClient.createHold(any(CreateReservationHoldRequest.class)))
                .thenReturn(reservation(20L, 1L, 7L, 123L, ReservationStatus.HELD, expiresAt));

        var response = bookingService.create(7L, new CreateBookingRequest(123L));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.workshopSessionId()).isEqualTo(123L);
        assertThat(response.reservationId()).isEqualTo(20L);
        assertThat(response.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(response.reservationExpiresAt()).isEqualTo(expiresAt);

        ArgumentCaptor<CreateReservationHoldRequest> holdRequestCaptor =
                ArgumentCaptor.forClass(CreateReservationHoldRequest.class);
        verify(reservationClient).createHold(holdRequestCaptor.capture());
        assertThat(holdRequestCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(new CreateReservationHoldRequest(1L, 7L, 123L));
    }

    @Test
    void createSetsWaitlistedWhenReservationIsWaitlisted() {
        stubSaveAndFlush();
        when(reservationClient.createHold(any(CreateReservationHoldRequest.class)))
                .thenReturn(reservation(21L, 1L, 7L, 123L, ReservationStatus.WAITLISTED, null));

        var response = bookingService.create(7L, new CreateBookingRequest(123L));

        assertThat(response.reservationId()).isEqualTo(21L);
        assertThat(response.status()).isEqualTo(BookingStatus.WAITLISTED);
        assertThat(response.reservationExpiresAt()).isNull();
    }

    @Test
    void createMarksBookingFailedWhenReservationClientFails() {
        List<BookingStatus> savedStatuses = new ArrayList<>();
        stubSaveAndFlush(savedStatuses);
        when(reservationClient.createHold(any(CreateReservationHoldRequest.class)))
                .thenThrow(new RuntimeException("reservation-service unavailable"));

        assertThatThrownBy(() -> bookingService.create(7L, new CreateBookingRequest(123L)))
                .isInstanceOf(BookingCreationException.class)
                .hasMessageContaining("Booking could not reserve a workshop session: 1");

        assertThat(savedStatuses).containsExactly(BookingStatus.INITIATING, BookingStatus.FAILED);
    }

    @Test
    void getDoesNotAllowUserToReadAnotherUsersBooking() {
        when(bookingRepository.findByIdAndUserId(55L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.get(7L, 55L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Booking not found: 55");
    }

    @Test
    void cancelReleasesReservationAndSetsBookingCancelled() {
        Booking booking = Booking.builder()
                .id(1L)
                .userId(7L)
                .workshopSessionId(123L)
                .reservationId(20L)
                .status(BookingStatus.PENDING_PAYMENT)
                .build();

        when(bookingRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(booking));
        stubSaveAndFlush();

        var response = bookingService.cancel(7L, 1L);

        verify(reservationClient).release(20L);
        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    private void stubSaveAndFlush() {
        stubSaveAndFlush(new ArrayList<>());
    }

    private void stubSaveAndFlush(List<BookingStatus> savedStatuses) {
        AtomicLong nextId = new AtomicLong(1L);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            savedStatuses.add(booking.getStatus());
            if (booking.getId() == null) {
                booking.setId(nextId.getAndIncrement());
            }
            return booking;
        });
    }

    private ReservationResponse reservation(
            Long id,
            Long bookingId,
            Long userId,
            Long workshopSessionId,
            ReservationStatus status,
            LocalDateTime expiresAt
    ) {
        return new ReservationResponse(
                id,
                bookingId,
                userId,
                workshopSessionId,
                status,
                expiresAt,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
