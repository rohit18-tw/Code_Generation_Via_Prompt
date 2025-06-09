package com.ekyc.service.service;

import com.ekyc.service.dto.EkycResponseDto;
import com.ekyc.service.dto.OtpVerificationDto;
import com.ekyc.service.dto.UidaiOtpVerifyRequestDto;
import com.ekyc.service.dto.UidaiOtpVerifyResponseDto;
import com.ekyc.service.entity.EkycTransaction;
import com.ekyc.service.enums.EkycStatus;
import com.ekyc.service.exception.EkycException;
import com.ekyc.service.repository.EkycTransactionRepository;
import com.ekyc.service.service.impl.OtpVerificationServiceImpl;
import com.ekyc.service.util.UidaiApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class OtpVerificationServiceImplTest {

    private OtpVerificationServiceImpl otpVerificationService;
    private EkycTransactionRepository ekycTransactionRepository;
    private UidaiApiClient uidaiApiClient;

    @BeforeEach
    public void setUp() {
        ekycTransactionRepository = new MockEkycTransactionRepository();
        uidaiApiClient = new MockUidaiApiClient();
        otpVerificationService = new OtpVerificationServiceImpl(ekycTransactionRepository, uidaiApiClient);
    }

    @Test
    @DisplayName("Should verify OTP successfully")
    public void testVerifyOtpSuccess() {
        // Given
        String transactionId = "test-transaction-id";
        String otp = "123456";
        String aadhaarNumber = "123456789012";

        EkycTransaction transaction = new EkycTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setAadhaarNumber(aadhaarNumber);
        transaction.setStatus(EkycStatus.OTP_INITIATED);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setTxnId("uidai-txn-id");
        ((MockEkycTransactionRepository) ekycTransactionRepository).addTransaction(transaction);

        OtpVerificationDto otpVerificationDto = new OtpVerificationDto();
        otpVerificationDto.setTransactionId(transactionId);
        otpVerificationDto.setOtp(otp);

        // When
        EkycResponseDto response = otpVerificationService.verifyOtp(otpVerificationDto);

        // Then
        assertNotNull(response);
        assertEquals("OTP verification successful", response.getMessage());
        assertEquals(EkycStatus.OTP_VERIFIED, response.getStatus());
        assertEquals(transactionId, response.getTransactionId());

        Optional<EkycTransaction> updatedTransaction = ekycTransactionRepository.findByTransactionId(transactionId);
        assertTrue(updatedTransaction.isPresent());
        assertEquals(EkycStatus.OTP_VERIFIED, updatedTransaction.get().getStatus());
    }

    @Test
    @DisplayName("Should throw exception when transaction not found")
    public void testVerifyOtpTransactionNotFound() {
        // Given
        String transactionId = "non-existent-transaction-id";
        String otp = "123456";

        OtpVerificationDto otpVerificationDto = new OtpVerificationDto();
        otpVerificationDto.setTransactionId(transactionId);
        otpVerificationDto.setOtp(otp);

        // When & Then
        EkycException exception = assertThrows(EkycException.class, () -> {
            otpVerificationService.verifyOtp(otpVerificationDto);
        });

        assertEquals("Transaction not found for the given transaction ID", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when transaction status is not OTP_INITIATED")
    public void testVerifyOtpInvalidTransactionStatus() {
        // Given
        String transactionId = "test-transaction-id";
        String otp = "123456";
        String aadhaarNumber = "123456789012";

        EkycTransaction transaction = new EkycTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setAadhaarNumber(aadhaarNumber);
        transaction.setStatus(EkycStatus.COMPLETED); // Invalid status for OTP verification
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setTxnId("uidai-txn-id");
        ((MockEkycTransactionRepository) ekycTransactionRepository).addTransaction(transaction);

        OtpVerificationDto otpVerificationDto = new OtpVerificationDto();
        otpVerificationDto.setTransactionId(transactionId);
        otpVerificationDto.setOtp(otp);

        // When & Then
        EkycException exception = assertThrows(EkycException.class, () -> {
            otpVerificationService.verifyOtp(otpVerificationDto);
        });

        assertEquals("Invalid transaction status for OTP verification", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when UIDAI API returns error")
    public void testVerifyOtpUidaiApiError() {
        // Given
        String transactionId = "test-transaction-id";
        String otp = "999999"; // Special OTP to trigger error in mock
        String aadhaarNumber = "123456789012";

        EkycTransaction transaction = new EkycTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setAadhaarNumber(aadhaarNumber);
        transaction.setStatus(EkycStatus.OTP_INITIATED);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setTxnId("uidai-txn-id");
        ((MockEkycTransactionRepository) ekycTransactionRepository).addTransaction(transaction);

        OtpVerificationDto otpVerificationDto = new OtpVerificationDto();
        otpVerificationDto.setTransactionId(transactionId);
        otpVerificationDto.setOtp(otp);

        // When & Then
        EkycException exception = assertThrows(EkycException.class, () -> {
            otpVerificationService.verifyOtp(otpVerificationDto);
        });

        assertEquals("Failed to verify OTP with UIDAI", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when OTP is invalid")
    public void testVerifyOtpInvalidOtp() {
        // Given
        String transactionId = "test-transaction-id";
        String otp = "111111"; // Special OTP to trigger invalid OTP in mock
        String aadhaarNumber = "123456789012";

        EkycTransaction transaction = new EkycTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setAadhaarNumber(aadhaarNumber);
        transaction.setStatus(EkycStatus.OTP_INITIATED);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setTxnId("uidai-txn-id");
        ((MockEkycTransactionRepository) ekycTransactionRepository).addTransaction(transaction);

        OtpVerificationDto otpVerificationDto = new OtpVerificationDto();
        otpVerificationDto.setTransactionId(transactionId);
        otpVerificationDto.setOtp(otp);

        // When & Then
        EkycException exception = assertThrows(EkycException.class, () -> {
            otpVerificationService.verifyOtp(otpVerificationDto);
        });

        assertEquals("Invalid OTP", exception.getMessage());
    }

    /**
    * Mock implementation of EkycTransactionRepository for testing
    */
    private static class MockEkycTransactionRepository implements EkycTransactionRepository {
        private final java.util.Map<String, EkycTransaction> transactions = new java.util.HashMap<>();

        public void addTransaction(EkycTransaction transaction) {
            transactions.put(transaction.getTransactionId(), transaction);
        }

        @Override
        public Optional<EkycTransaction> findByTransactionId(String transactionId) {
            return Optional.ofNullable(transactions.get(transactionId));
        }

        @Override
        public EkycTransaction save(EkycTransaction transaction) {
            transactions.put(transaction.getTransactionId(), transaction);
            return transaction;
        }

        @Override
        public <S extends EkycTransaction> Iterable<S> saveAll(Iterable<S> entities) {
            return null;
        }

        @Override
        public Optional<EkycTransaction> findById(Long aLong) {
            return Optional.empty();
        }

        @Override
        public boolean existsById(Long aLong) {
            return false;
        }

        @Override
        public Iterable<EkycTransaction> findAll() {
            return transactions.values();
        }

        @Override
        public Iterable<EkycTransaction> findAllById(Iterable<Long> longs) {
            return null;
        }

        @Override
        public long count() {
            return 0;
        }

        @Override
        public void deleteById(Long aLong) {
            // Not implemented for test
        }

        @Override
        public void delete(EkycTransaction entity) {
            // Not implemented for test
        }

        @Override
        public void deleteAllById(Iterable<? extends Long> longs) {
            // Not implemented for test
        }

        @Override
        public void deleteAll(Iterable<? extends EkycTransaction> entities) {
            // Not implemented for test
        }

        @Override
        public void deleteAll() {
            // Not implemented for test
        }
    }

    /**
    * Mock implementation of UidaiApiClient for testing
    */
    private static class MockUidaiApiClient implements UidaiApiClient {
        @Override
        public UidaiOtpVerifyResponseDto verifyOtp(UidaiOtpVerifyRequestDto requestDto) {
            UidaiOtpVerifyResponseDto responseDto = new UidaiOtpVerifyResponseDto();

            // Simulate different responses based on OTP value
            if ("999999".equals(requestDto.getOtp())) {
                throw new RuntimeException("UIDAI API error");
            } else if ("111111".equals(requestDto.getOtp())) {
                responseDto.setStatus("error");
                responseDto.setMessage("Invalid OTP");
            } else {
                responseDto.setStatus("success");
                responseDto.setMessage("OTP verification successful");
                responseDto.setEkycXml("<xml>Sample eKYC XML data</xml>");
            }

            return responseDto;
        }

        // Other methods not needed for this test
        @Override
        public Object initiateOtp(Object requestDto) {
            return null;
        }
    }
}