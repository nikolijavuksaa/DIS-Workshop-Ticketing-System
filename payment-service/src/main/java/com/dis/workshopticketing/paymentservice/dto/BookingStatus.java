package com.dis.workshopticketing.paymentservice.dto;

public enum BookingStatus {
    INITIATING,
    PENDING_PAYMENT,
    WAITLISTED,
    CONFIRMED,
    PAYMENT_FAILED,
    CANCELLED,
    FAILED
}
