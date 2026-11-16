package com.integrityfamily.familyhome.application.exception;

public class FamilyHomeProjectionException extends RuntimeException {
    public FamilyHomeProjectionException(String message) {
        super(message);
    }
    public FamilyHomeProjectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
