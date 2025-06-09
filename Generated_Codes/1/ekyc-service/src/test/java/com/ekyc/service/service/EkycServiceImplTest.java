package com.ekyc.service.service;

import com.ekyc.service.dto.EkycRequestDto;
import com.ekyc.service.dto.EkycResponseDto;
import com.ekyc.service.dto.UidaiOtpInitiateRequestDto;
import com.ekyc.service.dto.UidaiOtpInitiateResponseDto;
import com.ekyc.service.dto.UidaiOtpVerifyRequestDto;
import com.ekyc.service.dto.UidaiOtpVerifyResponseDto;
import com.ekyc.service.entity.EkycTransaction;
import com.ekyc.service.enums.EkycStatus;
import com.ekyc.service.exception.EkycException;
import com.ekyc.service.exception.UidaiServiceException;
import com.ekyc.service.repository.EkycTransactionRepository;
import com.ekyc.service.service.impl.EkycServiceImpl;
import com.ekyc.service.service.impl.UidaiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class EkycServiceImplTest {

    private EkycServiceImpl ekycService;
    private UidaiServiceImpl uidaiService;
    private EkycTransactionRepository ekycTransactionRepository;

    @BeforeEach
    public void setUp() {
        // Create mocks manually without Mockito
        uidaiService = new TestUidaiService();
        ekycTransactionRepository = new TestEkycTransactionRepository();
        ekycService = new EkycServiceImpl(uidaiService, ekycTransactionRepository);
    }

    @Test
    public void testInitiateEkyc_Success() {
        // Arrange
        EkycRequestDto requestDto = new EkycRequestDto();
        requestDto.setAadhaarNumber("123456789012");
        requestDto.setMobileNumber("9876543210");
        requestDto.setName("Test User");

        // Act
        EkycResponseDto responseDto = ekycService.initiateEkyc(requestDto);

        // Assert
        assertNotNull(responseDto);
        assertNotNull(responseDto.getVerificationId());
        assertEquals(EkycStatus.OTP_INITIATED, responseDto.getStatus());
        assertTrue(responseDto.getVerificationId().startsWith("EKYC-"));
    }

    @Test
    public void testInitiateEkyc_UidaiServiceFailure() {
        // Arrange
        EkycRequestDto requestDto = new EkycRequestDto();
        requestDto.setAadhaarNumber("999999999999"); // Trigger failure in test service
        requestDto.setMobileNumber("9876543210");
        requestDto.setName("Test User");

        // Act & Assert
        Exception exception = assertThrows(EkycException.class, () -> {
            ekycService.initiateEkyc(requestDto);
        });

        assertTrue(exception.getMessage().contains("Failed to initiate OTP"));
    }

    @Test
    public void testVerifyEkyc_Success() {
        // Arrange
        String verificationId = "EKYC-12345678";
        String otp = "123456";

        // Create a transaction in the repository
        EkycTransaction transaction = new EkycTransaction();
        transaction.setVerificationId(verificationId);
        transaction.setAadhaarNumber("123456789012");
        transaction.setMobileNumber("9876543210");
        transaction.setName("Test User");
        transaction.setStatus(EkycStatus.OTP_INITIATED);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setTxnId("TXN123456");
        ((TestEkycTransactionRepository) ekycTransactionRepository).save(transaction);

        // Act
        EkycResponseDto responseDto = ekycService.verifyEkyc(verificationId, otp);

        // Assert
        assertNotNull(responseDto);
        assertEquals(verificationId, responseDto.getVerificationId());
        assertEquals(EkycStatus.VERIFIED, responseDto.getStatus());
    }

    @Test
    public void testVerifyEkyc_InvalidVerificationId() {
        // Arrange
        String verificationId = "EKYC-INVALID";
        String otp = "123456";

        // Act & Assert
        Exception exception = assertThrows(EkycException.class, () -> {
            ekycService.verifyEkyc(verificationId, otp);
        });

        assertTrue(exception.getMessage().contains("No eKYC transaction found"));
    }

    @Test
    public void testVerifyEkyc_UidaiVerificationFailure() {
        // Arrange
        String verificationId = "EKYC-12345678";
        String otp = "999999"; // Trigger failure in test service

        // Create a transaction in the repository
        EkycTransaction transaction = new EkycTransaction();
        transaction.setVerificationId(verificationId);
        transaction.setAadhaarNumber("123456789012");
        transaction.setMobileNumber("9876543210");
        transaction.setName("Test User");
        transaction.setStatus(EkycStatus.OTP_INITIATED);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setTxnId("TXN123456");
        ((TestEkycTransactionRepository) ekycTransactionRepository).save(transaction);

        // Act & Assert
        Exception exception = assertThrows(EkycException.class, () -> {
            ekycService.verifyEkyc(verificationId, otp);
        });

        assertTrue(exception.getMessage().contains("Failed to verify OTP"));
    }

    @Test
    public void testGetEkycStatus_Success() {
        // Arrange
        String verificationId = "EKYC-12345678";

        // Create a transaction in the repository
        EkycTransaction transaction = new EkycTransaction();
        transaction.setVerificationId(verificationId);
        transaction.setAadhaarNumber("123456789012");
        transaction.setMobileNumber("9876543210");
        transaction.setName("Test User");
        transaction.setStatus(EkycStatus.VERIFIED);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setTxnId("TXN123456");
        ((TestEkycTransactionRepository) ekycTransactionRepository).save(transaction);

        // Act
        EkycResponseDto responseDto = ekycService.getEkycStatus(verificationId);

        // Assert
        assertNotNull(responseDto);
        assertEquals(verificationId, responseDto.getVerificationId());
        assertEquals(EkycStatus.VERIFIED, responseDto.getStatus());
    }

    @Test
    public void testGetEkycStatus_InvalidVerificationId() {
        // Arrange
        String verificationId = "EKYC-INVALID";

        // Act & Assert
        Exception exception = assertThrows(EkycException.class, () -> {
            ekycService.getEkycStatus(verificationId);
        });

        assertTrue(exception.getMessage().contains("No eKYC transaction found"));
    }

    /**
    * Test implementation of UidaiService for unit testing
    */
    private static class TestUidaiService extends UidaiServiceImpl {

        @Override
        public UidaiOtpInitiateResponseDto initiateOtp(UidaiOtpInitiateRequestDto requestDto) {
            if ("999999999999".equals(requestDto.getAadhaarNumber())) {
                throw new UidaiServiceException("UIDAI service error");
            }

            UidaiOtpInitiateResponseDto responseDto = new UidaiOtpInitiateResponseDto();
            responseDto.setTxnId("TXN123456");
            responseDto.setStatus("SUCCESS");
            return responseDto;
        }

        @Override
        public UidaiOtpVerifyResponseDto verifyOtp(UidaiOtpVerifyRequestDto requestDto) {
            if ("999999".equals(requestDto.getOtp())) {
                throw new UidaiServiceException("UIDAI verification error");
            }

            UidaiOtpVerifyResponseDto responseDto = new UidaiOtpVerifyResponseDto();
            responseDto.setStatus("SUCCESS");
            responseDto.setEkycXml("<ekyc>Sample XML data</ekyc>");
            return responseDto;
        }
    }

    /**
    * Test implementation of EkycTransactionRepository for unit testing
    */
    private static class TestEkycTransactionRepository implements EkycTransactionRepository {
        private final java.util.Map<String, EkycTransaction> transactions = new java.util.HashMap<>();

        @Override
        public EkycTransaction save(EkycTransaction transaction) {
            transactions.put(transaction.getVerificationId(), transaction);
            return transaction;
        }

        @Override
        public Optional<EkycTransaction> findByVerificationId(String verificationId) {
            return Optional.ofNullable(transactions.get(verificationId));
        }
    }
}