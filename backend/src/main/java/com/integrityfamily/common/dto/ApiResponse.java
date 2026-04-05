package com.integrityfamily.common.dto;

/**
 * ApiResponse: DTO inmutable (Record) para estandarizar respuestas hacia el Frontend.
 * Incluye métodos estáticos de conveniencia para éxito y error.
 */
public record ApiResponse<T>(boolean success, T data, String message) {
    
    // Método para respuestas exitosas rápidas
    public static <T> ApiResponse<T> ok(T d) { 
        return new ApiResponse<>(true, d, "Operación exitosa"); 
    }

    // Método para respuestas exitosas con mensaje personalizado
    public static <T> ApiResponse<T> ok(T d, String m) { 
        return new ApiResponse<>(true, d, m); 
    }

    // ESTA ES LA ESTOCADA FINAL: El método que AuthController está buscando
    public static <T> ApiResponse<T> error(String m) { 
        return new ApiResponse<>(false, null, m); 
    }
}