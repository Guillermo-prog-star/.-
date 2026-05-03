// backend/src/main/java/com/integrityfamily/auth/dto/ResetPasswordRequest.java
package com.integrityfamily.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank String token,

        @NotBlank @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", message = "MÃƒÂ­nimo 8 caracteres, con mayÃƒÂºscula, minÃƒÂºscula y dÃƒÂ­gito") String newPassword) {
}


