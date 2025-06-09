package com.ekyc.service.service.impl;

import com.ekyc.service.dto.EkycResponseDto;
import com.ekyc.service.dto.OtpVerificationDto;
import com.ekyc.service.dto.UidaiOtpVerifyRequestDto;
import com.ekyc.service.dto.UidaiOtpVerifyResponseDto;
import com.ekyc.service.entity.EkycRequest;
import com.ekyc.service.entity.OtpVerification;
import com.ekyc.service.enums.VerificationStatus;
import com.ekyc.service.exception.EkycException;
import com.ekyc.service.exception.InvalidOtpException;
import com.ekyc.service.exception.MaxAttemptsExceededException;
import com.ekyc.service.exception.ResourceNotFoundException;
import com.ekyc.service.repository.EkycRequestRepository;
import com.ekyc.service.repository.OtpVerificationRepository;
import com.ekyc.service.service.OtpVerificationService;
import com.ekyc.service.service.UidaiApiService;
import com.ekyc.service.util.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
* Implementation of the OTP verification service.
* This service handles OTP verification operations including validation,
* tracking attempts, and integration with UIDAI API.
*/
@Service
public class OtpVerificationServiceImpl implements OtpVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(OtpVerificationServiceImpl.class);
    private final AuditLogger auditLogger = new AuditLogger(logger);

    private final OtpVerificationRepository otpVerificationRepository;
    private final EkycRequestRepository ekycRequestRepository;
    private final UidaiApiService uidaiApiService;

    @Value("${otp.verification.max-attempts:3}")
    private int maxAttempts;

    @Value("${otp.verification.expiry-minutes:10}")
    private int otpExpiryMinutes;

    /**
    * Constructor for OtpVerificationServiceImpl.
    *
    * @param otpVerificationRepository Repository for OTP verification data
    * @param ekycRequestRepository Repository for eKYC request data
    * @param uidaiApiService Service for UIDAI API interactions
    */
    public OtpVerificationServiceImpl(
    OtpVerificationRepository otpVerificationRepository,
    EkycRequestRepository ekycRequestRepository,
    UidaiApiService uidaiApiService) {
        this.otpVerificationRepository = otpVerificationRepository;
        this.ekycRequestRepository = ekycRequestRepository;
        this.uidaiApiService = uidaiApiService;
    }

    /**
    * Verifies an OTP for a given reference ID.
    *
    * @param otpVerificationDto The DTO containing the reference ID and OTP
    * @return An EkycResponseDto with the verification result
    */
    @Override
    @Transactional
    public EkycResponseDto verifyOtp(OtpVerificationDto otpVerificationDto) {
        String referenceId = otpVerificationDto.getReferenceId();
        String otp = otpVerificationDto.getOtp();

        auditLogger.info("Starting OTP verification for reference ID: {}", referenceId);

        // Find the eKYC request
        EkycRequest ekycRequest = ekycRequestRepository.findByReferenceId(referenceId)
        .orElseThrow(() -> {
            auditLogger.error("eKYC request not found for reference ID: {}", referenceId);
            return new ResourceNotFoundException("eKYC request not found for reference ID: " + referenceId);
        });

        // Find the OTP verification record
        OtpVerification otpVerification = otpVerificationRepository.findByReferenceId(referenceId)
        .orElseThrow(() -> {
            auditLogger.error("OTP verification record not found for reference ID: {}", referenceId);
            return new ResourceNotFoundException("OTP verification record not found for reference ID: " + referenceId);
        });

        // Check if OTP is expired
        if (isOtpExpired(otpVerification.getCreatedAt())) {
            auditLogger.warn("OTP expired for reference ID: {}", referenceId);
            otpVerification.setStatus(VerificationStatus.EXPIRED);
            otpVerificationRepository.save(otpVerification);
            return new EkycResponseDto(referenceId, VerificationStatus.EXPIRED, "OTP has expired");
        }

        // Check if max attempts exceeded
        if (otpVerification.getAttempts() >= maxAttempts) {
            auditLogger.warn("Maximum OTP verification attempts exceeded for reference ID: {}", referenceId);
            otpVerification.setStatus(VerificationStatus.FAILED);
            otpVerificationRepository.save(otpVerification);
            throw new MaxAttemptsExceededException("Maximum OTP verification attempts exceeded");
        }

        // Increment attempt count
        otpVerification.setAttempts(otpVerification.getAttempts() + 1);

        try {
            // Call UIDAI API to verify OTP
            UidaiOtpVerifyRequestDto verifyRequestDto = new UidaiOtpVerifyRequestDto();
            verifyRequestDto.setUid(ekycRequest.getAadhaarNumber());
            verifyRequestDto.setOtp(otp);
            verifyRequestDto.setTxnId(otpVerification.getTransactionId());

            CompletableFuture<UidaiOtpVerifyResponseDto> futureResponse = uidaiApiService.verifyOtp(verifyRequestDto);
            UidaiOtpVerifyResponseDto response = futureResponse.get();

            // Process the response
            if (response.isSuccess()) {
                auditLogger.info("OTP verification successful for reference ID: {}", referenceId);
                otpVerification.setStatus(VerificationStatus.VERIFIED);
                otpVerificationRepository.save(otpVerification);

                // Update eKYC request status
                ekycRequest.setVerificationStatus(VerificationStatus.VERIFIED);
                ekycRequestRepository.save(ekycRequest);

                return new EkycResponseDto(referenceId, VerificationStatus.VERIFIED, "OTP verification successful");
            } else {
                auditLogger.warn("OTP verification failed for reference ID: {}", referenceId);

                // Check if this was the last attempt
                if (otpVerification.getAttempts() >= maxAttempts) {
                    otpVerification.setStatus(VerificationStatus.FAILED);
                    otpVerificationRepository.save(otpVerification);

                    // Update eKYC request status
                    ekycRequest.setVerificationStatus(VerificationStatus.FAILED);
                    ekycRequestRepository.save(ekycRequest);

                    throw new MaxAttemptsExceededException("Maximum OTP verification attempts exceeded");
                } else {
                    otpVerificationRepository.save(otpVerification);
                    throw new InvalidOtpException("Invalid OTP. Remaining attempts: " +
                    (maxAttempts - otpVerification.getAttempts()));
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            auditLogger.error("Error during OTP verification for reference ID: {}", referenceId, e);

            // Save the attempt even if there was an error
            otpVerificationRepository.save(otpVerification);

            throw new EkycException("Error during OTP verification: " + e.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
    * Creates a new OTP verification record.
    *
    * @param phoneNumber The phone number for verification
    * @param aadhaarNumber The Aadhaar number for verification
    * @param transactionId The transaction ID from UIDAI
    * @return The reference ID for the verification
    */
    @Override
    @Transactional
    public String createOtpVerification(String phoneNumber, String aadhaarNumber, String transactionId) {
        auditLogger.info("Creating OTP verification for phone: {}, Aadhaar: {}",
        phoneNumber, aadhaarNumber);

        String referenceId = generateReferenceId();

        OtpVerification otpVerification = new OtpVerification();
        otpVerification.setReferenceId(referenceId);
        otpVerification.setPhoneNumber(phoneNumber);
        otpVerification.setAadhaarNumber(aadhaarNumber);
        otpVerification.setTransactionId(transactionId);
        otpVerification.setStatus(VerificationStatus.PENDING);
        otpVerification.setAttempts(0);
        otpVerification.setCreatedAt(LocalDateTime.now());

        otpVerificationRepository.save(otpVerification);

        auditLogger.info("OTP verification created with reference ID: {}", referenceId);

        return referenceId;
    }

    /**
    * Gets the verification status for a reference ID.
    *
    * @param referenceId The reference ID to check
    * @return The verification status
    */
    @Override
    public VerificationStatus getVerificationStatus(String referenceId) {
        auditLogger.info("Getting verification status for reference ID: {}", referenceId);

        Optional<OtpVerification> otpVerificationOpt = otpVerificationRepository.findByReferenceId(referenceId);

        if (otpVerificationOpt.isPresent()) {
            OtpVerification otpVerification = otpVerificationOpt.get();

            // If status is PENDING but OTP is expired, update to EXPIRED
            if (otpVerification.getStatus() == VerificationStatus.PENDING &&
            isOtpExpired(otpVerification.getCreatedAt())) {
                otpVerification.setStatus(VerificationStatus.EXPIRED);
                otpVerificationRepository.save(otpVerification);
                return VerificationStatus.EXPIRED;
            }

            return otpVerification.getStatus();
        } else {
            auditLogger.warn("No verification record found for reference ID: {}", referenceId);
            throw new ResourceNotFoundException("No verification record found for reference ID: " + referenceId);
        }
    }

    /**
    * Gets the number of remaining verification attempts for a reference ID.
    *
    * @param referenceId The reference ID to check
    * @return The number of remaining attempts, or 0 if no attempts remain or the reference ID is invalid
    */
    @Override
    public int getRemainingAttempts(String referenceId) {
        auditLogger.info("Getting remaining attempts for reference ID: {}", referenceId);

        Optional<OtpVerification> otpVerificationOpt = otpVerificationRepository.findByReferenceId(referenceId);

        if (otpVerificationOpt.isPresent()) {
            OtpVerification otpVerification = otpVerificationOpt.get();

            // If OTP is expired, no attempts remain
            if (isOtpExpired(otpVerification.getCreatedAt())) {
                return 0;
            }

            int remainingAttempts = maxAttempts - otpVerification.getAttempts();
            return Math.max(0, remainingAttempts);
        } else {
            auditLogger.warn("No verification record found for reference ID: {}", referenceId);
            return 0;
        }
    }

    /**
    * Checks if an OTP is expired based on its creation time.
    *
    * @param createdAt The time when the OTP was created
    * @return true if the OTP is expired, false otherwise
    */
    private boolean isOtpExpired(LocalDateTime createdAt) {
        LocalDateTime expiryTime = createdAt.plusMinutes(otpExpiryMinutes);
        return LocalDateTime.now().isAfter(expiryTime);
    }

    /**
    * Generates a unique reference ID for OTP verification.
    *
    * @return A unique reference ID
    */
    private String generateReferenceId() {
        return UUID.randomUUID().toString();
    }
}