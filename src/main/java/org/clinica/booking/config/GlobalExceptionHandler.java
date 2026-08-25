package org.clinica.booking.config;

import lombok.extern.slf4j.Slf4j;
import org.clinica.booking.auth.exception.EmailAlreadyRegisteredException;
import org.clinica.booking.auth.exception.InvalidCredentialsException;
import org.clinica.booking.integration.holidays.exception.HolidayProviderException;
import org.clinica.booking.patient.exception.PatientNotFoundException;
import org.clinica.booking.schedule.dto.ConfirmedSlot;
import org.clinica.booking.schedule.exception.AvailabilitySlotNotFoundException;
import org.clinica.booking.schedule.exception.BlockedDateNotFoundException;
import org.clinica.booking.schedule.exception.ConfirmedAppointmentConflictException;
import org.clinica.booking.schedule.exception.HolidayBlockNotDeletableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    public record ErrorResponse(int status, String message, OffsetDateTime timestamp) {

        static ErrorResponse of(HttpStatus status, String message) {
            return new ErrorResponse(status.value(), message, OffsetDateTime.now());
        }
    }

    public record ValidationErrorResponse(int status, String message, Map<String, String> fieldErrors,
                                          OffsetDateTime timestamp) {
    }

    public record ConflictErrorResponse(int status, String message, List<ConfirmedSlot> conflicts,
                                        OffsetDateTime timestamp) {
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED, ex.getMessage()));
    }

    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePatientNotFound(PatientNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler({AvailabilitySlotNotFoundException.class, BlockedDateNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleScheduleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(HolidayBlockNotDeletableException.class)
    public ResponseEntity<ErrorResponse> handleHolidayBlockNotDeletable(HolidayBlockNotDeletableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(ConfirmedAppointmentConflictException.class)
    public ResponseEntity<ConflictErrorResponse> handleConfirmedAppointmentConflict(
            ConfirmedAppointmentConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ConflictErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage(),
                        ex.getConflicts(), OffsetDateTime.now()));
    }

    @ExceptionHandler(HolidayProviderException.class)
    public ResponseEntity<ErrorResponse> handleHolidayProvider(HolidayProviderException ex) {
        log.error("Holiday provider unavailable", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE, "Holiday provider unavailable"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN, "Access denied"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                        (first, second) -> first));

        return ResponseEntity.badRequest()
                .body(new ValidationErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation failed",
                        fieldErrors, OffsetDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error"));
    }
}
