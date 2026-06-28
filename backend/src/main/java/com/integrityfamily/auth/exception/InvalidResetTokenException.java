// backend/src/main/java/com/integrityfamily/auth/exception/InvalidResetTokenException.java
package com.integrityfamily.auth.exception;

public class InvalidResetTokenException extends RuntimeException {
    public InvalidResetTokenException() {
        super("Token de recuperación inválido o expirado");
    }
}


