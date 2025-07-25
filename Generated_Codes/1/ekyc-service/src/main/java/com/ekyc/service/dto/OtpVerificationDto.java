package com.ekyc.service.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
* Data Transfer Object for OTP verification.
* This class encapsulates the data required for verifying an OTP (One-Time Password)
* during the eKYC process.
*/
public class OtpVerificationDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
    * The reference ID associated with the OTP verification request.
    * This is typically generated during the initial eKYC request.
    */
    @NotBlank(message = "Reference ID is required")
    private String referenceId;

    /**
    * The OTP code entered by the user for verification.
    * Must be a numeric value with a specific length.
    */
    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^[0-9]+$", message = "OTP must contain only numeric characters")
    @Size(min = 4, max = 8, message = "OTP must be between 4 and 8 digits")
    private String otp;

    /**
    * The timestamp when this verification request was created.
    */
    private LocalDateTime requestTimestamp;

    /**
    * Default constructor for OtpVerificationDto.
    */
    public OtpVerificationDto() {
        this.requestTimestamp = LocalDateTime.now();
    }

    /**
    * Parameterized constructor for OtpVerificationDto.
    *
    * @param referenceId The reference ID for the verification
    * @param otp The OTP code to verify
    */
    public OtpVerificationDto(String referenceId, String otp) {
        this.referenceId = referenceId;
        this.otp = otp;
        this.requestTimestamp = LocalDateTime.now();
    }

    /**
    * Gets the reference ID.
    *
    * @return The reference ID
    */
    public String getReferenceId() {
        return referenceId;
    }

    /**
    * Sets the reference ID.
    *
    * @param referenceId The reference ID to set
    */
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    /**
    * Gets the OTP code.
    *
    * @return The OTP code
    */
    public String getOtp() {
        return otp;
    }

    /**
    * Sets the OTP code.
    *
    * @param otp The OTP code to set
    */
    public void setOtp(String otp) {
        this.otp = otp;
    }

    /**
    * Gets the request timestamp.
    *
    * @return The request timestamp
    */
    public LocalDateTime getRequestTimestamp() {
        return requestTimestamp;
    }

    /**
    * Sets the request timestamp.
    *
    * @param requestTimestamp The request timestamp to set
    */
    public void setRequestTimestamp(LocalDateTime requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OtpVerificationDto that = (OtpVerificationDto) o;
        return Objects.equals(referenceId, that.referenceId) &&
        Objects.equals(otp, that.otp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceId, otp);
    }

    @Override
    public String toString() {
        // Mask OTP for security in logs
        return "OtpVerificationDto{" +
        "referenceId='" + referenceId + '\'' +
        ", otp='****'" +
        ", requestTimestamp=" + requestTimestamp +
        '}';
    }
}