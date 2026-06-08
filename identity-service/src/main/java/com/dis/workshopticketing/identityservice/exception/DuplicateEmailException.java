package com.dis.workshopticketing.identityservice.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Email is already in use: " + email);
    }
}
