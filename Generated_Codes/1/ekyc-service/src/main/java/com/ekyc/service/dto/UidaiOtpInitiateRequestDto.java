package com.ekyc.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Objects;

/**
* Data Transfer Object for UIDAI OTP initiation request.
* This class encapsulates the data required to initiate an OTP request with UIDAI.
*/
public class UidaiOtpInitiateRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
    * The Aadhaar number for which OTP is to be generated.
    * Must be a valid 12-digit Aadhaar number.
    */
    @NotBlank(message = "Aadhaar number is required")
    @Size(min = 12, max = 12, message = "Aadhaar number must be 12 digits")
    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar number must contain only digits")
    @JsonProperty("uid")
    private String aadhaarNumber;

    /**
    * The transaction ID for the OTP request.
    * This is a unique identifier for the transaction.
    */
    @JsonProperty("txnId")
    private String transactionId;

    /**
    * The type of OTP channel to be used.
    * For example: "SMS", "EMAIL", etc.
    */
    @JsonProperty("otpChannel")
    private String otpChannel;

    /**
    * Default constructor for UidaiOtpInitiateRequestDto.
    */
    public UidaiOtpInitiateRequestDto() {
    }

    /**
    * Parameterized constructor for UidaiOtpInitiateRequestDto.
    *
    * @param aadhaarNumber The Aadhaar number
    * @param transactionId The transaction ID
    * @param otpChannel    The OTP channel
    */
    public UidaiOtpInitiateRequestDto(String aadhaarNumber, String transactionId, String otpChannel) {
        this.aadhaarNumber = aadhaarNumber;
        this.transactionId = transactionId;
        this.otpChannel = otpChannel;
    }

    /**
    * Gets the Aadhaar number.
    *
    * @return The Aadhaar number
    */
    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    /**
    * Sets the Aadhaar number.
    *
    * @param aadhaarNumber The Aadhaar number to set
    */
    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = aadhaarNumber;
    }

    /**
    * Gets the transaction ID.
    *
    * @return The transaction ID
    */
    public String getTransactionId() {
        return transactionId;
    }

    /**
    * Sets the transaction ID.
    *
    * @param transactionId The transaction ID to set
    */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
    * Gets the OTP channel.
    *
    * @return The OTP channel
    */
    public String getOtpChannel() {
        return otpChannel;
    }

    /**
    * Sets the OTP channel.
    *
    * @param otpChannel The OTP channel to set
    */
    public void setOtpChannel(String otpChannel) {
        this.otpChannel = otpChannel;
    }

    /**
    * Returns a string representation of the UidaiOtpInitiateRequestDto.
    * Note: The Aadhaar number is masked for security reasons.
    *
    * @return A string representation of the object
    */
    @Override
    public String toString() {
        String maskedAadhaar = aadhaarNumber != null && aadhaarNumber.length() >= 4
        ? "XXXX-XXXX-" + aadhaarNumber.substring(aadhaarNumber.length() - 4)
        : "null";

        return "UidaiOtpInitiateRequestDto{" +
        "aadhaarNumber='" + maskedAadhaar + '\'' +
        ", transactionId='" + transactionId + '\'' +
        ", otpChannel='" + otpChannel + '\'' +
        '}';
    }

    /**
    * Compares this UidaiOtpInitiateRequestDto with another object for equality.
    *
    * @param o The object to compare with
    * @return true if the objects are equal, false otherwise
    */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UidaiOtpInitiateRequestDto that = (UidaiOtpInitiateRequestDto) o;
        return Objects.equals(aadhaarNumber, that.aadhaarNumber) &&
        Objects.equals(transactionId, that.transactionId) &&
        Objects.equals(otpChannel, that.otpChannel);
    }

    /**
    * Generates a hash code for this UidaiOtpInitiateRequestDto.
    *
    * @return The hash code
    */
    @Override
    public int hashCode() {
        return Objects.hash(aadhaarNumber, transactionId, otpChannel);
    }
}