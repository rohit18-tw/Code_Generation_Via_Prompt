package com.mock.uidai.dto;

import java.io.Serializable;
import java.util.Objects;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;

/**
* Data Transfer Object for OTP verification request.
* This class represents the request payload for verifying an OTP sent to a user.
*/
@Validated
public class OtpVerifyRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
    * The transaction ID received during OTP initiation.
    * This field is required to identify the OTP transaction.
    */
    @NotBlank(message = "Transaction ID cannot be blank")
    @Size(max = 50, message = "Transaction ID cannot exceed 50 characters")
    private String txnId;

    /**
    * The OTP entered by the user for verification.
    * This field is required and must be numeric.
    */
    @NotBlank(message = "OTP cannot be blank")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be a 6-digit number")
    private String otp;

    /**
    * The Aadhaar number (UID) of the user.
    * This field is required and must follow the Aadhaar number format.
    */
    @NotBlank(message = "UID cannot be blank")
    @Pattern(regexp = "^[0-9]{12}$", message = "UID must be a 12-digit number")
    private String uid;

    /**
    * Default constructor.
    */
    public OtpVerifyRequestDto() {
    }

    /**
    * Parameterized constructor.
    *
    * @param txnId The transaction ID
    * @param otp   The OTP to verify
    * @param uid   The Aadhaar number
    */
    public OtpVerifyRequestDto(String txnId, String otp, String uid) {
        this.txnId = txnId;
        this.otp = otp;
        this.uid = uid;
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

    /**
    * Gets the OTP.
    *
    * @return The OTP
    */
    public String getOtp() {
        return otp;
    }

    /**
    * Sets the OTP.
    *
    * @param otp The OTP to set
    */
    public void setOtp(String otp) {
        this.otp = otp;
    }

    /**
    * Gets the Aadhaar number (UID).
    *
    * @return The Aadhaar number
    */
    public String getUid() {
        return uid;
    }

    /**
    * Sets the Aadhaar number (UID).
    *
    * @param uid The Aadhaar number to set
    */
    public void setUid(String uid) {
        this.uid = uid;
    }

    /**
    * Returns a string representation of the OTP verification request.
    * The UID and OTP are masked for security.
    *
    * @return A string representation of the object
    */
    @Override
    public String toString() {
        return "OtpVerifyRequestDto{" +
        "txnId='" + txnId + '\'' +
        ", otp='******'" +
        ", uid='XXXX-XXXX-" + (uid != null && uid.length() >= 4 ? uid.substring(uid.length() - 4) : "null") + '\'' +
        '}';
    }

    /**
    * Compares this OTP verification request with another object for equality.
    *
    * @param o The object to compare with
    * @return true if the objects are equal, false otherwise
    */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OtpVerifyRequestDto that = (OtpVerifyRequestDto) o;
        return Objects.equals(txnId, that.txnId) &&
        Objects.equals(otp, that.otp) &&
        Objects.equals(uid, that.uid);
    }

    /**
    * Generates a hash code for this OTP verification request.
    *
    * @return The hash code
    */
    @Override
    public int hashCode() {
        return Objects.hash(txnId, otp, uid);
    }
}