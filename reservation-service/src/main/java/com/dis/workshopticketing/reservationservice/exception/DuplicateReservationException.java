package com.dis.workshopticketing.reservationservice.exception;

public class DuplicateReservationException extends RuntimeException {

    public DuplicateReservationException(Long bookingId) {
        super("Reservation already exists for booking: " + bookingId);
    }
}
