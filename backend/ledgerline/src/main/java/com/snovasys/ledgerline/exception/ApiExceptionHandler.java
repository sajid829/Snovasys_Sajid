package com.snovasys.ledgerline.exception;

import com.snovasys.ledgerline.dto.ApiErrorResponse;
import com.snovasys.ledgerline.exception.DuplicateEmailException;
import com.snovasys.ledgerline.exception.IdempotencyConflictException;
import com.snovasys.ledgerline.exception.InsufficientFundsException;
import com.snovasys.ledgerline.exception.InvalidCredentialsException;
import com.snovasys.ledgerline.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(
            DuplicateEmailException exception, HttpServletRequest request) {
        return errorResponse(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(HttpServletRequest request) {
        return errorResponse(
                HttpStatus.UNAUTHORIZED,
                "Email or password is incorrect.",
                request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(HttpServletRequest request) {
        return errorResponse(HttpStatus.NOT_FOUND, "The requested resource was not found.", request.getRequestURI());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleIdempotencyConflict(HttpServletRequest request) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "The idempotency key conflicts with an earlier transaction.",
                request.getRequestURI());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientFunds(HttpServletRequest request) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "The account has insufficient funds.",
                request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed.",
                request.getRequestURI());
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(HttpServletRequest request) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "The request is invalid.",
                request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(HttpServletRequest request) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "The request conflicts with an existing record.",
                request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class)
                .error("Unhandled API error for {} ({})", request.getRequestURI(), exception.getClass().getSimpleName());
        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> errorResponse(
            HttpStatus status, String message, String path) {
        var body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path);
        return ResponseEntity.status(status).body(body);
    }
}
