package com.integrityfamily.common.exception;
import com.integrityfamily.common.dto.ErrorResponse;
import org.springframework.http.*; import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime; import java.util.LinkedHashMap;
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> h(NotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(false,ex.getMessage(),LocalDateTime.now(),null));
    }
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> h(BusinessException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(false,ex.getMessage(),LocalDateTime.now(),null));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> h(MethodArgumentNotValidException ex) {
        var e = new LinkedHashMap<String,String>();
        ex.getBindingResult().getFieldErrors().forEach((FieldError f)->e.put(f.getField(),f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorResponse(false,"Errores de validación",LocalDateTime.now(),e));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> h(Exception ex) {
        return ResponseEntity.status(500).body(new ErrorResponse(false,"Error: "+ex.getMessage(),LocalDateTime.now(),null));
    }
}
