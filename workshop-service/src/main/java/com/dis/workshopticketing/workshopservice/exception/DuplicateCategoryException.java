package com.dis.workshopticketing.workshopservice.exception;

public class DuplicateCategoryException extends RuntimeException {

    public DuplicateCategoryException(String name) {
        super("Category is already in use: " + name);
    }
}
