package com.ekyc.service.controller;

import com.ekyc.service.dto.EkycRequestDto;
import com.ekyc.service.dto.EkycResponseDto;
import com.ekyc.service.service.EkycService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
* REST controller for eKYC operations.
* Provides endpoints for submitting, retrieving, and managing eKYC verifications.
*/
@RestController
@RequestMapping("/api/v1/ekyc")
@Validated
public class EkycController {

    private static final Logger logger = LoggerFactory.getLogger(EkycController.class);
    private final EkycService ekycService;

    /**
    * Constructor for dependency injection.
    *
    * @param ekycService the eKYC service
    */
    public EkycController(EkycService ekycService) {
        this.ekycService = ekycService;
    }

    /**
    * Submit a new eKYC verification request.
    *
    * @param requestDto the eKYC request data
    * @return the eKYC response with verification details
    */
    @PostMapping("/verify")
    public ResponseEntity<EkycResponseDto> submitVerification(@Valid @RequestBody EkycRequestDto requestDto) {
        logger.info("Received eKYC verification request for ID type: {}", requestDto.getIdType());
        try {
            EkycResponseDto responseDto = ekycService.submitVerification(requestDto);
            logger.info("Successfully processed eKYC verification with ID: {}", responseDto.getVerificationId());
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid eKYC request: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error processing eKYC verification request", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing verification request", e);
        }
    }

    /**
    * Get an eKYC verification by ID.
    *
    * @param verificationId the verification ID
    * @return the eKYC verification details
    */
    @GetMapping("/{verificationId}")
    public ResponseEntity<EkycResponseDto> getVerification(
    @PathVariable @NotBlank(message = "Verification ID is required") String verificationId) {
        logger.info("Retrieving eKYC verification with ID: {}", verificationId);
        try {
            EkycResponseDto responseDto = ekycService.getVerification(verificationId);
            logger.info("Successfully retrieved eKYC verification with ID: {}", verificationId);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid verification ID: {}", verificationId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error retrieving eKYC verification with ID: {}", verificationId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving verification", e);
        }
    }

    /**
    * Get all eKYC verifications.
    *
    * @return list of all eKYC verifications
    */
    @GetMapping
    public ResponseEntity<List<EkycResponseDto>> getAllVerifications() {
        logger.info("Retrieving all eKYC verifications");
        try {
            List<EkycResponseDto> verifications = ekycService.getAllVerifications();
            logger.info("Successfully retrieved {} eKYC verifications", verifications.size());
            return ResponseEntity.ok(verifications);
        } catch (Exception e) {
            logger.error("Error retrieving all eKYC verifications", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving verifications", e);
        }
    }

    /**
    * Cancel an eKYC verification.
    *
    * @param verificationId the verification ID
    * @return the updated eKYC verification details
    */
    @DeleteMapping("/{verificationId}")
    public ResponseEntity<EkycResponseDto> cancelVerification(
    @PathVariable @NotBlank(message = "Verification ID is required") String verificationId) {
        logger.info("Cancelling eKYC verification with ID: {}", verificationId);
        try {
            EkycResponseDto responseDto = ekycService.cancelVerification(verificationId);
            logger.info("Successfully cancelled eKYC verification with ID: {}", verificationId);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid verification ID for cancellation: {}", verificationId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalStateException e) {
            logger.error("Cannot cancel verification with ID: {}: {}", verificationId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error cancelling eKYC verification with ID: {}", verificationId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error cancelling verification", e);
        }
    }

    /**
    * Resubmit an eKYC verification with updated information.
    *
    * @param verificationId the verification ID
    * @param requestDto the updated eKYC request data
    * @return the updated eKYC verification details
    */
    @PutMapping("/{verificationId}")
    public ResponseEntity<EkycResponseDto> resubmitVerification(
    @PathVariable @NotBlank(message = "Verification ID is required") String verificationId,
    @Valid @RequestBody EkycRequestDto requestDto) {
        logger.info("Resubmitting eKYC verification with ID: {}", verificationId);
        try {
            EkycResponseDto responseDto = ekycService.resubmitVerification(verificationId, requestDto);
            logger.info("Successfully resubmitted eKYC verification with ID: {}", verificationId);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid verification ID for resubmission: {}", verificationId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalStateException e) {
            logger.error("Cannot resubmit verification with ID: {}: {}", verificationId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error resubmitting eKYC verification with ID: {}", verificationId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error resubmitting verification", e);
        }
    }
}