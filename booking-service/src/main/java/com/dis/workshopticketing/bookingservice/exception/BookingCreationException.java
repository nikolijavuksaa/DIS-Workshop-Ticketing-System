package com.dis.workshopticketing.bookingservice.exception;

public class BookingCreationException extends RuntimeException {

    public BookingCreationException(Long bookingId, Throwable cause) {
        super("Booking could not reserve a workshop session: " + bookingId, cause);
    }

    public BookingCreationException(Long bookingId, String message) {
        super("Booking could not reserve a workshop session: " + bookingId + ". " + message);
    }
}
