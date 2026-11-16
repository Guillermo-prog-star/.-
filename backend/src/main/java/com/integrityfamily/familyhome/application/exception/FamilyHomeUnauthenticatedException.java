package com.integrityfamily.familyhome.application.exception;

public class FamilyHomeUnauthenticatedException extends FamilyHomeProjectionException {
    public FamilyHomeUnauthenticatedException() {
        super("User is not authenticated");
    }
}
