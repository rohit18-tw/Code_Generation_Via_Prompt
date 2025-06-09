package com.ekyc.service.exception;

import com.ekyc.service.dto.ErrorResponseDto;
import com.ekyc.service.util.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* Global exception handler for standardized error responses across the application.
* This class intercepts exceptions thrown during request processing and converts them
* into consistent, well-structured error responses.
*/
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String GENERIC_ERROR_MESSAGE = "An unexpected error occurred while processing your request";
    private static final String VALIDATION_ERROR_MESSAGE = "Validation failed for the request";
    private static final String DATABASE_ERROR_MESSAGE = "A database error occurred while processing your request";
    private static final String NOT_FOUND_ERROR_MESSAGE = "The requested resource was not found";
    private static final String INVALID_REQUEST_ERROR_MESSAGE = "The request contains invalid parameters";

    /**
    * Handles custom EkycException and returns appropriate error response.
    *
    * @param ex The EkycException that was thrown
    * @param request The current request
    * @return A ResponseEntity containing error details
    */
    @ExceptionHandler(EkycException.class)
    public ResponseEntity<ErrorResponseDto> handleEkycException(EkycException ex, WebRequest request) {
        logger.error("EkycException occurred: {}", ex.getMessage(), ex);

        ErrorResponseDto errorResponse = new ErrorResponseDto();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
    * Handles validation exceptions from @Valid annotations.
    *
    * @param ex The MethodArgumentNotValidException that was thrown
    * @param headers The headers for the response
    * @param status The status code for the response
    * @param request The current request
    * @return A ResponseEntity containing validation error details
    */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
    MethodArgumentNotValidException ex,
    HttpHeaders headers,
    HttpStatus status,
    WebRequest request) {

        logger.error("Validation error occurred: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponseDto errorResponse = new ErrorResponseDto();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.setMessage(VALIDATION_ERROR_MESSAGE);
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

        List<ErrorResponseDto.ValidationError> validationErrors = new ArrayList<>();
        errors.forEach((field, message) -> {
            ErrorResponseDto.ValidationError validationError = new ErrorResponseDto.ValidationError();
            validationError.setField(field);
            validationError.setMessage(message);
            validationErrors.add(validationError);
        });

        errorResponse.setValidationErrors(validationErrors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
    * Handles constraint violation exceptions from @Validated annotations.
    *
    * @param ex The ConstraintViolationException that was thrown
    * @param request The current request
    * @return A ResponseEntity containing validation error details
    */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolationException(
    ConstraintViolationException ex, WebRequest request) {

        logger.error("Constraint violation occurred: {}", ex.getMessage());

        List<ErrorResponseDto.ValidationError> validationErrors = ex.getConstraintViolations().stream()
        .map(violation -> {
            ErrorResponseDto.ValidationError validationError = new ErrorResponseDto.ValidationError();
            String propertyPath = violation.getPropertyPath().toString();
            String field = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
            validationError.setField(field);
            validationError.setMessage(violation.getMessage());
            return validationError;
        })
        .collect(Collectors.toList());

        ErrorResponseDto errorResponse = new ErrorResponseDto();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.setMessage(VALIDATION_ERROR_MESSAGE);
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
        errorResponse.setValidationErrors(validationErrors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
    * Handles exceptions for missing request parameters.
    *
    * @param ex The MissingServletRequestParameterException that was thrown
    * @param headers The headers for the response
    * @param status The status code for the response
    * @param request The current request
    * @return A ResponseEntity containing error details
    */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
    MissingServletRequestParameterException ex,
    HttpHeaders headers,
    HttpStatus status,
    WebRequest request) {

        logger.error("Missing request parameter: {}", ex.getMessage());

        ErrorResponseDto errorResponse = new ErrorResponseDto();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.setMessage("Required parameter '" + ex.getParameterName() + "' is missing");
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
    * Handles exceptions for method argument type mismatch.
    *
    * @param ex The MethodArgumentTypeMismatchException that was thrown
    * @param request The current request
    * @return A ResponseEntity containing error details
    */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentTypeMismatch(
    MethodArgumentTypeMismatchException ex, WebRequest request) {

        logger.error("Method argument type mismatch: {}", ex.getMessage());

        String errorMessage = String.format("Parameter '%s' should be of type '%s'",
        ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ErrorResponseDto errorResponse = new ErrorResponseDto();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.setMessage(errorMessage);
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
    * Handles exceptions for invalid JSON in request body.
    *
    * @param ex The HttpMessageNotReadableException that was thrown
    * @param headers The headers for the response
    * @param status The status code for the response
    * @param request The current request
    * @return A ResponseEntity containing error details
    */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
    HttpMessageNotReadableException ex,
    HttpHeaders headers,
    HttpStatus status,
    WebRequest request) {

        logger.error("Message not readable: {}", ex.getMessage());

        ErrorResponseDto errorResponse = new ErrorResponseDto();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.setMessage("Invalid request body: " + ex.getMessage());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
    * Handles entity not found exceptions.
    *
    * @param ex The EntityNotFoundException that was thrown
    * @param request The current request
    * @return A ResponseEntity containing error details
    */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleEntityNotFoundException(
    EntityNotFoundException ex, WebRequest request) {

        logger.error("Entity not found: {}", ex.getMessage());

        ErrorResponseDto errorResponse = new ErrorResponseDto();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.NOT_FOUND.value());
        errorResponse.setError(HttpStatus.NOT_FOUND.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage() != null ? ex.getMessage() : NOT_FOUND_ERROR_MESSAGE);
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
    * Handles database access exceptions.
    *
    * @param ex The DataAccessException that was thrown
    * @param request The current request
    * @return A ResponseEntity containing error details
    */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponseDto> handleDataAccessException(
    DataAccessException ex, WebRequest request) {

        logger.error("Database error occurred: {}", ex.getMessage(), ex);

        ErrorResponseDto errorResponse = new ErrorResponseDto();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        errorResponse.setMessage(DATABASE_ERROR_MESSAGE);
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
    * Handles WebClient response exceptions from external API calls.
    *
    * @param ex The WebClientResponseException that was thrown
    * @param request The current request
    * @return A ResponseEntity containing error details
    */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponseDto> handleWebClientResponseException(
    WebClientResponseException ex, WebRequest request) {

        logger.error("External API error: {} - {}", ex.getStatusCode(), ex.getMessage());

        ErrorResponseDto errorResponse = new ErrorResponseDto();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(ex.getStatusCode().value());
        errorResponse.setError(ex.getStatusText());
        errorResponse.setMessage("Error from external service: " + ex.getMessage());
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(errorResponse, ex.getStatusCode());
    }

    /**
    * Handles all other uncaught exceptions.
    *
    * @param ex The Exception that was thrown
    * @param request The current request
    * @return A ResponseEntity containing error details
    */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex, WebRequest request) {
        logger.error("Unhandled exception occurred: {}", ex.getMessage(), ex);

        ErrorResponseDto errorResponse = new ErrorResponseDto();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        errorResponse.setMessage(GENERIC_ERROR_MESSAGE);
        errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}