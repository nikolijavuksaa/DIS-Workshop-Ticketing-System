package com.dis.workshopticketing.reservationservice.exception;

public class DuplicateInventoryException extends RuntimeException {

    public DuplicateInventoryException(Long workshopSessionId) {
        super("Inventory already exists for workshop session: " + workshopSessionId);
    }
}
