package com.integrityfamily.common.dto;

/**
 * ApiResponse: DTO inmutable (Record) para estandarizar respuestas hacia el Frontend.
 * Incluye mÃƒÂ©todos estÃƒÂ¡ticos de conveniencia para ÃƒÂ©xito y error.
 */
public record ApiResponse<T>(boolean success, T data, String message) {
    
    // MÃƒÂ©todo para respuestas exitosas rÃƒÂ¡pidas
    public static <T> ApiResponse<T> ok(T d) { 
        return new ApiResponse<>(true, d, "OperaciÃƒÂ³n exitosa"); 
    }

    // MÃƒÂ©todo para respuestas exitosas con mensaje personalizado
    public static <T> ApiResponse<T> ok(T d, String m) { 
        return new ApiResponse<>(true, d, m); 
    }

    // ESTA ES LA ESTOCADA FINAL: El mÃƒÂ©todo que AuthController estÃƒÂ¡ buscando
    public static <T> ApiResponse<T> error(String m) { 
        return new ApiResponse<>(false, null, m); 
    }
}


