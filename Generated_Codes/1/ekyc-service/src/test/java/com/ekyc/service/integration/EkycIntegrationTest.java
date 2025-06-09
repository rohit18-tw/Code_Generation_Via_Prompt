package com.ekyc.service.integration;

import com.ekyc.service.dto.EkycRequestDto;
import com.ekyc.service.dto.EkycResponseDto;
import com.ekyc.service.dto.OtpVerificationDto;
import com.ekyc.service.entity.EkycVerification;
import com.ekyc.service.entity.VerificationStatus;
import com.ekyc.service.repository.EkycVerificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class EkycIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EkycVerificationRepository ekycVerificationRepository;

    private EkycRequestDto validEkycRequest;
    private EkycVerification existingVerification;

    @BeforeEach
    public void setup() {
        // Clean up any existing test data
        ekycVerificationRepository.deleteAll();

        // Setup valid eKYC request
        validEkycRequest = new EkycRequestDto();
        validEkycRequest.setFullName("John Doe");
        validEkycRequest.setDateOfBirth("1990-01-01");
        validEkycRequest.setIdNumber("ABC123456789");
        validEkycRequest.setIdType("PASSPORT");
        validEkycRequest.setPhoneNumber("+1234567890");
        validEkycRequest.setEmail("john.doe@example.com");
        validEkycRequest.setAddress("123 Main St, City, Country");

        // Create an existing verification for resubmission and OTP tests
        existingVerification = new EkycVerification();
        existingVerification.setFullName("Jane Smith");
        existingVerification.setDateOfBirth("1985-05-15");
        existingVerification.setIdNumber("XYZ987654321");
        existingVerification.setIdType("NATIONAL_ID");
        existingVerification.setPhoneNumber("+9876543210");
        existingVerification.setEmail("jane.smith@example.com");
        existingVerification.setAddress("456 Oak St, Town, Country");
        existingVerification.setStatus(VerificationStatus.PENDING);
        existingVerification.setCreatedAt(LocalDateTime.now());
        existingVerification.setOtpCode("123456");
        existingVerification.setOtpExpiryTime(LocalDateTime.now().plusMinutes(10));
        existingVerification = ekycVerificationRepository.save(existingVerification);
    }

    @Test
    public void testSubmitEkycVerification_Success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/ekyc/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validEkycRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verificationId").exists())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        EkycResponseDto response = objectMapper.readValue(responseContent, EkycResponseDto.class);

        // Verify the verification was saved in the database
        Optional<EkycVerification> savedVerification = ekycVerificationRepository.findById(UUID.fromString(response.getVerificationId()));
        assertTrue(savedVerification.isPresent());
        assertEquals(validEkycRequest.getFullName(), savedVerification.get().getFullName());
        assertEquals(validEkycRequest.getIdNumber(), savedVerification.get().getIdNumber());
        assertEquals(VerificationStatus.PENDING, savedVerification.get().getStatus());
        assertNotNull(savedVerification.get().getOtpCode());
        assertNotNull(savedVerification.get().getOtpExpiryTime());
    }

    @Test
    public void testSubmitEkycVerification_InvalidData() throws Exception {
        // Create invalid request with missing required fields
        EkycRequestDto invalidRequest = new EkycRequestDto();
        invalidRequest.setFullName(""); // Empty name
        invalidRequest.setIdNumber("ABC123"); // Too short ID

        mockMvc.perform(post("/api/v1/ekyc/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    public void testGetVerificationStatus_Success() throws Exception {
        String verificationId = existingVerification.getId().toString();

        mockMvc.perform(get("/api/v1/ekyc/status/{verificationId}", verificationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verificationId").value(verificationId))
        .andExpect(jsonPath("$.status").value(existingVerification.getStatus().toString()));
    }

    @Test
    public void testGetVerificationStatus_NotFound() throws Exception {
        String nonExistentId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/ekyc/status/{verificationId}", nonExistentId))
        .andExpect(status().isNotFound());
    }

    @Test
    public void testResubmitVerification_Success() throws Exception {
        // First, set the existing verification to REJECTED
        existingVerification.setStatus(VerificationStatus.REJECTED);
        existingVerification.setRejectionReason("Document unclear");
        ekycVerificationRepository.save(existingVerification);

        // Create updated request
        EkycRequestDto updatedRequest = new EkycRequestDto();
        updatedRequest.setFullName(existingVerification.getFullName());
        updatedRequest.setDateOfBirth(existingVerification.getDateOfBirth());
        updatedRequest.setIdNumber(existingVerification.getIdNumber());
        updatedRequest.setIdType(existingVerification.getIdType());
        updatedRequest.setPhoneNumber(existingVerification.getPhoneNumber());
        updatedRequest.setEmail(existingVerification.getEmail());
        updatedRequest.setAddress("New Address, City, Country"); // Updated address

        String verificationId = existingVerification.getId().toString();

        mockMvc.perform(put("/api/v1/ekyc/resubmit/{verificationId}", verificationId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updatedRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verificationId").value(verificationId))
        .andExpect(jsonPath("$.status").value("PENDING"));

        // Verify the verification was updated in the database
        Optional<EkycVerification> updatedVerification = ekycVerificationRepository.findById(existingVerification.getId());
        assertTrue(updatedVerification.isPresent());
        assertEquals("New Address, City, Country", updatedVerification.get().getAddress());
        assertEquals(VerificationStatus.PENDING, updatedVerification.get().getStatus());
        assertNull(updatedVerification.get().getRejectionReason());
    }

    @Test
    public void testResubmitVerification_NotRejected() throws Exception {
        // Verification is in PENDING state, not REJECTED
        String verificationId = existingVerification.getId().toString();

        mockMvc.perform(put("/api/v1/ekyc/resubmit/{verificationId}", verificationId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validEkycRequest)))
        .andExpect(status().isBadRequest());
    }

    @Test
    public void testVerifyOtp_Success() throws Exception {
        String verificationId = existingVerification.getId().toString();
        String otpCode = existingVerification.getOtpCode();

        OtpVerificationDto otpVerificationDto = new OtpVerificationDto();
        otpVerificationDto.setVerificationId(verificationId);
        otpVerificationDto.setOtpCode(otpCode);

        mockMvc.perform(post("/api/v1/otp/verify")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(otpVerificationDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("OTP verification successful"));

        // Verify the verification status was updated to VERIFIED
        Optional<EkycVerification> verifiedRecord = ekycVerificationRepository.findById(existingVerification.getId());
        assertTrue(verifiedRecord.isPresent());
        assertEquals(VerificationStatus.VERIFIED, verifiedRecord.get().getStatus());
    }

    @Test
    public void testVerifyOtp_InvalidOtp() throws Exception {
        String verificationId = existingVerification.getId().toString();
        String wrongOtpCode = "999999"; // Wrong OTP

        OtpVerificationDto otpVerificationDto = new OtpVerificationDto();
        otpVerificationDto.setVerificationId(verificationId);
        otpVerificationDto.setOtpCode(wrongOtpCode);

        mockMvc.perform(post("/api/v1/otp/verify")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(otpVerificationDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Invalid OTP code"));

        // Verify the verification status remains PENDING
        Optional<EkycVerification> record = ekycVerificationRepository.findById(existingVerification.getId());
        assertTrue(record.isPresent());
        assertEquals(VerificationStatus.PENDING, record.get().getStatus());
    }

    @Test
    public void testResendOtp_Success() throws Exception {
        String verificationId = existingVerification.getId().toString();
        String originalOtp = existingVerification.getOtpCode();

        mockMvc.perform(post("/api/v1/otp/resend/{verificationId}", verificationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("OTP resent successfully"));

        // Verify a new OTP was generated
        Optional<EkycVerification> updatedRecord = ekycVerificationRepository.findById(existingVerification.getId());
        assertTrue(updatedRecord.isPresent());
        assertNotEquals(originalOtp, updatedRecord.get().getOtpCode());
        assertTrue(updatedRecord.get().getOtpExpiryTime().isAfter(LocalDateTime.now()));
    }

    @Test
    public void testResendOtp_VerificationNotFound() throws Exception {
        String nonExistentId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/otp/resend/{verificationId}", nonExistentId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.message").value("Verification not found"));
    }

    @Test
    public void testEndToEndEkycFlow() throws Exception {
        // Step 1: Submit eKYC verification
        MvcResult submitResult = mockMvc.perform(post("/api/v1/ekyc/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validEkycRequest)))
        .andExpect(status().isOk())
        .andReturn();

        String responseContent = submitResult.getResponse().getContentAsString();
        EkycResponseDto submitResponse = objectMapper.readValue(responseContent, EkycResponseDto.class);
        String verificationId = submitResponse.getVerificationId();

        // Step 2: Check verification status
        mockMvc.perform(get("/api/v1/ekyc/status/{verificationId}", verificationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"));

        // Step 3: Get the OTP from the database (in a real scenario, this would be sent to the user)
        Optional<EkycVerification> verification = ekycVerificationRepository.findById(UUID.fromString(verificationId));
        assertTrue(verification.isPresent());
        String otpCode = verification.get().getOtpCode();

        // Step 4: Verify OTP
        OtpVerificationDto otpVerificationDto = new OtpVerificationDto();
        otpVerificationDto.setVerificationId(verificationId);
        otpVerificationDto.setOtpCode(otpCode);

        mockMvc.perform(post("/api/v1/otp/verify")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(otpVerificationDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

        // Step 5: Check final verification status
        mockMvc.perform(get("/api/v1/ekyc/status/{verificationId}", verificationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("VERIFIED"));
    }
}