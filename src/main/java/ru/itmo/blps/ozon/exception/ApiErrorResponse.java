package ru.itmo.blps.ozon.exception;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private Map<String, String> validationErrors;
}
