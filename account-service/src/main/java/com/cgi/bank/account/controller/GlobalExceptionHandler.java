package com.cgi.bank.account.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.cgi.bank.account.controller.dto.ErrorResponseDTO;
import com.cgi.bank.account.exception.AccountNotFoundException;
import com.cgi.bank.account.exception.BalanceNotFoundException;
import com.cgi.bank.account.exception.InsufficientFundsException;
import com.cgi.bank.account.exception.InvalidCurrencyException;
import com.cgi.bank.account.exception.OptimisticLockingConflictException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for providing consistent error responses.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles AccountNotFoundException and maps it to a 404 Not Found response.
     *
     * @param ex the exception
     * @param request the current request
     * @return a ResponseEntity with error details
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccountNotFoundException(
            AccountNotFoundException ex, 
            HttpServletRequest request) {
        
        log.debug("Account not found: {}", ex.getMessage());
        
        return buildErrorResponse(
                ex,
                HttpStatus.NOT_FOUND,
                "Account not found",
                request.getRequestURI());
    }

    /**
     * Handles BalanceNotFoundException and maps it to a 400 Bad Request response.
     *
     * @param ex the exception
     * @param request the current request
     * @return a ResponseEntity with error details
     */
    @ExceptionHandler(BalanceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleBalanceNotFoundException(
            BalanceNotFoundException ex, 
            HttpServletRequest request) {
        
        log.debug("Balance not found: {}", ex.getMessage());
        
        return buildErrorResponse(
                ex,
                HttpStatus.BAD_REQUEST,
                "Balance not found",
                request.getRequestURI());
    }

    /**
     * Handles InsufficientFundsException and maps it to a 422 Unprocessable Entity response.
     *
     * @param ex the exception
     * @param request the current request
     * @return a ResponseEntity with error details
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponseDTO> handleInsufficientFundsException(
            InsufficientFundsException ex, 
            HttpServletRequest request) {
        
        log.debug("Insufficient funds: {}", ex.getMessage());
        
        return buildErrorResponse(
                ex,
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Insufficient funds",
                request.getRequestURI());
    }

    /**
     * Handles InvalidCurrencyException and maps it to a 400 Bad Request response.
     *
     * @param ex the exception
     * @param request the current request
     * @return a ResponseEntity with error details
     */
    @ExceptionHandler(InvalidCurrencyException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidCurrencyException(
            InvalidCurrencyException ex, 
            HttpServletRequest request) {
        
        log.debug("Invalid currency: {}", ex.getMessage());
        
        return buildErrorResponse(
                ex,
                HttpStatus.BAD_REQUEST,
                "Invalid currency",
                request.getRequestURI());
    }

    /**
     * Handles MethodArgumentNotValidException and maps it to a 400 Bad Request response.
     * Extracts validation errors from the BindingResult.
     *
     * @param ex the exception
     * @param request the current request
     * @return a ResponseEntity with error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, 
            HttpServletRequest request) {
        
        // Extract validation errors
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // Create a readable error message
        String errorMessage = errors.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
        
        log.debug("Validation error: {}", errorMessage);
        
        return buildErrorResponse(
                new Exception(errorMessage),
                HttpStatus.BAD_REQUEST,
                "Validation error",
                request.getRequestURI());
    }

    /**
     * Handles HttpMessageNotReadableException and maps it to a 400 Bad Request response.
     *
     * @param ex the exception
     * @param request the current request
     * @return a ResponseEntity with error details
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, 
            HttpServletRequest request) {
        
        log.debug("Malformed request body: {}", ex.getMessage());
        
        return buildErrorResponse(
                ex,
                HttpStatus.BAD_REQUEST,
                "Malformed request body",
                request.getRequestURI());
    }

    /**
     * Handles ObjectOptimisticLockingFailureException and maps it to a 409 Conflict response.
     *
     * @param ex the exception
     * @param request the current request
     * @return a ResponseEntity with error details
     */
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockingConflictException.class})
    public ResponseEntity<ErrorResponseDTO> handleOptimisticLockingFailureException(
            Exception ex, 
            HttpServletRequest request) {
        
        log.debug("Optimistic locking conflict: {}", ex.getMessage());
        
        return buildErrorResponse(
                ex,
                HttpStatus.CONFLICT,
                "Concurrent modification conflict",
                request.getRequestURI());
    }

    /**
     * Handles IllegalArgumentException and maps it to a 400 Bad Request response.
     *
     * @param ex the exception
     * @param request the current request
     * @return a ResponseEntity with error details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(
            IllegalArgumentException ex, 
            HttpServletRequest request) {
        
        log.debug("Invalid argument: {}", ex.getMessage());
        
        return buildErrorResponse(
                ex,
                HttpStatus.BAD_REQUEST,
                "Invalid argument",
                request.getRequestURI());
    }

    /**
     * Fallback handler for any unhandled exceptions.
     * Maps to a 500 Internal Server Error response.
     *
     * @param ex the exception
     * @param request the current request
     * @return a ResponseEntity with error details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(
            Exception ex, 
            HttpServletRequest request) {
        
        log.error("Unhandled exception", ex);
        
        return buildErrorResponse(
                ex,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                request.getRequestURI());
    }

    /**
     * Builds a standardized error response.
     *
     * @param exception the caught exception
     * @param status the HTTP status
     * @param error the error type description
     * @param path the request path
     * @return a ResponseEntity containing the error details
     */
    private ResponseEntity<ErrorResponseDTO> buildErrorResponse(
            Exception exception,
            HttpStatus status,
            String error,
            String path) {
        
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                LocalDateTime.now(),
                status.value(),
                error,
                exception.getMessage(),
                path
        );
        
        return new ResponseEntity<>(errorResponse, status);
    }
} 