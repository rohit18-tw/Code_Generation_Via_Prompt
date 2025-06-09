package com.ekyc.service.dto;

import com.ekyc.service.enums.IdType;
import com.ekyc.service.enums.VerificationStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
* Data Transfer Object for eKYC response data.
* This class encapsulates the response data returned from the eKYC verification process.
*/
public class EkycResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String requestId;
    private String userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private IdType idType;
    private String idNumber;
    private VerificationStatus status;
    private String message;
    private LocalDateTime verificationTime;
    private LocalDateTime expiryTime;
    private String transactionReference;
    private boolean consentProvided;
    private String rejectionReason;
    private Integer confidenceScore;

    /**
    * Default constructor
    */
    public EkycResponseDto() {
    }

    /**
    * Parameterized constructor with essential fields
    *
    * @param requestId             The unique identifier for the eKYC request
    * @param userId                The user identifier
    * @param status                The verification status
    * @param message               The response message
    * @param verificationTime      The time when verification was performed
    * @param transactionReference  The transaction reference number
    */
    public EkycResponseDto(String requestId, String userId, VerificationStatus status,
    String message, LocalDateTime verificationTime,
    String transactionReference) {
        this.requestId = requestId;
        this.userId = userId;
        this.status = status;
        this.message = message;
        this.verificationTime = verificationTime;
        this.transactionReference = transactionReference;
    }

    /**
    * Gets the request ID
    *
    * @return the request ID
    */
    public String getRequestId() {
        return requestId;
    }

    /**
    * Sets the request ID
    *
    * @param requestId the request ID to set
    */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
    * Gets the user ID
    *
    * @return the user ID
    */
    public String getUserId() {
        return userId;
    }

    /**
    * Sets the user ID
    *
    * @param userId the user ID to set
    */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
    * Gets the full name
    *
    * @return the full name
    */
    public String getFullName() {
        return fullName;
    }

    /**
    * Sets the full name
    *
    * @param fullName the full name to set
    */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
    * Gets the email
    *
    * @return the email
    */
    public String getEmail() {
        return email;
    }

    /**
    * Sets the email
    *
    * @param email the email to set
    */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
    * Gets the phone number
    *
    * @return the phone number
    */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
    * Sets the phone number
    *
    * @param phoneNumber the phone number to set
    */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
    * Gets the ID type
    *
    * @return the ID type
    */
    public IdType getIdType() {
        return idType;
    }

    /**
    * Sets the ID type
    *
    * @param idType the ID type to set
    */
    public void setIdType(IdType idType) {
        this.idType = idType;
    }

    /**
    * Gets the ID number
    *
    * @return the ID number
    */
    public String getIdNumber() {
        return idNumber;
    }

    /**
    * Sets the ID number
    *
    * @param idNumber the ID number to set
    */
    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    /**
    * Gets the verification status
    *
    * @return the verification status
    */
    public VerificationStatus getStatus() {
        return status;
    }

    /**
    * Sets the verification status
    *
    * @param status the verification status to set
    */
    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    /**
    * Gets the response message
    *
    * @return the response message
    */
    public String getMessage() {
        return message;
    }

    /**
    * Sets the response message
    *
    * @param message the response message to set
    */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
    * Gets the verification time
    *
    * @return the verification time
    */
    public LocalDateTime getVerificationTime() {
        return verificationTime;
    }

    /**
    * Sets the verification time
    *
    * @param verificationTime the verification time to set
    */
    public void setVerificationTime(LocalDateTime verificationTime) {
        this.verificationTime = verificationTime;
    }

    /**
    * Gets the expiry time
    *
    * @return the expiry time
    */
    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    /**
    * Sets the expiry time
    *
    * @param expiryTime the expiry time to set
    */
    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    /**
    * Gets the transaction reference
    *
    * @return the transaction reference
    */
    public String getTransactionReference() {
        return transactionReference;
    }

    /**
    * Sets the transaction reference
    *
    * @param transactionReference the transaction reference to set
    */
    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    /**
    * Checks if consent was provided
    *
    * @return true if consent was provided, false otherwise
    */
    public boolean isConsentProvided() {
        return consentProvided;
    }

    /**
    * Sets whether consent was provided
    *
    * @param consentProvided true if consent was provided, false otherwise
    */
    public void setConsentProvided(boolean consentProvided) {
        this.consentProvided = consentProvided;
    }

    /**
    * Gets the rejection reason
    *
    * @return the rejection reason
    */
    public String getRejectionReason() {
        return rejectionReason;
    }

    /**
    * Sets the rejection reason
    *
    * @param rejectionReason the rejection reason to set
    */
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    /**
    * Gets the confidence score
    *
    * @return the confidence score
    */
    public Integer getConfidenceScore() {
        return confidenceScore;
    }

    /**
    * Sets the confidence score
    *
    * @param confidenceScore the confidence score to set
    */
    public void setConfidenceScore(Integer confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EkycResponseDto that = (EkycResponseDto) o;
        return consentProvided == that.consentProvided &&
        Objects.equals(requestId, that.requestId) &&
        Objects.equals(userId, that.userId) &&
        Objects.equals(fullName, that.fullName) &&
        Objects.equals(email, that.email) &&
        Objects.equals(phoneNumber, that.phoneNumber) &&
        idType == that.idType &&
        Objects.equals(idNumber, that.idNumber) &&
        status == that.status &&
        Objects.equals(message, that.message) &&
        Objects.equals(verificationTime, that.verificationTime) &&
        Objects.equals(expiryTime, that.expiryTime) &&
        Objects.equals(transactionReference, that.transactionReference) &&
        Objects.equals(rejectionReason, that.rejectionReason) &&
        Objects.equals(confidenceScore, that.confidenceScore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, userId, fullName, email, phoneNumber, idType, idNumber,
        status, message, verificationTime, expiryTime, transactionReference,
        consentProvided, rejectionReason, confidenceScore);
    }

    @Override
    public String toString() {
        return "EkycResponseDto{" +
        "requestId='" + requestId + '\'' +
        ", userId='" + userId + '\'' +
        ", fullName='" + (fullName != null ? "***" : null) + '\'' +
        ", email='" + (email != null ? "***" : null) + '\'' +
        ", phoneNumber='" + (phoneNumber != null ? "***" : null) + '\'' +
        ", idType=" + idType +
        ", idNumber='" + (idNumber != null ? "***" : null) + '\'' +
        ", status=" + status +
        ", message='" + message + '\'' +
        ", verificationTime=" + verificationTime +
        ", expiryTime=" + expiryTime +
        ", transactionReference='" + transactionReference + '\'' +
        ", consentProvided=" + consentProvided +
        ", rejectionReason='" + rejectionReason + '\'' +
        ", confidenceScore=" + confidenceScore +
        '}';
    }
}