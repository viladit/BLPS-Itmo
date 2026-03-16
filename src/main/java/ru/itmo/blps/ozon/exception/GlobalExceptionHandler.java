package ru.itmo.blps.ozon.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderNotFound(OrderNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidState(InvalidOrderStateException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status,
                                                           String message,
                                                           Map<String, String> validationErrors) {
        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                validationErrors
        );
        return ResponseEntity.status(status).body(response);
    }
}
