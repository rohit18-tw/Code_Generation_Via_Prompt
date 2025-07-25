package com.mock.uidai.service;

import com.mock.uidai.dto.KycDataDto;
import com.mock.uidai.dto.OtpInitiateRequestDto;
import com.mock.uidai.dto.OtpInitiateResponseDto;
import com.mock.uidai.dto.OtpVerifyRequestDto;
import com.mock.uidai.dto.OtpVerifyResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
* Service class that provides mock implementations of UIDAI (Aadhaar) services.
* This includes OTP generation, verification, and KYC data retrieval.
*/
@Service
public class MockUidaiService {
    private static final Logger logger = LoggerFactory.getLogger(MockUidaiService.class);

    // Store OTPs with transaction IDs
    private final Map<String, String> otpStore = new ConcurrentHashMap<>();

    // Store KYC data with Aadhaar numbers
    private final Map<String, KycDataDto> kycDataStore = new HashMap<>();

    // Default OTP for testing purposes
    private static final String DEFAULT_OTP = "123456";

    // OTP validity duration in minutes
    private static final int OTP_VALIDITY_MINUTES = 10;

    // Map to store transaction timestamps
    private final Map<String, LocalDateTime> txnTimestamps = new ConcurrentHashMap<>();

    /**
    * Initialize mock KYC data for testing.
    */
    @PostConstruct
    public void init() {
        // Add some sample KYC data
        kycDataStore.put("999999999999", createSampleKycData("999999999999", "John", "Doe"));
        kycDataStore.put("888888888888", createSampleKycData("888888888888", "Jane", "Smith"));
        kycDataStore.put("777777777777", createSampleKycData("777777777777", "Alice", "Johnson"));

        logger.info("MockUidaiService initialized with {} sample KYC records", kycDataStore.size());
    }

    /**
    * Initiates OTP generation for the given Aadhaar number.
    *
    * @param request The OTP initiation request
    * @return OTP initiation response with transaction ID
    */
    public OtpInitiateResponseDto initiateOtp(OtpInitiateRequestDto request) {
        logger.info("OTP initiation request received for Aadhaar: {}", maskAadhaar(request.getUid()));

        validateAadhaarNumber(request.getUid());

        String txnId = generateTransactionId();
        String otp = generateOtp();

        // Store OTP for verification
        otpStore.put(txnId, otp);
        txnTimestamps.put(txnId, LocalDateTime.now());

        logger.debug("Generated OTP for txnId: {}", txnId);

        OtpInitiateResponseDto response = new OtpInitiateResponseDto();
        response.setStatus("Success");
        response.setStatusCode("200");
        response.setStatusMessage("OTP generated successfully");
        response.setMaskedTxnId(maskTransactionId(txnId));
        response.setTxnId(txnId);

        logger.info("OTP initiated successfully for Aadhaar: {}, txnId: {}",
        maskAadhaar(request.getUid()), maskTransactionId(txnId));

        return response;
    }

    /**
    * Verifies the OTP against the transaction ID.
    *
    * @param request The OTP verification request
    * @return OTP verification response
    */
    public OtpVerifyResponseDto verifyOtp(OtpVerifyRequestDto request) {
        logger.info("OTP verification request received for txnId: {}", maskTransactionId(request.getTxnId()));

        OtpVerifyResponseDto response = new OtpVerifyResponseDto();

        // Check if transaction exists
        if (!otpStore.containsKey(request.getTxnId())) {
            logger.warn("Invalid transaction ID: {}", maskTransactionId(request.getTxnId()));
            return createErrorResponse("Invalid transaction ID", "404");
        }

        // Check if OTP has expired
        if (isOtpExpired(request.getTxnId())) {
            logger.warn("OTP expired for txnId: {}", maskTransactionId(request.getTxnId()));
            return createErrorResponse("OTP has expired", "401");
        }

        // Verify OTP
        String storedOtp = otpStore.get(request.getTxnId());
        if (storedOtp.equals(request.getOtp()) || DEFAULT_OTP.equals(request.getOtp())) {
            logger.info("OTP verified successfully for txnId: {}", maskTransactionId(request.getTxnId()));

            // Get KYC data if available
            KycDataDto kycData = null;
            if (request.getUid() != null && kycDataStore.containsKey(request.getUid())) {
                kycData = kycDataStore.get(request.getUid());
                logger.debug("KYC data found for Aadhaar: {}", maskAadhaar(request.getUid()));
            }

            response.setStatus("Success");
            response.setStatusCode("200");
            response.setStatusMessage("OTP verification successful");
            response.setMaskedTxnId(maskTransactionId(request.getTxnId()));
            response.setTxnId(request.getTxnId());
            response.setKycData(kycData);

            // Remove the OTP after successful verification
            otpStore.remove(request.getTxnId());
            txnTimestamps.remove(request.getTxnId());

            return response;
        } else {
            logger.warn("Invalid OTP provided for txnId: {}", maskTransactionId(request.getTxnId()));
            return createErrorResponse("Invalid OTP", "401");
        }
    }

    /**
    * Retrieves KYC data for the given Aadhaar number.
    *
    * @param aadhaarNumber The Aadhaar number
    * @return KYC data if available, null otherwise
    */
    public KycDataDto getKycData(String aadhaarNumber) {
        logger.info("KYC data request received for Aadhaar: {}", maskAadhaar(aadhaarNumber));

        validateAadhaarNumber(aadhaarNumber);

        KycDataDto kycData = kycDataStore.get(aadhaarNumber);

        if (kycData != null) {
            logger.info("KYC data found for Aadhaar: {}", maskAadhaar(aadhaarNumber));
            return kycData;
        } else {
            logger.warn("No KYC data found for Aadhaar: {}", maskAadhaar(aadhaarNumber));
            return null;
        }
    }

    /**
    * Creates a sample KYC data record.
    *
    * @param aadhaarNumber The Aadhaar number
    * @param firstName The first name
    * @param lastName The last name
    * @return A sample KYC data record
    */
    private KycDataDto createSampleKycData(String aadhaarNumber, String firstName, String lastName) {
        KycDataDto kycData = new KycDataDto();
        kycData.setUid(aadhaarNumber);
        kycData.setName(firstName + " " + lastName);
        kycData.setDob("1990-01-01");
        kycData.setGender("M");
        kycData.setAddress("123 Sample Street, Sample City, Sample State - 123456");
        kycData.setMobile("9999999999");
        kycData.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com");
        kycData.setPhoto(generateSamplePhoto());
        return kycData;
    }

    /**
    * Generates a random OTP.
    *
    * @return A 6-digit OTP
    */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
    * Generates a unique transaction ID.
    *
    * @return A unique transaction ID
    */
    private String generateTransactionId() {
        return UUID.randomUUID().toString();
    }

    /**
    * Masks an Aadhaar number for logging purposes.
    *
    * @param aadhaarNumber The Aadhaar number to mask
    * @return Masked Aadhaar number
    */
    private String maskAadhaar(String aadhaarNumber) {
        if (aadhaarNumber == null || aadhaarNumber.length() < 4) {
            return "****";
        }
        return "XXXX-XXXX-" + aadhaarNumber.substring(aadhaarNumber.length() - 4);
    }

    /**
    * Masks a transaction ID for logging purposes.
    *
    * @param txnId The transaction ID to mask
    * @return Masked transaction ID
    */
    private String maskTransactionId(String txnId) {
        if (txnId == null || txnId.length() < 8) {
            return "********";
        }
        return txnId.substring(0, 4) + "..." + txnId.substring(txnId.length() - 4);
    }

    /**
    * Validates an Aadhaar number.
    *
    * @param aadhaarNumber The Aadhaar number to validate
    * @throws IllegalArgumentException if the Aadhaar number is invalid
    */
    private void validateAadhaarNumber(String aadhaarNumber) {
        if (aadhaarNumber == null || aadhaarNumber.trim().isEmpty()) {
            logger.error("Aadhaar number cannot be null or empty");
            throw new IllegalArgumentException("Aadhaar number cannot be null or empty");
        }

        if (!aadhaarNumber.matches("\\d{12}")) {
            logger.error("Invalid Aadhaar number format: {}", maskAadhaar(aadhaarNumber));
            throw new IllegalArgumentException("Aadhaar number must be 12 digits");
        }
    }

    /**
    * Checks if an OTP has expired.
    *
    * @param txnId The transaction ID
    * @return true if the OTP has expired, false otherwise
    */
    private boolean isOtpExpired(String txnId) {
        LocalDateTime timestamp = txnTimestamps.get(txnId);
        if (timestamp == null) {
            return true;
        }

        return LocalDateTime.now().isAfter(timestamp.plusMinutes(OTP_VALIDITY_MINUTES));
    }

    /**
    * Creates an error response.
    *
    * @param message The error message
    * @param code The error code
    * @return An error response
    */
    private OtpVerifyResponseDto createErrorResponse(String message, String code) {
        OtpVerifyResponseDto response = new OtpVerifyResponseDto();
        response.setStatus("Failure");
        response.setStatusCode(code);
        response.setStatusMessage(message);
        return response;
    }

    /**
    * Generates a sample base64-encoded photo.
    *
    * @return A base64-encoded sample photo
    */
    private String generateSamplePhoto() {
        // This is a very small sample image data (1x1 pixel)
        byte[] sampleImageData = new byte[] {
            (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A,
            (byte) 0x1A, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0D
        };
        return Base64.getEncoder().encodeToString(sampleImageData);
    }
}