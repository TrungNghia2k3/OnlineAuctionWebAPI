package com.ntn.auction.exception;

import com.ntn.auction.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Validation error: {}", e.getMessage());
        return ApiResponse.<Void>builder()
                .code(400)
                .message(e.getMessage())
                .build();
    }

    @ExceptionHandler(BidException.class)
    public ApiResponse<Void> handleBidException(BidException e) {
        log.error("Bid error: {}", e.getMessage());
        return (ApiResponse.<Void>builder()
                .code(400)
                .message(e.getMessage())
                .build());
    }

    @ExceptionHandler(ItemNotFoundException.class)
    public ApiResponse<Void> handleItemNotFoundException(ItemNotFoundException e) {
        log.error("Item not found: {}", e.getMessage());
        return ApiResponse.<Void>builder()
                .code(404)
                .message(e.getMessage())
                .build();
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ApiResponse<Void> handleUserNotFoundException(UserNotFoundException e) {
        log.error("User not found: {}", e.getMessage());
        return ApiResponse.<Void>builder()
                .code(404)
                .message(e.getMessage())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException e) {

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation errors: {}", errors);
        return ApiResponse.<Map<String, String>>builder()
                .code(400)
                .message("Validation failed")
                .result(errors)
                .build();
    }

    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Void> handleRuntimeException(RuntimeException e) {
        log.error("Runtime error: {}", e.getMessage(), e);
        return ApiResponse.<Void>builder()
                .code(500)
                .message("Internal server error: " + e.getMessage())
                .build();
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ApiResponse.<Void>builder()
                .code(500)
                .message("An unexpected error occurred: " + e.getMessage())
                .build();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Map<String, String>> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> errors = new HashMap<>();

        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            errors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }

        log.error("Constraint violations: {}", errors);
        return ApiResponse.<Map<String, String>>builder()
                .code(400)
                .message("Constraint violations occurred")
                .result(errors)
                .build();
    }
}
