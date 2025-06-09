package com.mock.uidai.dto;

import java.io.Serializable;
import java.util.Objects;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.springframework.validation.annotation.Validated;

/**
* Data Transfer Object for OTP initiation response from UIDAI service.
* This class represents the response structure when an OTP initiation request is processed.
*/
@Validated
public class OtpInitiateResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
    * Status code of the OTP initiation response.
    * Typically "SUCCESS" or an error code.
    */
    @NotBlank(message = "Status code cannot be blank")
    private String statusCode;

    /**
    * Status message providing details about the OTP initiation result.
    */
    @NotBlank(message = "Status message cannot be blank")
    private String statusMessage;

    /**
    * Transaction ID associated with the OTP request.
    * This ID can be used for subsequent operations related to this OTP.
    */
    @NotBlank(message = "Transaction ID cannot be blank")
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Transaction ID must contain only alphanumeric characters and hyphens")
    private String txnId;

    /**
    * Default constructor.
    */
    public OtpInitiateResponseDto() {
    }

    /**
    * Parameterized constructor to create a response with all required fields.
    *
    * @param statusCode    The status code of the response
    * @param statusMessage The detailed status message
    * @param txnId         The transaction ID for the OTP request
    */
    public OtpInitiateResponseDto(String statusCode, String statusMessage, String txnId) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.txnId = txnId;
    }

    /**
    * Gets the status code.
    *
    * @return The status code
    */
    public String getStatusCode() {
        return statusCode;
    }

    /**
    * Sets the status code.
    *
    * @param statusCode The status code to set
    */
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    /**
    * Gets the status message.
    *
    * @return The status message
    */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
    * Sets the status message.
    *
    * @param statusMessage The status message to set
    */
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    /**
    * Gets the transaction ID.
    *
    * @return The transaction ID
    */
    public String getTxnId() {
        return txnId;
    }

    /**
    * Sets the transaction ID.
    *
    * @param txnId The transaction ID to set
    */
    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OtpInitiateResponseDto that = (OtpInitiateResponseDto) o;
        return Objects.equals(statusCode, that.statusCode) &&
        Objects.equals(statusMessage, that.statusMessage) &&
        Objects.equals(txnId, that.txnId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, statusMessage, txnId);
    }

    @Override
    public String toString() {
        // Masking transaction ID for security in logs
        String maskedTxnId = txnId != null && txnId.length() > 4
        ? txnId.substring(0, 2) + "****" + txnId.substring(txnId.length() - 2)
        : "****";

        return "OtpInitiateResponseDto{" +
        "statusCode='" + statusCode + '\'' +
        ", statusMessage='" + statusMessage + '\'' +
        ", txnId='" + maskedTxnId + '\'' +
        '}';
    }
}