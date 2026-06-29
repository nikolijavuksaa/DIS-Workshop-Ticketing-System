package com.dis.workshopticketing.paymentservice.exception;

public class PaymentProcessingException extends RuntimeException {

    public PaymentProcessingException(Long paymentId, Throwable cause) {
        super("Payment could not update booking outcome: " + paymentId, cause);
    }
}
