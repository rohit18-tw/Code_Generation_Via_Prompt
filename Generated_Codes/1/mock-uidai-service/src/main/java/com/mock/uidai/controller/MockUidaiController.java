package com.mock.uidai.controller;

import com.mock.uidai.dto.KycDataDto;
import com.mock.uidai.dto.OtpInitiateRequestDto;
import com.mock.uidai.dto.OtpInitiateResponseDto;
import com.mock.uidai.dto.OtpVerifyRequestDto;
import com.mock.uidai.dto.OtpVerifyResponseDto;
import com.mock.uidai.service.MockUidaiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.Objects;

/**
* Controller for handling mock UIDAI operations.
* This controller simulates the behavior of the actual UIDAI API for testing purposes.
*/
@RestController
@RequestMapping("/api/v1/mock-uidai")
@Validated
public class MockUidaiController {

    private static final Logger logger = LoggerFactory.getLogger(MockUidaiController.class);
    private final MockUidaiService mockUidaiService;

    /**
    * Constructor for dependency injection.
    *
    * @param mockUidaiService The service that handles mock UIDAI operations
    */
    public MockUidaiController(MockUidaiService mockUidaiService) {
        this.mockUidaiService = mockUidaiService;
    }

    /**
    * Endpoint to initiate OTP generation for Aadhaar verification.
    *
    * @param request The OTP initiation request containing Aadhaar number and channel
    * @return ResponseEntity containing OTP initiation response
    */
    @PostMapping(value = "/otp/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OtpInitiateResponseDto> initiateOtp(@Valid @RequestBody OtpInitiateRequestDto request) {
        logger.info("Received OTP initiation request for Aadhaar number ending with: {}",
        maskAadhaarNumber(request.getUid()));

        try {
            if (Objects.isNull(request.getUid()) || request.getUid().trim().isEmpty()) {
                logger.error("Invalid request: Aadhaar number is missing");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aadhaar number is required");
            }

            OtpInitiateResponseDto response = mockUidaiService.initiateOtp(request);
            logger.info("OTP initiation successful for transaction: {}", response.getMaskedTxnId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during OTP initiation: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to initiate OTP", e);
        }
    }

    /**
    * Endpoint to verify OTP for Aadhaar verification.
    *
    * @param request The OTP verification request containing transaction ID, OTP, and Aadhaar number
    * @return ResponseEntity containing OTP verification response
    */
    @PostMapping(value = "/otp/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OtpVerifyResponseDto> verifyOtp(@Valid @RequestBody OtpVerifyRequestDto request) {
        logger.info("Received OTP verification request for transaction: {}", request.getTxnId());

        try {
            if (Objects.isNull(request.getOtp()) || request.getOtp().trim().isEmpty()) {
                logger.error("Invalid request: OTP is missing");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP is required");
            }

            OtpVerifyResponseDto response = mockUidaiService.verifyOtp(request);
            logger.info("OTP verification completed with status: {}", response.getStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during OTP verification: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to verify OTP", e);
        }
    }

    /**
    * Endpoint to retrieve KYC data for a verified Aadhaar.
    *
    * @param request The OTP verification request that was previously validated
    * @return ResponseEntity containing KYC data
    */
    @PostMapping(value = "/ekyc", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<KycDataDto> getKycData(@Valid @RequestBody OtpVerifyRequestDto request) {
        logger.info("Received KYC data request for transaction: {}", request.getTxnId());

        try {
            // First verify the OTP to ensure the request is valid
            OtpVerifyResponseDto verificationResponse = mockUidaiService.verifyOtp(request);

            if (!"SUCCESS".equals(verificationResponse.getStatus())) {
                logger.error("KYC data request failed: OTP verification unsuccessful");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP verification failed");
            }

            KycDataDto kycData = mockUidaiService.getKycData(request.getUid());
            logger.info("KYC data retrieved successfully for Aadhaar ending with: {}",
            maskAadhaarNumber(request.getUid()));
            return ResponseEntity.ok(kycData);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error retrieving KYC data: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve KYC data", e);
        }
    }

    /**
    * Utility method to mask Aadhaar number for logging purposes.
    *
    * @param aadhaarNumber The full Aadhaar number
    * @return Masked Aadhaar number showing only the last 4 digits
    */
    private String maskAadhaarNumber(String aadhaarNumber) {
        if (aadhaarNumber == null || aadhaarNumber.length() < 4) {
            return "****";
        }
        return "XXXX-XXXX-" + aadhaarNumber.substring(aadhaarNumber.length() - 4);
    }
}