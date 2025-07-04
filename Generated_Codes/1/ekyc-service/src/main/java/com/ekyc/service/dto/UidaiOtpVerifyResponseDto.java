package com.ekyc.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
* Data Transfer Object for UIDAI OTP verification response.
* This class represents the response received from the UIDAI service
* after an OTP verification request has been processed.
*/
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UidaiOtpVerifyResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("txnId")
    private String transactionId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("isVerified")
    private boolean verified;

    /**
    * Default constructor for UidaiOtpVerifyResponseDto.
    */
    public UidaiOtpVerifyResponseDto() {
        this.timestamp = LocalDateTime.now();
    }

    /**
    * Parameterized constructor for UidaiOtpVerifyResponseDto.
    *
    * @param transactionId The transaction ID of the OTP verification
    * @param status The status of the OTP verification
    * @param message The message associated with the verification response
    * @param errorCode The error code if verification failed
    * @param verified Whether the OTP verification was successful
    */
    public UidaiOtpVerifyResponseDto(String transactionId, String status, String message,
    String errorCode, boolean verified) {
        this.transactionId = transactionId;
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
        this.verified = verified;
        this.timestamp = LocalDateTime.now();
    }

    /**
    * Gets the transaction ID of the OTP verification.
    *
    * @return The transaction ID
    */
    public String getTransactionId() {
        return transactionId;
    }

    /**
    * Sets the transaction ID of the OTP verification.
    *
    * @param transactionId The transaction ID to set
    */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
    * Gets the status of the OTP verification.
    *
    * @return The status
    */
    public String getStatus() {
        return status;
    }

    /**
    * Sets the status of the OTP verification.
    *
    * @param status The status to set
    */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
    * Gets the message associated with the verification response.
    *
    * @return The message
    */
    public String getMessage() {
        return message;
    }

    /**
    * Sets the message associated with the verification response.
    *
    * @param message The message to set
    */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
    * Gets the error code if verification failed.
    *
    * @return The error code
    */
    public String getErrorCode() {
        return errorCode;
    }

    /**
    * Sets the error code if verification failed.
    *
    * @param errorCode The error code to set
    */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
    * Gets the timestamp of the verification response.
    *
    * @return The timestamp
    */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
    * Sets the timestamp of the verification response.
    *
    * @param timestamp The timestamp to set
    */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
    * Checks if the OTP verification was successful.
    *
    * @return true if verified, false otherwise
    */
    public boolean isVerified() {
        return verified;
    }

    /**
    * Sets whether the OTP verification was successful.
    *
    * @param verified The verification status to set
    */
    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UidaiOtpVerifyResponseDto that = (UidaiOtpVerifyResponseDto) o;
        return verified == that.verified &&
        Objects.equals(transactionId, that.transactionId) &&
        Objects.equals(status, that.status) &&
        Objects.equals(message, that.message) &&
        Objects.equals(errorCode, that.errorCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, status, message, errorCode, verified);
    }

    @Override
    public String toString() {
        return "UidaiOtpVerifyResponseDto{" +
        "transactionId='" + transactionId + '\'' +
        ", status='" + status + '\'' +
        ", message='" + message + '\'' +
        ", errorCode='" + errorCode + '\'' +
        ", timestamp=" + timestamp +
        ", verified=" + verified +
        '}';
    }
}