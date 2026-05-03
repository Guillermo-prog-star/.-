// backend/src/main/java/com/integrityfamily/auth/exception/AccountLockedException.java
package com.integrityfamily.auth.exception;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException() {
        super("Cuenta bloqueada temporalmente. Intenta mÃƒÂ¡s tarde.");
    }
}


