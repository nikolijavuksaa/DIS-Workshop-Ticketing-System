package com.dis.workshopticketing.bookingservice.exception;

public class BookingPaymentCompletionException extends RuntimeException {

    public BookingPaymentCompletionException(Long bookingId, String paymentOutcome, Throwable cause) {
        super("Booking payment " + paymentOutcome + " could not be completed: " + bookingId, cause);
    }
}
