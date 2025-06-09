package com.ekyc.service.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

/**
* Entity class representing OTP verification data.
* This entity stores information related to OTP generation, verification status,
* and expiration for the eKYC process.
*/
@Entity
@Table(name = "otp_verification")
public class OtpVerification {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Type(type = "org.hibernate.type.UUIDCharType")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 15, message = "Phone number must be between 10 and 15 characters")
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @NotBlank(message = "OTP is required")
    @Size(min = 4, max = 8, message = "OTP must be between 4 and 8 characters")
    @Column(name = "otp", nullable = false)
    private String otp;

    @NotNull(message = "OTP generation time is required")
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @NotNull(message = "OTP expiration time is required")
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @NotNull(message = "Verification status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VerificationStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "reference_id")
    @Type(type = "org.hibernate.type.UUIDCharType")
    private UUID referenceId;

    /**
    * Enum representing the possible states of OTP verification.
    */
    public enum VerificationStatus {
        PENDING,
        VERIFIED,
        EXPIRED,
        FAILED
    }

    /**
    * Default constructor for JPA.
    */
    public OtpVerification() {
    }

    /**
    * Constructor with essential fields for OTP verification.
    *
    * @param phoneNumber The phone number to which OTP is sent
    * @param otp The one-time password
    * @param generatedAt The time when OTP was generated
    * @param expiresAt The time when OTP will expire
    * @param status The initial verification status
    */
    public OtpVerification(String phoneNumber, String otp, LocalDateTime generatedAt,
    LocalDateTime expiresAt, VerificationStatus status) {
        this.phoneNumber = phoneNumber;
        this.otp = otp;
        this.generatedAt = generatedAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.retryCount = 0;
    }

    /**
    * Checks if the OTP has expired based on current time.
    *
    * @return true if OTP has expired, false otherwise
    */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
    * Masks the phone number for logging and display purposes.
    *
    * @return Masked phone number with middle digits replaced by asterisks
    */
    public String getMaskedPhoneNumber() {
        if (phoneNumber == null || phoneNumber.length() < 6) {
            return phoneNumber;
        }

        int length = phoneNumber.length();
        return phoneNumber.substring(0, 3) + "****" +
        phoneNumber.substring(length - 3);
    }

    /**
    * Masks the OTP for logging and display purposes.
    *
    * @return Masked OTP with all but last two digits replaced by asterisks
    */
    public String getMaskedOtp() {
        if (otp == null || otp.length() < 3) {
            return "****";
        }

        int length = otp.length();
        return "****" + otp.substring(length - 2);
    }

    /**
    * Increments the retry count for OTP verification attempts.
    */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OtpVerification that = (OtpVerification) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "OtpVerification{" +
        "id=" + id +
        ", phoneNumber='" + getMaskedPhoneNumber() + '\'' +
        ", otp='" + getMaskedOtp() + '\'' +
        ", generatedAt=" + generatedAt +
        ", verifiedAt=" + verifiedAt +
        ", expiresAt=" + expiresAt +
        ", status=" + status +
        ", retryCount=" + retryCount +
        ", referenceId=" + referenceId +
        '}';
    }
}