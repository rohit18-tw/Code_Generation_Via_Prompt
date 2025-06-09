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
* Data Transfer Object for UIDAI OTP initiation response.
* This class represents the response received from the UIDAI service
* when initiating an OTP verification process.
*/
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UidaiOtpInitiateResponseDto implements Serializable {

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

    @JsonProperty("uidaiRefId")
    private String uidaiReferenceId;

    /**
    * Default constructor
    */
    public UidaiOtpInitiateResponseDto() {
        this.timestamp = LocalDateTime.now();
    }

    /**
    * Parameterized constructor
    *
    * @param transactionId    The transaction ID for the OTP request
    * @param status           The status of the OTP initiation request
    * @param message          The message from UIDAI service
    * @param errorCode        The error code if any
    * @param uidaiReferenceId The reference ID from UIDAI
    */
    public UidaiOtpInitiateResponseDto(String transactionId, String status, String message,
    String errorCode, String uidaiReferenceId) {
        this.transactionId = transactionId;
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
        this.uidaiReferenceId = uidaiReferenceId;
        this.timestamp = LocalDateTime.now();
    }

    /**
    * Get the transaction ID
    *
    * @return the transaction ID
    */
    public String getTransactionId() {
        return transactionId;
    }

    /**
    * Set the transaction ID
    *
    * @param transactionId the transaction ID to set
    */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
    * Get the status
    *
    * @return the status
    */
    public String getStatus() {
        return status;
    }

    /**
    * Set the status
    *
    * @param status the status to set
    */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
    * Get the message
    *
    * @return the message
    */
    public String getMessage() {
        return message;
    }

    /**
    * Set the message
    *
    * @param message the message to set
    */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
    * Get the error code
    *
    * @return the error code
    */
    public String getErrorCode() {
        return errorCode;
    }

    /**
    * Set the error code
    *
    * @param errorCode the error code to set
    */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
    * Get the timestamp
    *
    * @return the timestamp
    */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
    * Set the timestamp
    *
    * @param timestamp the timestamp to set
    */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
    * Get the UIDAI reference ID
    *
    * @return the UIDAI reference ID
    */
    public String getUidaiReferenceId() {
        return uidaiReferenceId;
    }

    /**
    * Set the UIDAI reference ID
    *
    * @param uidaiReferenceId the UIDAI reference ID to set
    */
    public void setUidaiReferenceId(String uidaiReferenceId) {
        this.uidaiReferenceId = uidaiReferenceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UidaiOtpInitiateResponseDto that = (UidaiOtpInitiateResponseDto) o;
        return Objects.equals(transactionId, that.transactionId) &&
        Objects.equals(status, that.status) &&
        Objects.equals(message, that.message) &&
        Objects.equals(errorCode, that.errorCode) &&
        Objects.equals(timestamp, that.timestamp) &&
        Objects.equals(uidaiReferenceId, that.uidaiReferenceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, status, message, errorCode, timestamp, uidaiReferenceId);
    }

    @Override
    public String toString() {
        return "UidaiOtpInitiateResponseDto{" +
        "transactionId='" + transactionId + '\'' +
        ", status='" + status + '\'' +
        ", message='" + message + '\'' +
        ", errorCode='" + errorCode + '\'' +
        ", timestamp=" + timestamp +
        ", uidaiReferenceId='" + uidaiReferenceId + '\'' +
        '}';
    }

    /**
    * Builder class for UidaiOtpInitiateResponseDto
    */
    public static class Builder {
        private String transactionId;
        private String status;
        private String message;
        private String errorCode;
        private String uidaiReferenceId;

        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder uidaiReferenceId(String uidaiReferenceId) {
            this.uidaiReferenceId = uidaiReferenceId;
            return this;
        }

        public UidaiOtpInitiateResponseDto build() {
            return new UidaiOtpInitiateResponseDto(
            transactionId,
            status,
            message,
            errorCode,
            uidaiReferenceId
            );
        }
    }
}