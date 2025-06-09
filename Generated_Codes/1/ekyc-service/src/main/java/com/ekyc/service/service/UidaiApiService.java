package com.ekyc.service.service;

import com.ekyc.service.dto.UidaiOtpInitiateRequestDto;
import com.ekyc.service.dto.UidaiOtpInitiateResponseDto;
import com.ekyc.service.dto.UidaiOtpVerifyRequestDto;
import com.ekyc.service.dto.UidaiOtpVerifyResponseDto;

import java.util.concurrent.CompletableFuture;

/**
* Service interface for UIDAI (Unique Identification Authority of India) API operations.
* This service handles communication with the UIDAI API for Aadhaar-based verification processes.
*/
public interface UidaiApiService {

    /**
    * Initiates the OTP generation process for Aadhaar verification.
    * This method sends a request to the UIDAI API to generate an OTP for the provided Aadhaar number.
    *
    * @param request The request containing Aadhaar number and other required details
    * @return A CompletableFuture containing the response from UIDAI OTP initiation API
    */
    CompletableFuture<UidaiOtpInitiateResponseDto> initiateOtp(UidaiOtpInitiateRequestDto request);

    /**
    * Verifies the OTP provided by the user against the UIDAI system.
    * This method validates if the OTP entered by the user matches with the OTP generated for the Aadhaar verification.
    *
    * @param request The request containing the OTP, transaction ID, and Aadhaar number
    * @return A CompletableFuture containing the response from UIDAI OTP verification API
    */
    CompletableFuture<UidaiOtpVerifyResponseDto> verifyOtp(UidaiOtpVerifyRequestDto request);

    /**
    * Checks the health/status of the UIDAI API service.
    * This method can be used to verify if the UIDAI API is operational and accessible.
    *
    * @return A CompletableFuture containing a boolean indicating if the service is healthy
    */
    CompletableFuture<Boolean> checkServiceHealth();

    /**
    * Retrieves the current API version and configuration details from the UIDAI service.
    * This information can be useful for debugging and ensuring compatibility.
    *
    * @return A CompletableFuture containing a String with version and configuration information
    */
    CompletableFuture<String> getApiInfo();
}