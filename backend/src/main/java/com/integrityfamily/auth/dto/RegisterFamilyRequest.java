// backend/src/main/java/com/integrityfamily/auth/dto/RegisterFamilyRequest.java
package com.integrityfamily.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterFamilyRequest(
        @NotBlank(message = "El nombre de la familia es obligatorio") @Size(min = 2, max = 150) String familyName,

        @NotBlank @Size(min = 2, max = 150) String fullName,

        @NotBlank @Email String email,

        @NotBlank @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", message = "MÃƒÂ­nimo 8 caracteres, con mayÃƒÂºscula, minÃƒÂºscula y dÃƒÂ­gito") String password,

        @NotBlank String confirmPassword,
        
        @NotBlank(message = "El municipio es obligatorio") String municipio,
        
        @NotBlank(message = "El código de país es obligatorio") String countryCode,
        
        @NotBlank(message = "El código de departamento es obligatorio") String departmentCode) {
    @AssertTrue(message = "Las contraseÃƒÂ±as no coinciden")
    public boolean isPasswordsMatch() {
        return password != null && password.equals(confirmPassword);
    }
}


