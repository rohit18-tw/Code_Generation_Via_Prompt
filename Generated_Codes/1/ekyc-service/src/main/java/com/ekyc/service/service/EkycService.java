package com.ekyc.service.service;

import com.ekyc.service.dto.EkycRequestDto;
import com.ekyc.service.dto.EkycResponseDto;
import com.ekyc.service.enums.VerificationStatus;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
* Service interface for eKYC (Electronic Know Your Customer) operations.
* This interface defines the contract for all eKYC verification and management operations.
*/
public interface EkycService {

    /**
    * Submits a new eKYC verification request.
    *
    * @param requestDto The eKYC request data transfer object containing all necessary information for verification
    * @return The response containing the verification result and details
    * @throws IllegalArgumentException if the request data is invalid
    */
    EkycResponseDto submitVerification(EkycRequestDto requestDto);

    /**
    * Submits a new eKYC verification request asynchronously.
    *
    * @param requestDto The eKYC request data transfer object
    * @return A CompletableFuture that will complete with the verification result
    * @throws IllegalArgumentException if the request data is invalid
    */
    CompletableFuture<EkycResponseDto> submitVerificationAsync(EkycRequestDto requestDto);

    /**
    * Retrieves the verification result by its unique identifier.
    *
    * @param verificationId The unique identifier of the verification
    * @return An Optional containing the verification result if found, or empty if not found
    */
    Optional<EkycResponseDto> getVerificationById(String verificationId);

    /**
    * Retrieves all verification results for a specific customer.
    *
    * @param customerId The unique identifier of the customer
    * @return A list of verification results for the customer
    */
    List<EkycResponseDto> getVerificationsByCustomerId(String customerId);

    /**
    * Updates the status of an existing verification.
    *
    * @param verificationId The unique identifier of the verification
    * @param status The new verification status
    * @param reason Optional reason for the status change, especially for rejections
    * @return The updated verification result
    * @throws IllegalArgumentException if the verification ID is invalid
    * @throws IllegalStateException if the verification cannot be updated due to its current state
    */
    EkycResponseDto updateVerificationStatus(String verificationId, VerificationStatus status, String reason);

    /**
    * Cancels an in-progress verification.
    *
    * @param verificationId The unique identifier of the verification
    * @return true if the verification was successfully canceled, false otherwise
    * @throws IllegalArgumentException if the verification ID is invalid
    * @throws IllegalStateException if the verification cannot be canceled due to its current state
    */
    boolean cancelVerification(String verificationId);

    /**
    * Retrieves verification statistics for a given time period.
    *
    * @param startDate The start date of the period (ISO format: yyyy-MM-dd)
    * @param endDate The end date of the period (ISO format: yyyy-MM-dd)
    * @return A summary of verification statistics for the period
    */
    Object getVerificationStatistics(String startDate, String endDate);

    /**
    * Resubmits a previously failed or rejected verification with updated information.
    *
    * @param verificationId The unique identifier of the original verification
    * @param updatedRequestDto The updated eKYC request data
    * @return The response containing the new verification result
    * @throws IllegalArgumentException if the verification ID is invalid
    * @throws IllegalStateException if the verification cannot be resubmitted
    */
    EkycResponseDto resubmitVerification(String verificationId, EkycRequestDto updatedRequestDto);
}