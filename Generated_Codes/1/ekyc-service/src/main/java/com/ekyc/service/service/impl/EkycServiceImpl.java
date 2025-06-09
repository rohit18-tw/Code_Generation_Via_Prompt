package com.ekyc.service.service.impl;

import com.ekyc.service.dto.EkycRequestDto;
import com.ekyc.service.dto.EkycResponseDto;
import com.ekyc.service.dto.UidaiOtpInitiateRequestDto;
import com.ekyc.service.dto.UidaiOtpInitiateResponseDto;
import com.ekyc.service.dto.UidaiOtpVerifyRequestDto;
import com.ekyc.service.dto.UidaiOtpVerifyResponseDto;
import com.ekyc.service.entity.EkycRequest;
import com.ekyc.service.enums.VerificationStatus;
import com.ekyc.service.exception.EkycServiceException;
import com.ekyc.service.exception.ResourceNotFoundException;
import com.ekyc.service.repository.EkycRequestRepository;
import com.ekyc.service.service.EkycService;
import com.ekyc.service.service.UidaiApiService;
import com.ekyc.service.util.AuditLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
* Implementation of the EkycService interface that provides eKYC verification functionality.
* This service handles the business logic for initiating, verifying, and managing eKYC requests.
*/
@Service
public class EkycServiceImpl implements EkycService {

    private static final Logger logger = LoggerFactory.getLogger(EkycServiceImpl.class);
    private final AuditLogger auditLogger = new AuditLogger(logger);

    private final EkycRequestRepository ekycRequestRepository;
    private final UidaiApiService uidaiApiService;

    @Value("${ekyc.verification.expiry.days:30}")
    private int verificationExpiryDays;

    @Value("${ekyc.verification.max-attempts:3}")
    private int maxVerificationAttempts;

    /**
    * Constructor for dependency injection.
    *
    * @param ekycRequestRepository Repository for eKYC request data
    * @param uidaiApiService Service for UIDAI API interactions
    */
    @Autowired
    public EkycServiceImpl(EkycRequestRepository ekycRequestRepository, UidaiApiService uidaiApiService) {
        this.ekycRequestRepository = ekycRequestRepository;
        this.uidaiApiService = uidaiApiService;
    }

    /**
    * Initiates a new eKYC verification process by generating an OTP.
    *
    * @param requestDto The eKYC request data
    * @return EkycResponseDto containing the verification ID and status
    */
    @Override
    @Transactional
    public EkycResponseDto initiateVerification(EkycRequestDto requestDto) {
        auditLogger.info("Initiating eKYC verification for user", "aadhaar", requestDto.getAadhaarNumber());

        validateRequestData(requestDto);

        // Create and save the initial eKYC request
        EkycRequest ekycRequest = new EkycRequest();
        ekycRequest.setVerificationId(generateVerificationId());
        ekycRequest.setAadhaarNumber(requestDto.getAadhaarNumber());
        ekycRequest.setName(requestDto.getName());
        ekycRequest.setDateOfBirth(requestDto.getDateOfBirth());
        ekycRequest.setGender(requestDto.getGender());
        ekycRequest.setMobileNumber(requestDto.getMobileNumber());
        ekycRequest.setEmail(requestDto.getEmail());
        ekycRequest.setAddress(requestDto.getAddress());
        ekycRequest.setStatus(VerificationStatus.INITIATED);
        ekycRequest.setCreatedAt(LocalDateTime.now());
        ekycRequest.setAttempts(0);

        ekycRequestRepository.save(ekycRequest);

        // Initiate OTP with UIDAI
        try {
            UidaiOtpInitiateRequestDto otpRequest = new UidaiOtpInitiateRequestDto();
            otpRequest.setAadhaarNumber(requestDto.getAadhaarNumber());
            otpRequest.setMobileNumber(requestDto.getMobileNumber());

            CompletableFuture<UidaiOtpInitiateResponseDto> otpResponseFuture = uidaiApiService.initiateOtp(otpRequest);
            UidaiOtpInitiateResponseDto otpResponse = otpResponseFuture.get();

            if (!otpResponse.isSuccess()) {
                ekycRequest.setStatus(VerificationStatus.FAILED);
                ekycRequest.setFailureReason(otpResponse.getErrorMessage());
                ekycRequestRepository.save(ekycRequest);

                auditLogger.error("OTP initiation failed for verification ID: {}", ekycRequest.getVerificationId());
                throw new EkycServiceException("Failed to initiate OTP: " + otpResponse.getErrorMessage());
            }

            ekycRequest.setTransactionId(otpResponse.getTransactionId());
            ekycRequestRepository.save(ekycRequest);

            auditLogger.info("OTP initiated successfully for verification ID: {}", ekycRequest.getVerificationId());

            return createResponseDto(ekycRequest);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            ekycRequest.setStatus(VerificationStatus.FAILED);
            ekycRequest.setFailureReason("OTP initiation service error");
            ekycRequestRepository.save(ekycRequest);

            auditLogger.error("Error during OTP initiation for verification ID: {}", ekycRequest.getVerificationId(), e);
            throw new EkycServiceException("Error during OTP initiation: " + e.getMessage(), e);
        }
    }

    /**
    * Verifies an eKYC request using the provided OTP.
    *
    * @param verificationId The unique verification ID
    * @param otp The OTP received by the user
    * @return EkycResponseDto with the updated verification status
    */
    @Override
    @Transactional
    public EkycResponseDto verifyOtp(String verificationId, String otp) {
        auditLogger.info("Verifying OTP for verification ID: {}", verificationId);

        if (otp == null || otp.trim().isEmpty()) {
            throw new IllegalArgumentException("OTP cannot be null or empty");
        }

        EkycRequest ekycRequest = findAndValidateVerificationRequest(verificationId);

        // Check if the verification is in a valid state for OTP verification
        if (ekycRequest.getStatus() != VerificationStatus.INITIATED &&
        ekycRequest.getStatus() != VerificationStatus.OTP_RESENT) {
            auditLogger.warn("Invalid verification status for OTP verification: {}", ekycRequest.getStatus());
            throw new IllegalStateException("Verification is not in a valid state for OTP verification");
        }

        // Check if max attempts reached
        if (ekycRequest.getAttempts() >= maxVerificationAttempts) {
            ekycRequest.setStatus(VerificationStatus.MAX_ATTEMPTS_EXCEEDED);
            ekycRequestRepository.save(ekycRequest);

            auditLogger.warn("Max verification attempts exceeded for verification ID: {}", verificationId);
            throw new IllegalStateException("Maximum verification attempts exceeded");
        }

        // Increment attempt counter
        ekycRequest.setAttempts(ekycRequest.getAttempts() + 1);

        try {
            UidaiOtpVerifyRequestDto verifyRequest = new UidaiOtpVerifyRequestDto();
            verifyRequest.setTransactionId(ekycRequest.getTransactionId());
            verifyRequest.setOtp(otp);
            verifyRequest.setAadhaarNumber(ekycRequest.getAadhaarNumber());

            CompletableFuture<UidaiOtpVerifyResponseDto> verifyResponseFuture = uidaiApiService.verifyOtp(verifyRequest);
            UidaiOtpVerifyResponseDto verifyResponse = verifyResponseFuture.get();

            if (!verifyResponse.isSuccess()) {
                ekycRequest.setStatus(VerificationStatus.OTP_VERIFICATION_FAILED);
                ekycRequest.setFailureReason(verifyResponse.getErrorMessage());
                ekycRequestRepository.save(ekycRequest);

                auditLogger.warn("OTP verification failed for verification ID: {}", verificationId);
                throw new EkycServiceException("OTP verification failed: " + verifyResponse.getErrorMessage());
            }

            // Update verification status based on UIDAI response
            if (verifyResponse.isKycDataMatched()) {
                ekycRequest.setStatus(VerificationStatus.VERIFIED);
                ekycRequest.setVerifiedAt(LocalDateTime.now());
            } else {
                ekycRequest.setStatus(VerificationStatus.KYC_DATA_MISMATCH);
                ekycRequest.setFailureReason("KYC data mismatch with UIDAI records");
            }

            ekycRequestRepository.save(ekycRequest);

            auditLogger.info("OTP verification completed for verification ID: {} with status: {}",
            verificationId, ekycRequest.getStatus());

            return createResponseDto(ekycRequest);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            ekycRequest.setStatus(VerificationStatus.OTP_VERIFICATION_FAILED);
            ekycRequest.setFailureReason("OTP verification service error");
            ekycRequestRepository.save(ekycRequest);

            auditLogger.error("Error during OTP verification for verification ID: {}", verificationId, e);
            throw new EkycServiceException("Error during OTP verification: " + e.getMessage(), e);
        }
    }

    /**
    * Resends the OTP for an existing verification request.
    *
    * @param verificationId The unique verification ID
    * @return EkycResponseDto with the updated verification status
    */
    @Override
    @Transactional
    public EkycResponseDto resendOtp(String verificationId) {
        auditLogger.info("Resending OTP for verification ID: {}", verificationId);

        EkycRequest ekycRequest = findAndValidateVerificationRequest(verificationId);

        // Check if the verification is in a valid state for OTP resend
        if (ekycRequest.getStatus() != VerificationStatus.INITIATED &&
        ekycRequest.getStatus() != VerificationStatus.OTP_VERIFICATION_FAILED &&
        ekycRequest.getStatus() != VerificationStatus.OTP_RESENT) {
            auditLogger.warn("Invalid verification status for OTP resend: {}", ekycRequest.getStatus());
            throw new IllegalStateException("Verification is not in a valid state for OTP resend");
        }

        try {
            UidaiOtpInitiateRequestDto otpRequest = new UidaiOtpInitiateRequestDto();
            otpRequest.setAadhaarNumber(ekycRequest.getAadhaarNumber());
            otpRequest.setMobileNumber(ekycRequest.getMobileNumber());

            CompletableFuture<UidaiOtpInitiateResponseDto> otpResponseFuture = uidaiApiService.initiateOtp(otpRequest);
            UidaiOtpInitiateResponseDto otpResponse = otpResponseFuture.get();

            if (!otpResponse.isSuccess()) {
                ekycRequest.setStatus(VerificationStatus.FAILED);
                ekycRequest.setFailureReason(otpResponse.getErrorMessage());
                ekycRequestRepository.save(ekycRequest);

                auditLogger.error("OTP resend failed for verification ID: {}", verificationId);
                throw new EkycServiceException("Failed to resend OTP: " + otpResponse.getErrorMessage());
            }

            ekycRequest.setTransactionId(otpResponse.getTransactionId());
            ekycRequest.setStatus(VerificationStatus.OTP_RESENT);
            ekycRequest.setUpdatedAt(LocalDateTime.now());
            ekycRequestRepository.save(ekycRequest);

            auditLogger.info("OTP resent successfully for verification ID: {}", verificationId);

            return createResponseDto(ekycRequest);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            ekycRequest.setStatus(VerificationStatus.FAILED);
            ekycRequest.setFailureReason("OTP resend service error");
            ekycRequestRepository.save(ekycRequest);

            auditLogger.error("Error during OTP resend for verification ID: {}", verificationId, e);
            throw new EkycServiceException("Error during OTP resend: " + e.getMessage(), e);
        }
    }

    /**
    * Retrieves the status of an eKYC verification request.
    *
    * @param verificationId The unique verification ID
    * @return EkycResponseDto containing the current verification status
    */
    @Override
    @Transactional(readOnly = true)
    public EkycResponseDto getVerificationStatus(String verificationId) {
        auditLogger.info("Getting verification status for ID: {}", verificationId);

        EkycRequest ekycRequest = findAndValidateVerificationRequest(verificationId);

        return createResponseDto(ekycRequest);
    }

    /**
    * Retrieves a paginated list of eKYC verification requests.
    *
    * @param status Optional filter by verification status
    * @param pageable Pagination information
    * @return Page of EkycResponseDto objects
    */
    @Override
    @Transactional(readOnly = true)
    public Page<EkycResponseDto> getAllVerifications(VerificationStatus status, Pageable pageable) {
        auditLogger.info("Getting all verifications with status: {}, page: {}",
        status, pageable.getPageNumber());

        Page<EkycRequest> ekycRequests;

        if (status != null) {
            ekycRequests = ekycRequestRepository.findByStatus(status, pageable);
        } else {
            ekycRequests = ekycRequestRepository.findAll(pageable);
        }

        return ekycRequests.map(this::createResponseDto);
    }

    /**
    * Cancels an ongoing eKYC verification request.
    *
    * @param verificationId The unique verification ID
    * @return EkycResponseDto with the updated verification status
    */
    @Override
    @Transactional
    public EkycResponseDto cancelVerification(String verificationId) {
        auditLogger.info("Cancelling verification with ID: {}", verificationId);

        EkycRequest ekycRequest = findAndValidateVerificationRequest(verificationId);

        // Check if the verification is in a state that can be cancelled
        if (ekycRequest.getStatus() == VerificationStatus.VERIFIED ||
        ekycRequest.getStatus() == VerificationStatus.CANCELLED) {
            auditLogger.warn("Cannot cancel verification with status: {}", ekycRequest.getStatus());
            throw new IllegalStateException("Verification cannot be cancelled in its current state");
        }

        ekycRequest.setStatus(VerificationStatus.CANCELLED);
        ekycRequest.setUpdatedAt(LocalDateTime.now());
        ekycRequestRepository.save(ekycRequest);

        auditLogger.info("Verification cancelled successfully for ID: {}", verificationId);

        return createResponseDto(ekycRequest);
    }

    /**
    * Resubmits an eKYC verification with updated information.
    *
    * @param verificationId The unique verification ID
    * @param updatedRequestDto The updated eKYC request data
    * @return EkycResponseDto with the updated verification status
    */
    @Override
    @Transactional
    public EkycResponseDto resubmitVerification(String verificationId, EkycRequestDto updatedRequestDto) {
        auditLogger.info("Resubmitting verification for ID: {}", verificationId);

        EkycRequest ekycRequest = findAndValidateVerificationRequest(verificationId);

        // Check if the verification is in a state that can be resubmitted
        if (ekycRequest.getStatus() != VerificationStatus.KYC_DATA_MISMATCH &&
        ekycRequest.getStatus() != VerificationStatus.FAILED &&
        ekycRequest.getStatus() != VerificationStatus.OTP_VERIFICATION_FAILED) {
            auditLogger.warn("Cannot resubmit verification with status: {}", ekycRequest.getStatus());
            throw new IllegalStateException("Verification cannot be resubmitted in its current state");
        }

        validateRequestData(updatedRequestDto);

        // Update the eKYC request with new information
        ekycRequest.setAadhaarNumber(updatedRequestDto.getAadhaarNumber());
        ekycRequest.setName(updatedRequestDto.getName());
        ekycRequest.setDateOfBirth(updatedRequestDto.getDateOfBirth());
        ekycRequest.setGender(updatedRequestDto.getGender());
        ekycRequest.setMobileNumber(updatedRequestDto.getMobileNumber());
        ekycRequest.setEmail(updatedRequestDto.getEmail());
        ekycRequest.setAddress(updatedRequestDto.getAddress());
        ekycRequest.setStatus(VerificationStatus.INITIATED);
        ekycRequest.setUpdatedAt(LocalDateTime.now());
        ekycRequest.setAttempts(0);
        ekycRequest.setFailureReason(null);

        ekycRequestRepository.save(ekycRequest);

        // Initiate OTP with UIDAI
        try {
            UidaiOtpInitiateRequestDto otpRequest = new UidaiOtpInitiateRequestDto();
            otpRequest.setAadhaarNumber(updatedRequestDto.getAadhaarNumber());
            otpRequest.setMobileNumber(updatedRequestDto.getMobileNumber());

            CompletableFuture<UidaiOtpInitiateResponseDto> otpResponseFuture = uidaiApiService.initiateOtp(otpRequest);
            UidaiOtpInitiateResponseDto otpResponse = otpResponseFuture.get();

            if (!otpResponse.isSuccess()) {
                ekycRequest.setStatus(VerificationStatus.FAILED);
                ekycRequest.setFailureReason(otpResponse.getErrorMessage());
                ekycRequestRepository.save(ekycRequest);

                auditLogger.error("OTP initiation failed during resubmission for verification ID: {}", verificationId);
                throw new EkycServiceException("Failed to initiate OTP: " + otpResponse.getErrorMessage());
            }

            ekycRequest.setTransactionId(otpResponse.getTransactionId());
            ekycRequestRepository.save(ekycRequest);

            auditLogger.info("Verification resubmitted successfully for ID: {}", verificationId);

            return createResponseDto(ekycRequest);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            ekycRequest.setStatus(VerificationStatus.FAILED);
            ekycRequest.setFailureReason("OTP initiation service error during resubmission");
            ekycRequestRepository.save(ekycRequest);

            auditLogger.error("Error during verification resubmission for ID: {}", verificationId, e);
            throw new EkycServiceException("Error during verification resubmission: " + e.getMessage(), e);
        }
    }

    /**
    * Cleans up expired verification requests.
    *
    * @return The number of expired requests deleted
    */
    @Override
    @Transactional
    public int cleanupExpiredVerifications() {
        LocalDateTime expiryTime = LocalDateTime.now().minusDays(verificationExpiryDays);
        auditLogger.info("Cleaning up expired verifications older than: {}", expiryTime);

        int deletedCount = ekycRequestRepository.deleteExpiredRequests(expiryTime);

        auditLogger.info("Deleted {} expired verification requests", deletedCount);
        return deletedCount;
    }

    /**
    * Validates the eKYC request data.
    *
    * @param requestDto The eKYC request data to validate
    * @throws IllegalArgumentException if the request data is invalid
    */
    private void validateRequestData(EkycRequestDto requestDto) {
        if (requestDto == null) {
            throw new IllegalArgumentException("Request data cannot be null");
        }

        if (requestDto.getAadhaarNumber() == null || !requestDto.getAadhaarNumber().matches("^[0-9]{12}$")) {
            throw new IllegalArgumentException("Invalid Aadhaar number format");
        }

        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        if (requestDto.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Date of birth cannot be null");
        }

        if (requestDto.getGender() == null) {
            throw new IllegalArgumentException("Gender cannot be null");
        }

        if (requestDto.getMobileNumber() == null || !requestDto.getMobileNumber().matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("Invalid mobile number format");
        }

        // Email validation (optional field)
        if (requestDto.getEmail() != null && !requestDto.getEmail().isEmpty() &&
        !requestDto.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (requestDto.getAddress() == null || requestDto.getAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Address cannot be empty");
        }
    }

    /**
    * Finds and validates an eKYC verification request by ID.
    *
    * @param verificationId The verification ID to look up
    * @return The found EkycRequest entity
    * @throws ResourceNotFoundException if the verification ID is not found
    */
    private EkycRequest findAndValidateVerificationRequest(String verificationId) {
        if (verificationId == null || verificationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Verification ID cannot be null or empty");
        }

        return ekycRequestRepository.findByVerificationId(verificationId)
        .orElseThrow(() -> {
            auditLogger.warn("Verification not found with ID: {}", verificationId);
            return new ResourceNotFoundException("Verification not found with ID: " + verificationId);
        });
    }

    /**
    * Creates a response DTO from an eKYC request entity.
    *
    * @param ekycRequest The eKYC request entity
    * @return EkycResponseDto with the verification details
    */
    private EkycResponseDto createResponseDto(EkycRequest ekycRequest) {
        EkycResponseDto responseDto = new EkycResponseDto();
        responseDto.setVerificationId(ekycRequest.getVerificationId());
        responseDto.setStatus(ekycRequest.getStatus());
        responseDto.setCreatedAt(ekycRequest.getCreatedAt());
        responseDto.setUpdatedAt(ekycRequest.getUpdatedAt());
        responseDto.setVerifiedAt(ekycRequest.getVerifiedAt());
        responseDto.setFailureReason(ekycRequest.getFailureReason());
        responseDto.setAttempts(ekycRequest.getAttempts());

        // Include user details in the response
        responseDto.setAadhaarNumber(ekycRequest.getAadhaarNumber());
        responseDto.setName(ekycRequest.getName());
        responseDto.setDateOfBirth(ekycRequest.getDateOfBirth());
        responseDto.setGender(ekycRequest.getGender());
        responseDto.setMobileNumber(ekycRequest.getMobileNumber());
        responseDto.setEmail(ekycRequest.getEmail());
        responseDto.setAddress(ekycRequest.getAddress());

        return responseDto;
    }

    /**
    * Generates a unique verification ID.
    *
    * @return A unique verification ID string
    */
    private String generateVerificationId() {
        return "EKYC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}