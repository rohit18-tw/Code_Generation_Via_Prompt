package com.ekyc.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
* Data Transfer Object for standardized error responses.
* This class provides a consistent structure for error responses across the application.
*/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String errorCode;
    private String message;
    private String path;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;

    private List<ValidationError> validationErrors;

    /**
    * Default constructor.
    */
    public ErrorResponseDto() {
        this.timestamp = LocalDateTime.now();
    }

    /**
    * Constructor with error code and message.
    *
    * @param errorCode the error code
    * @param message the error message
    */
    public ErrorResponseDto(String errorCode, String message) {
        this();
        this.errorCode = errorCode;
        this.message = message;
    }

    /**
    * Constructor with error code, message, and path.
    *
    * @param errorCode the error code
    * @param message the error message
    * @param path the request path that generated the error
    */
    public ErrorResponseDto(String errorCode, String message, String path) {
        this(errorCode, message);
        this.path = path;
    }

    /**
    * Gets the error code.
    *
    * @return the error code
    */
    public String getErrorCode() {
        return errorCode;
    }

    /**
    * Sets the error code.
    *
    * @param errorCode the error code to set
    */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
    * Gets the error message.
    *
    * @return the error message
    */
    public String getMessage() {
        return message;
    }

    /**
    * Sets the error message.
    *
    * @param message the error message to set
    */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
    * Gets the request path that generated the error.
    *
    * @return the path
    */
    public String getPath() {
        return path;
    }

    /**
    * Sets the request path that generated the error.
    *
    * @param path the path to set
    */
    public void setPath(String path) {
        this.path = path;
    }

    /**
    * Gets the timestamp when the error occurred.
    *
    * @return the timestamp
    */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
    * Sets the timestamp when the error occurred.
    *
    * @param timestamp the timestamp to set
    */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
    * Gets the validation errors.
    *
    * @return the list of validation errors
    */
    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    /**
    * Sets the validation errors.
    *
    * @param validationErrors the validation errors to set
    */
    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
    }

    /**
    * Adds a validation error to the list.
    *
    * @param field the field that has the validation error
    * @param message the validation error message
    */
    public void addValidationError(String field, String message) {
        if (validationErrors == null) {
            validationErrors = new ArrayList<>();
        }
        validationErrors.add(new ValidationError(field, message));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorResponseDto that = (ErrorResponseDto) o;
        return Objects.equals(errorCode, that.errorCode) &&
        Objects.equals(message, that.message) &&
        Objects.equals(path, that.path) &&
        Objects.equals(timestamp, that.timestamp) &&
        Objects.equals(validationErrors, that.validationErrors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorCode, message, path, timestamp, validationErrors);
    }

    @Override
    public String toString() {
        return "ErrorResponseDto{" +
        "errorCode='" + errorCode + '\'' +
        ", message='" + message + '\'' +
        ", path='" + path + '\'' +
        ", timestamp=" + timestamp +
        ", validationErrors=" + validationErrors +
        '}';
    }

    /**
    * Inner class to represent individual validation errors.
    */
    public static class ValidationError implements Serializable {

        private static final long serialVersionUID = 1L;

        private String field;
        private String message;

        /**
        * Default constructor.
        */
        public ValidationError() {
        }

        /**
        * Constructor with field and message.
        *
        * @param field the field with the validation error
        * @param message the validation error message
        */
        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        /**
        * Gets the field with the validation error.
        *
        * @return the field
        */
        public String getField() {
            return field;
        }

        /**
        * Sets the field with the validation error.
        *
        * @param field the field to set
        */
        public void setField(String field) {
            this.field = field;
        }

        /**
        * Gets the validation error message.
        *
        * @return the message
        */
        public String getMessage() {
            return message;
        }

        /**
        * Sets the validation error message.
        *
        * @param message the message to set
        */
        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ValidationError that = (ValidationError) o;
            return Objects.equals(field, that.field) &&
            Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field, message);
        }

        @Override
        public String toString() {
            return "ValidationError{" +
            "field='" + field + '\'' +
            ", message='" + message + '\'' +
            '}';
        }
    }
}