package com.ekyc.service.service;

import com.ekyc.service.dto.EkycResponseDto;
import com.ekyc.service.dto.OtpVerificationDto;
import com.ekyc.service.enums.VerificationStatus;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
* Service interface for OTP verification operations.
* This service handles the generation, sending, and verification of One-Time Passwords (OTPs)
* as part of the eKYC verification process.
*/
public interface OtpVerificationService {

    /**
    * Generates and sends an OTP to the provided contact (phone/email).
    *
    * @param contactInfo The phone number or email to send the OTP to
    * @param referenceId The reference ID associated with the verification request
    * @return A CompletableFuture containing the status of the OTP generation and sending process
    */
    CompletableFuture<Boolean> generateAndSendOtp(String contactInfo, String referenceId);

    /**
    * Verifies the OTP provided by the user against the stored OTP.
    *
    * @param otpVerificationDto The DTO containing the OTP and reference information
    * @return A CompletableFuture containing the verification status
    */
    CompletableFuture<VerificationStatus> verifyOtp(OtpVerificationDto otpVerificationDto);

    /**
    * Retrieves the verification status for a given reference ID.
    *
    * @param referenceId The reference ID to check
    * @return An Optional containing the eKYC response if found
    */
    Optional<EkycResponseDto> getVerificationStatus(String referenceId);

    /**
    * Invalidates an OTP for a given reference ID.
    * This is typically used when a user requests a new OTP or after maximum retry attempts.
    *
    * @param referenceId The reference ID for which to invalidate the OTP
    * @return true if the OTP was successfully invalidated, false otherwise
    */
    boolean invalidateOtp(String referenceId);

    /**
    * Checks if an OTP is still valid for a given reference ID.
    *
    * @param referenceId The reference ID to check
    * @return true if the OTP is still valid, false if it has expired or does not exist
    */
    boolean isOtpValid(String referenceId);

    /**
    * Gets the remaining attempts for OTP verification for a given reference ID.
    *
    * @param referenceId The reference ID to check
    * @return The number of remaining attempts, or 0 if no attempts remain or the reference ID is invalid
    */
    int getRemainingAttempts(String referenceId);
}