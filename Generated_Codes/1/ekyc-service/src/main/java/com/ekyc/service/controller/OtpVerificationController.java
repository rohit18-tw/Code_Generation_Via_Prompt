package com.ekyc.service.controller;

import com.ekyc.service.dto.EkycResponseDto;
import com.ekyc.service.dto.OtpVerificationDto;
import com.ekyc.service.service.OtpVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

/**
* REST controller for OTP verification operations.
* Handles OTP verification requests and provides endpoints for checking remaining attempts.
*/
@RestController
@RequestMapping("/api/v1/otp")
@Validated
public class OtpVerificationController {

    private static final Logger logger = LoggerFactory.getLogger(OtpVerificationController.class);

    private final OtpVerificationService otpVerificationService;

    /**
    * Constructor for dependency injection.
    *
    * @param otpVerificationService The OTP verification service
    */
    @Autowired
    public OtpVerificationController(OtpVerificationService otpVerificationService) {
        this.otpVerificationService = otpVerificationService;
    }

    /**
    * Endpoint to verify an OTP for a given reference ID.
    *
    * @param otpVerificationDto The DTO containing the reference ID and OTP
    * @return ResponseEntity with verification result
    */
    @PostMapping("/verify")
    public ResponseEntity<EkycResponseDto> verifyOtp(@Valid @RequestBody OtpVerificationDto otpVerificationDto) {
        logger.info("Received OTP verification request for referenceId: {}", otpVerificationDto.getReferenceId());

        try {
            EkycResponseDto response = otpVerificationService.verifyOtp(otpVerificationDto);
            logger.info("OTP verification completed for referenceId: {} with status: {}",
            otpVerificationDto.getReferenceId(), response.getVerificationStatus());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid OTP verification request for referenceId: {}: {}",
            otpVerificationDto.getReferenceId(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error during OTP verification for referenceId: {}",
            otpVerificationDto.getReferenceId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
    * Endpoint to check the remaining verification attempts for a reference ID.
    *
    * @param referenceId The reference ID to check
    * @return ResponseEntity with the number of remaining attempts
    */
    @GetMapping("/attempts/{referenceId}")
    public ResponseEntity<Map<String, Integer>> getRemainingAttempts(
    @PathVariable @NotBlank(message = "Reference ID cannot be blank") String referenceId) {

        logger.info("Checking remaining attempts for referenceId: {}", referenceId);

        try {
            int remainingAttempts = otpVerificationService.getRemainingAttempts(referenceId);

            Map<String, Integer> response = new HashMap<>();
            response.put("remainingAttempts", remainingAttempts);

            logger.info("Remaining attempts for referenceId {}: {}", referenceId, remainingAttempts);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid reference ID provided: {}: {}", referenceId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error retrieving remaining attempts for referenceId: {}", referenceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
    * Endpoint to resend OTP for a given reference ID.
    *
    * @param referenceId The reference ID for which to resend the OTP
    * @return ResponseEntity with the result of the resend operation
    */
    @PostMapping("/resend/{referenceId}")
    public ResponseEntity<Map<String, String>> resendOtp(
    @PathVariable @NotBlank(message = "Reference ID cannot be blank") String referenceId) {

        logger.info("Received request to resend OTP for referenceId: {}", referenceId);

        try {
            boolean resendSuccessful = otpVerificationService.resendOtp(referenceId);

            Map<String, String> response = new HashMap<>();
            if (resendSuccessful) {
                response.put("status", "success");
                response.put("message", "OTP resent successfully");
                logger.info("OTP resent successfully for referenceId: {}", referenceId);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "failed");
                response.put("message", "Failed to resend OTP");
                logger.warn("Failed to resend OTP for referenceId: {}", referenceId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid reference ID for OTP resend: {}: {}", referenceId, e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error during OTP resend for referenceId: {}", referenceId, e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}