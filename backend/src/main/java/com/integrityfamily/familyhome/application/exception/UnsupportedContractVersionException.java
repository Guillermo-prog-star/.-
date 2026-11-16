package com.integrityfamily.familyhome.application.exception;

public class UnsupportedContractVersionException extends FamilyHomeProjectionException {
    public UnsupportedContractVersionException(String version) {
        super("Contract version not supported: " + version);
    }
}
