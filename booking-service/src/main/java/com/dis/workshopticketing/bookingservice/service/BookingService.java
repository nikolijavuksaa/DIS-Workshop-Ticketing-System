package com.dis.workshopticketing.bookingservice.service;

import com.dis.workshopticketing.bookingservice.client.ReservationClient;
import com.dis.workshopticketing.bookingservice.dto.BookingResponse;
import com.dis.workshopticketing.bookingservice.dto.CreateBookingRequest;
import com.dis.workshopticketing.bookingservice.dto.CreateReservationHoldRequest;
import com.dis.workshopticketing.bookingservice.dto.ReservationResponse;
import com.dis.workshopticketing.bookingservice.dto.ReservationStatus;
import com.dis.workshopticketing.bookingservice.exception.BadRequestException;
import com.dis.workshopticketing.bookingservice.exception.BookingCancellationException;
import com.dis.workshopticketing.bookingservice.exception.BookingCreationException;
import com.dis.workshopticketing.bookingservice.exception.BookingPaymentCompletionException;
import com.dis.workshopticketing.bookingservice.exception.ResourceNotFoundException;
import com.dis.workshopticketing.bookingservice.model.Booking;
import com.dis.workshopticketing.bookingservice.model.BookingStatus;
import com.dis.workshopticketing.bookingservice.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ReservationClient reservationClient;

    public BookingService(BookingRepository bookingRepository, ReservationClient reservationClient) {
        this.bookingRepository = bookingRepository;
        this.reservationClient = reservationClient;
    }

    public BookingResponse create(Long userId, CreateBookingRequest request) {
        Booking booking = Booking.builder()
                .userId(userId)
                .workshopSessionId(request.workshopSessionId())
                .status(BookingStatus.INITIATING)
                .build();

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        try {
            ReservationResponse reservation = reservationClient.createHold(new CreateReservationHoldRequest(
                    savedBooking.getId(),
                    savedBooking.getUserId(),
                    savedBooking.getWorkshopSessionId()
            ));

            applyReservation(savedBooking, reservation);
            return BookingResponse.from(bookingRepository.saveAndFlush(savedBooking));
        } catch (BookingCreationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            savedBooking.setStatus(BookingStatus.FAILED);
            bookingRepository.saveAndFlush(savedBooking);
            throw new BookingCreationException(savedBooking.getId(), exception);
        }
    }

    @Transactional(readOnly = true)
    public BookingResponse get(Long userId, Long id) {
        return BookingResponse.from(findOwnedBooking(userId, id));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAll(Long userId) {
        return bookingRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    public BookingResponse cancel(Long userId, Long id) {
        Booking booking = findOwnedBooking(userId, id);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return BookingResponse.from(booking);
        }
        if (booking.getStatus() == BookingStatus.FAILED
                || booking.getStatus() == BookingStatus.PAYMENT_FAILED
                || booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BadRequestException(booking.getStatus() + " bookings cannot be cancelled");
        }

        if (booking.getReservationId() != null) {
            try {
                reservationClient.release(booking.getReservationId());
            } catch (RuntimeException exception) {
                throw new BookingCancellationException(booking.getId(), exception);
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return BookingResponse.from(bookingRepository.saveAndFlush(booking));
    }

    public BookingResponse confirmPayment(Long userId, Long id) {
        Booking booking = findOwnedBooking(userId, id);
        validatePendingPayment(booking);
        validateReservationPresent(booking);

        try {
            reservationClient.confirm(booking.getReservationId());
        } catch (RuntimeException exception) {
            throw new BookingPaymentCompletionException(booking.getId(), "confirmation", exception);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setReservationExpiresAt(null);
        return BookingResponse.from(bookingRepository.saveAndFlush(booking));
    }

    public BookingResponse failPayment(Long userId, Long id) {
        Booking booking = findOwnedBooking(userId, id);
        validatePendingPayment(booking);
        validateReservationPresent(booking);

        try {
            reservationClient.release(booking.getReservationId());
        } catch (RuntimeException exception) {
            throw new BookingPaymentCompletionException(booking.getId(), "failure", exception);
        }

        booking.setStatus(BookingStatus.PAYMENT_FAILED);
        booking.setReservationExpiresAt(null);
        return BookingResponse.from(bookingRepository.saveAndFlush(booking));
    }

    private void applyReservation(Booking booking, ReservationResponse reservation) {
        booking.setReservationId(reservation.id());
        booking.setReservationExpiresAt(reservation.expiresAt());

        if (reservation.status() == ReservationStatus.HELD) {
            booking.setStatus(BookingStatus.PENDING_PAYMENT);
            return;
        }
        if (reservation.status() == ReservationStatus.WAITLISTED) {
            booking.setStatus(BookingStatus.WAITLISTED);
            return;
        }

        booking.setStatus(BookingStatus.FAILED);
        bookingRepository.saveAndFlush(booking);
        throw new BookingCreationException(
                booking.getId(),
                "Unsupported reservation status: " + reservation.status()
        );
    }

    private Booking findOwnedBooking(Long userId, Long id) {
        return bookingRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    private void validatePendingPayment(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Only PENDING_PAYMENT bookings can complete payment");
        }
    }

    private void validateReservationPresent(Booking booking) {
        if (booking.getReservationId() == null) {
            throw new BadRequestException("Booking has no reservation to complete payment");
        }
    }
}
