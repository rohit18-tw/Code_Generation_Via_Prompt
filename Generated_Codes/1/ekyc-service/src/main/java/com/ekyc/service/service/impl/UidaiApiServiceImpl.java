package com.ekyc.service.service.impl;

import com.ekyc.service.dto.UidaiOtpInitiateRequestDto;
import com.ekyc.service.dto.UidaiOtpInitiateResponseDto;
import com.ekyc.service.dto.UidaiOtpVerifyRequestDto;
import com.ekyc.service.dto.UidaiOtpVerifyResponseDto;
import com.ekyc.service.exception.EkycException;
import com.ekyc.service.service.UidaiApiService;
import com.ekyc.service.util.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
* Implementation of the UIDAI API service for interacting with the UIDAI (Aadhaar) system.
* This service handles OTP initiation, verification, and other UIDAI-related operations.
*/
@Service
public class UidaiApiServiceImpl implements UidaiApiService {

    private static final Logger logger = LoggerFactory.getLogger(UidaiApiServiceImpl.class);
    private static final AuditLogger auditLogger = new AuditLogger(UidaiApiServiceImpl.class);

    private final WebClient webClient;

    @Value("${uidai.api.base-url}")
    private String uidaiBaseUrl;

    @Value("${uidai.api.otp.initiate-endpoint}")
    private String otpInitiateEndpoint;

    @Value("${uidai.api.otp.verify-endpoint}")
    private String otpVerifyEndpoint;

    @Value("${uidai.api.info-endpoint}")
    private String apiInfoEndpoint;

    @Value("${uidai.api.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${uidai.api.version}")
    private String apiVersion;

    @Value("${uidai.api.license-key}")
    private String licenseKey;

    /**
    * Constructor for UidaiApiServiceImpl.
    * Initializes the WebClient for making HTTP requests to the UIDAI API.
    */
    public UidaiApiServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
        .baseUrl(uidaiBaseUrl)
        .defaultHeader("X-API-Version", apiVersion)
        .defaultHeader("X-License-Key", licenseKey)
        .build();

        logger.info("UidaiApiServiceImpl initialized with base URL: {}", uidaiBaseUrl);
    }

    /**
    * Initiates an OTP request to the UIDAI system.
    *
    * @param requestDto The OTP initiation request containing Aadhaar number and other details
    * @return A CompletableFuture containing the OTP initiation response
    */
    @Override
    public CompletableFuture<UidaiOtpInitiateResponseDto> initiateOtp(UidaiOtpInitiateRequestDto requestDto) {
        if (requestDto == null || requestDto.getAadhaarNumber() == null || requestDto.getAadhaarNumber().isEmpty()) {
            logger.error("Invalid OTP initiation request: Aadhaar number is missing");
            return CompletableFuture.failedFuture(
            new EkycException("Aadhaar number is required for OTP initiation", HttpStatus.BAD_REQUEST));
        }

        auditLogger.info("Initiating OTP for Aadhaar: {}", maskAadhaarNumber(requestDto.getAadhaarNumber()));

        return CompletableFuture.supplyAsync(() -> {
            try {
                return webClient.post()
                .uri(otpInitiateEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(UidaiOtpInitiateResponseDto.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();
            } catch (WebClientResponseException e) {
                logger.error("UIDAI API error during OTP initiation: {} - {}", e.getStatusCode(), e.getMessage());
                throw new EkycException("Failed to initiate OTP with UIDAI: " + e.getMessage(),
                HttpStatus.valueOf(e.getStatusCode().value()));
            } catch (Exception e) {
                if (e.getCause() instanceof TimeoutException) {
                    logger.error("Timeout while connecting to UIDAI API for OTP initiation", e);
                    throw new EkycException("Connection timeout while initiating OTP", HttpStatus.GATEWAY_TIMEOUT);
                }
                logger.error("Unexpected error during OTP initiation with UIDAI", e);
                throw new EkycException("Failed to initiate OTP: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });
    }

    /**
    * Verifies an OTP with the UIDAI system.
    *
    * @param requestDto The OTP verification request containing Aadhaar number, OTP, and transaction ID
    * @return A CompletableFuture containing the OTP verification response
    */
    @Override
    public CompletableFuture<UidaiOtpVerifyResponseDto> verifyOtp(UidaiOtpVerifyRequestDto requestDto) {
        if (requestDto == null || requestDto.getAadhaarNumber() == null || requestDto.getAadhaarNumber().isEmpty()) {
            logger.error("Invalid OTP verification request: Aadhaar number is missing");
            return CompletableFuture.failedFuture(
            new EkycException("Aadhaar number is required for OTP verification", HttpStatus.BAD_REQUEST));
        }

        if (requestDto.getOtp() == null || requestDto.getOtp().isEmpty()) {
            logger.error("Invalid OTP verification request: OTP is missing");
            return CompletableFuture.failedFuture(
            new EkycException("OTP is required for verification", HttpStatus.BAD_REQUEST));
        }

        if (requestDto.getTransactionId() == null || requestDto.getTransactionId().isEmpty()) {
            logger.error("Invalid OTP verification request: Transaction ID is missing");
            return CompletableFuture.failedFuture(
            new EkycException("Transaction ID is required for OTP verification", HttpStatus.BAD_REQUEST));
        }

        auditLogger.info("Verifying OTP for Aadhaar: {}, Transaction ID: {}",
        maskAadhaarNumber(requestDto.getAadhaarNumber()), requestDto.getTransactionId());

        return CompletableFuture.supplyAsync(() -> {
            try {
                return webClient.post()
                .uri(otpVerifyEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(UidaiOtpVerifyResponseDto.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();
            } catch (WebClientResponseException e) {
                logger.error("UIDAI API error during OTP verification: {} - {}", e.getStatusCode(), e.getMessage());
                throw new EkycException("Failed to verify OTP with UIDAI: " + e.getMessage(),
                HttpStatus.valueOf(e.getStatusCode().value()));
            } catch (Exception e) {
                if (e.getCause() instanceof TimeoutException) {
                    logger.error("Timeout while connecting to UIDAI API for OTP verification", e);
                    throw new EkycException("Connection timeout while verifying OTP", HttpStatus.GATEWAY_TIMEOUT);
                }
                logger.error("Unexpected error during OTP verification with UIDAI", e);
                throw new EkycException("Failed to verify OTP: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });
    }

    /**
    * Retrieves information about the UIDAI API, including version and configuration details.
    *
    * @return A CompletableFuture containing a String with version and configuration information
    */
    @Override
    public CompletableFuture<String> getApiInfo() {
        logger.info("Retrieving UIDAI API information");

        return CompletableFuture.supplyAsync(() -> {
            try {
                return webClient.get()
                .uri(apiInfoEndpoint)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();
            } catch (WebClientResponseException e) {
                logger.error("UIDAI API error while retrieving API info: {} - {}", e.getStatusCode(), e.getMessage());
                throw new EkycException("Failed to retrieve UIDAI API information: " + e.getMessage(),
                HttpStatus.valueOf(e.getStatusCode().value()));
            } catch (Exception e) {
                if (e.getCause() instanceof TimeoutException) {
                    logger.error("Timeout while connecting to UIDAI API for information", e);
                    throw new EkycException("Connection timeout while retrieving API information", HttpStatus.GATEWAY_TIMEOUT);
                }
                logger.error("Unexpected error while retrieving UIDAI API information", e);
                throw new EkycException("Failed to retrieve API information: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });
    }

    /**
    * Masks an Aadhaar number for logging purposes to protect PII.
    * Only the last 4 digits are visible, the rest are masked with 'X'.
    *
    * @param aadhaarNumber The Aadhaar number to mask
    * @return The masked Aadhaar number
    */
    private String maskAadhaarNumber(String aadhaarNumber) {
        if (aadhaarNumber == null || aadhaarNumber.length() < 4) {
            return "INVALID_AADHAAR";
        }

        int length = aadhaarNumber.length();
        int visibleDigits = 4;
        int maskedLength = length - visibleDigits;

        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < maskedLength; i++) {
            masked.append("X");
        }

        masked.append(aadhaarNumber.substring(maskedLength));
        return masked.toString();
    }
}