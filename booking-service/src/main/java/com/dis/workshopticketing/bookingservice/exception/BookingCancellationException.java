package com.dis.workshopticketing.bookingservice.exception;

public class BookingCancellationException extends RuntimeException {

    public BookingCancellationException(Long bookingId, Throwable cause) {
        super("Booking could not be cancelled: " + bookingId, cause);
    }
}
