package com.kamran.template.common.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standardized error response format for all API errors.
 * Used by GlobalExceptionHandler to return consistent error messages.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private int status;

    private String error;

    private String message;

    private String path;
}
