package com.lealtixservice.exception;

import com.lealtixservice.dto.GenericResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        String message = "Errores de validación: " + String.join(", ", errors);
        return ResponseEntity.ok(new GenericResponse(400, message, errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        List<String> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
        }

        String message = "Errores de validación: " + String.join(", ", errors);
        return ResponseEntity.ok(new GenericResponse(400, message, errors));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<GenericResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), new ArrayList<>()));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<GenericResponse> handleEmailAlreadyRegisteredException(EmailAlreadyRegisteredException ex) {
        return ResponseEntity.ok(new GenericResponse(409, ex.getMessage(), new ArrayList<>()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<GenericResponse> handleBusinessRuleException(BusinessRuleException ex) {
        return ResponseEntity.ok(new GenericResponse(422, ex.getMessage(), new ArrayList<>()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<GenericResponse> handleResponseStatusException(ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        return ResponseEntity.ok(new GenericResponse(status, ex.getReason(), new ArrayList<>()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GenericResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.ok(new GenericResponse(400, ex.getMessage(), new ArrayList<>()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String message;
        
        if (ex.getCause() instanceof DateTimeParseException) {
            message = String.format("Formato de fecha inválido para el parámetro '%s'. " +
                    "Use formato ISO 8601: yyyy-MM-dd'T'HH:mm:ss (ejemplo: 2026-01-01T06:00:00) " +
                    "o yyyy-MM-dd'T'HH:mm:ss.SSS (ejemplo: 2026-01-01T06:00:00.000). " +
                    "También puede incluir zona horaria: 2026-01-01T06:00:00Z", paramName);
        } else {
            message = String.format("Valor inválido para el parámetro '%s': %s", paramName, ex.getValue());
        }
        
        return ResponseEntity.ok(new GenericResponse(400, message, new ArrayList<>()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<GenericResponse> handleDateTimeParseException(DateTimeParseException ex) {
        String message = "Formato de fecha inválido. " +
                "Use formato ISO 8601: yyyy-MM-dd'T'HH:mm:ss (ejemplo: 2026-01-01T06:00:00) " +
                "o yyyy-MM-dd'T'HH:mm:ss.SSS (ejemplo: 2026-01-01T06:00:00.000). " +
                "También puede incluir zona horaria: 2026-01-01T06:00:00Z";
        return ResponseEntity.ok(new GenericResponse(400, message, new ArrayList<>()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGenericException(Exception ex) {
        return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", new ArrayList<>()));
    }
}
