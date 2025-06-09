package com.mock.uidai.service;

import com.mock.uidai.dto.KycDataDto;
import com.mock.uidai.dto.OtpInitiateRequestDto;
import com.mock.uidai.dto.OtpInitiateResponseDto;
import com.mock.uidai.dto.OtpValidateRequestDto;
import com.mock.uidai.dto.OtpValidateResponseDto;
import com.mock.uidai.entity.AadhaarData;
import com.mock.uidai.entity.OtpTransaction;
import com.mock.uidai.exception.AadhaarNotFoundException;
import com.mock.uidai.exception.InvalidOtpException;
import com.mock.uidai.exception.OtpExpiredException;
import com.mock.uidai.repository.AadhaarDataRepository;
import com.mock.uidai.repository.OtpTransactionRepository;
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
class MockUidaiServiceTest {

    private MockUidaiService mockUidaiService;
    private AadhaarDataRepository aadhaarDataRepository;
    private OtpTransactionRepository otpTransactionRepository;

    @BeforeEach
    void setUp() {
        // Create custom test implementations of repositories
        aadhaarDataRepository = new TestAadhaarDataRepository();
        otpTransactionRepository = new TestOtpTransactionRepository();

        // Initialize the service with test repositories
        mockUidaiService = new MockUidaiService(aadhaarDataRepository, otpTransactionRepository);
    }

    @Test
    void initiateOtp_ValidAadhaar_ReturnsSuccessResponse() {
        // Arrange
        OtpInitiateRequestDto requestDto = new OtpInitiateRequestDto();
        requestDto.setUid("123456789012");

        // Act
        OtpInitiateResponseDto responseDto = mockUidaiService.initiateOtp(requestDto);

        // Assert
        assertNotNull(responseDto);
        assertEquals("Success", responseDto.getStatus());
        assertNotNull(responseDto.getTxnId());
    }

    @Test
    void initiateOtp_InvalidAadhaar_ThrowsException() {
        // Arrange
        OtpInitiateRequestDto requestDto = new OtpInitiateRequestDto();
        requestDto.setUid("999999999999"); // Non-existent Aadhaar

        // Act & Assert
        assertThrows(AadhaarNotFoundException.class, () -> mockUidaiService.initiateOtp(requestDto));
    }

    @Test
    void validateOtp_ValidOtp_ReturnsSuccessResponse() {
        // Arrange
        // First initiate OTP
        OtpInitiateRequestDto initiateRequestDto = new OtpInitiateRequestDto();
        initiateRequestDto.setUid("123456789012");
        OtpInitiateResponseDto initiateResponseDto = mockUidaiService.initiateOtp(initiateRequestDto);

        // Then validate OTP
        OtpValidateRequestDto validateRequestDto = new OtpValidateRequestDto();
        validateRequestDto.setTxnId(initiateResponseDto.getTxnId());
        validateRequestDto.setOtp("123456"); // The test repository always returns this OTP

        // Act
        OtpValidateResponseDto validateResponseDto = mockUidaiService.validateOtp(validateRequestDto);

        // Assert
        assertNotNull(validateResponseDto);
        assertEquals("Success", validateResponseDto.getStatus());
        assertNotNull(validateResponseDto.getEKycXML());
    }

    @Test
    void validateOtp_InvalidOtp_ThrowsException() {
        // Arrange
        // First initiate OTP
        OtpInitiateRequestDto initiateRequestDto = new OtpInitiateRequestDto();
        initiateRequestDto.setUid("123456789012");
        OtpInitiateResponseDto initiateResponseDto = mockUidaiService.initiateOtp(initiateRequestDto);

        // Then validate with wrong OTP
        OtpValidateRequestDto validateRequestDto = new OtpValidateRequestDto();
        validateRequestDto.setTxnId(initiateResponseDto.getTxnId());
        validateRequestDto.setOtp("999999"); // Wrong OTP

        // Act & Assert
        assertThrows(InvalidOtpException.class, () -> mockUidaiService.validateOtp(validateRequestDto));
    }

    @Test
    void validateOtp_ExpiredOtp_ThrowsException() {
        // Arrange
        // Create an expired OTP transaction
        String txnId = "expired-txn-123";
        OtpTransaction expiredTransaction = new OtpTransaction();
        expiredTransaction.setId(txnId);
        expiredTransaction.setAadhaarNumber("123456789012");
        expiredTransaction.setOtp("123456");
        expiredTransaction.setCreatedAt(LocalDateTime.now().minusHours(2)); // 2 hours old (expired)
        ((TestOtpTransactionRepository) otpTransactionRepository).saveExpiredTransaction(expiredTransaction);

        // Validate with expired transaction
        OtpValidateRequestDto validateRequestDto = new OtpValidateRequestDto();
        validateRequestDto.setTxnId(txnId);
        validateRequestDto.setOtp("123456");

        // Act & Assert
        assertThrows(OtpExpiredException.class, () -> mockUidaiService.validateOtp(validateRequestDto));
    }

    @Test
    void getKycData_ValidAadhaar_ReturnsKycData() {
        // Arrange
        String aadhaarNumber = "123456789012";

        // Act
        KycDataDto kycDataDto = mockUidaiService.getKycData(aadhaarNumber);

        // Assert
        assertNotNull(kycDataDto);
        assertEquals(aadhaarNumber, kycDataDto.getUid());
        assertNotNull(kycDataDto.getName());
        assertNotNull(kycDataDto.getDob());
        assertNotNull(kycDataDto.getGender());
        assertNotNull(kycDataDto.getAddress());
        assertNotNull(kycDataDto.getPhotoBase64());
    }

    @Test
    void getKycData_InvalidAadhaar_ThrowsException() {
        // Arrange
        String aadhaarNumber = "999999999999"; // Non-existent Aadhaar

        // Act & Assert
        assertThrows(AadhaarNotFoundException.class, () -> mockUidaiService.getKycData(aadhaarNumber));
    }

    @Test
    void generateDummyPhoto_ReturnsBase64EncodedString() {
        // Act
        String photoBase64 = mockUidaiService.generateDummyPhoto();

        // Assert
        assertNotNull(photoBase64);
        assertFalse(photoBase64.isEmpty());
        // Verify it's a valid Base64 string
        assertTrue(photoBase64.matches("^[A-Za-z0-9+/]+={0,2}$"));
    }

    // Test implementation of AadhaarDataRepository
    private static class TestAadhaarDataRepository implements AadhaarDataRepository {
        @Override
        public Optional<AadhaarData> findByAadhaarNumber(String aadhaarNumber) {
            if ("123456789012".equals(aadhaarNumber)) {
                AadhaarData aadhaarData = new AadhaarData();
                aadhaarData.setAadhaarNumber(aadhaarNumber);
                aadhaarData.setName("Test User");
                aadhaarData.setDob("1990-01-01");
                aadhaarData.setGender("M");
                aadhaarData.setAddress("123 Test Street, Test City, Test State - 123456");
                aadhaarData.setPhoneNumber("9876543210");
                aadhaarData.setEmail("test@example.com");
                return Optional.of(aadhaarData);
            }
            return Optional.empty();
        }

        @Override
        public <S extends AadhaarData> S save(S entity) {
            return entity;
        }

        @Override
        public <S extends AadhaarData> Iterable<S> saveAll(Iterable<S> entities) {
            return entities;
        }

        @Override
        public Optional<AadhaarData> findById(Long aLong) {
            return Optional.empty();
        }

        @Override
        public boolean existsById(Long aLong) {
            return false;
        }

        @Override
        public Iterable<AadhaarData> findAll() {
            return null;
        }

        @Override
        public Iterable<AadhaarData> findAllById(Iterable<Long> longs) {
            return null;
        }

        @Override
        public long count() {
            return 0;
        }

        @Override
        public void deleteById(Long aLong) {
            // No implementation needed for tests
        }

        @Override
        public void delete(AadhaarData entity) {
            // No implementation needed for tests
        }

        @Override
        public void deleteAllById(Iterable<? extends Long> longs) {
            // No implementation needed for tests
        }

        @Override
        public void deleteAll(Iterable<? extends AadhaarData> entities) {
            // No implementation needed for tests
        }

        @Override
        public void deleteAll() {
            // No implementation needed for tests
        }
    }

    // Test implementation of OtpTransactionRepository
    private static class TestOtpTransactionRepository implements OtpTransactionRepository {
        private OtpTransaction expiredTransaction;

        @Override
        public Optional<OtpTransaction> findById(String id) {
            if (expiredTransaction != null && expiredTransaction.getId().equals(id)) {
                return Optional.of(expiredTransaction);
            }

            if (id != null && !id.isEmpty()) {
                OtpTransaction transaction = new OtpTransaction();
                transaction.setId(id);
                transaction.setAadhaarNumber("123456789012");
                transaction.setOtp("123456");
                transaction.setCreatedAt(LocalDateTime.now());
                return Optional.of(transaction);
            }
            return Optional.empty();
        }

        public void saveExpiredTransaction(OtpTransaction transaction) {
            this.expiredTransaction = transaction;
        }

        @Override
        public <S extends OtpTransaction> S save(S entity) {
            return entity;
        }

        @Override
        public <S extends OtpTransaction> Iterable<S> saveAll(Iterable<S> entities) {
            return entities;
        }

        @Override
        public boolean existsById(String s) {
            return false;
        }

        @Override
        public Iterable<OtpTransaction> findAll() {
            return null;
        }

        @Override
        public Iterable<OtpTransaction> findAllById(Iterable<String> strings) {
            return null;
        }

        @Override
        public long count() {
            return 0;
        }

        @Override
        public void deleteById(String s) {
            // No implementation needed for tests
        }

        @Override
        public void delete(OtpTransaction entity) {
            // No implementation needed for tests
        }

        @Override
        public void deleteAllById(Iterable<? extends String> strings) {
            // No implementation needed for tests
        }

        @Override
        public void deleteAll(Iterable<? extends OtpTransaction> entities) {
            // No implementation needed for tests
        }

        @Override
        public void deleteAll() {
            // No implementation needed for tests
        }
    }
}