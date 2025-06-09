package com.mock.uidai.dto;

import java.io.Serializable;
import java.util.Objects;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;

/**
* Data Transfer Object for error responses in the UIDAI service.
* This class encapsulates error information to be returned to clients
* when an operation fails.
*/
@Validated
public class ErrorResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Error code cannot be null")
    private String errorCode;

    @NotBlank(message = "Error message cannot be blank")
    private String errorMessage;

    private String requestId;

    private String timestamp;

    /**
    * Default constructor for ErrorResponseDto.
    */
    public ErrorResponseDto() {
    }

    /**
    * Constructs an ErrorResponseDto with specified error code and message.
    *
    * @param errorCode The error code identifying the type of error
    * @param errorMessage A descriptive message about the error
    */
    public ErrorResponseDto(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
    * Constructs an ErrorResponseDto with specified error code, message, and request ID.
    *
    * @param errorCode The error code identifying the type of error
    * @param errorMessage A descriptive message about the error
    * @param requestId The ID of the request that resulted in this error
    */
    public ErrorResponseDto(String errorCode, String errorMessage, String requestId) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.requestId = requestId;
    }

    /**
    * Constructs an ErrorResponseDto with all fields.
    *
    * @param errorCode The error code identifying the type of error
    * @param errorMessage A descriptive message about the error
    * @param requestId The ID of the request that resulted in this error
    * @param timestamp The timestamp when the error occurred
    */
    public ErrorResponseDto(String errorCode, String errorMessage, String requestId, String timestamp) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.requestId = requestId;
        this.timestamp = timestamp;
    }

    /**
    * Gets the error code.
    *
    * @return The error code
    */
    public String getErrorCode() {
        return errorCode;
    }

    /**
    * Sets the error code.
    *
    * @param errorCode The error code to set
    */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
    * Gets the error message.
    *
    * @return The error message
    */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
    * Sets the error message.
    *
    * @param errorMessage The error message to set
    */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
    * Gets the request ID.
    *
    * @return The request ID
    */
    public String getRequestId() {
        return requestId;
    }

    /**
    * Sets the request ID.
    *
    * @param requestId The request ID to set
    */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
    * Gets the timestamp.
    *
    * @return The timestamp
    */
    public String getTimestamp() {
        return timestamp;
    }

    /**
    * Sets the timestamp.
    *
    * @param timestamp The timestamp to set
    */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorResponseDto that = (ErrorResponseDto) o;
        return Objects.equals(errorCode, that.errorCode) &&
        Objects.equals(errorMessage, that.errorMessage) &&
        Objects.equals(requestId, that.requestId) &&
        Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorCode, errorMessage, requestId, timestamp);
    }

    @Override
    public String toString() {
        return "ErrorResponseDto{" +
        "errorCode='" + errorCode + '\'' +
        ", errorMessage='" + errorMessage + '\'' +
        ", requestId='" + requestId + '\'' +
        ", timestamp='" + timestamp + '\'' +
        '}';
    }
}