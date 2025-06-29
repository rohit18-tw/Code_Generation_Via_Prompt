package com.mock.uidai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.uidai.dto.KycDataDto;
import com.mock.uidai.dto.OtpInitiateRequestDto;
import com.mock.uidai.dto.OtpInitiateResponseDto;
import com.mock.uidai.dto.OtpValidateRequestDto;
import com.mock.uidai.dto.OtpValidateResponseDto;
import com.mock.uidai.service.MockUidaiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(MockUidaiController.class)
public class MockUidaiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockUidaiService mockUidaiService;

    private OtpInitiateRequestDto otpInitiateRequestDto;
    private OtpValidateRequestDto otpValidateRequestDto;
    private String validAadhaarNumber;
    private String validOtp;
    private String validTxnId;

    @TestConfiguration
    static class MockUidaiServiceTestConfig {
        @Bean
        public MockUidaiService mockUidaiService() {
            return new MockUidaiService() {
                @Override
                public OtpInitiateResponseDto initiateOtp(OtpInitiateRequestDto requestDto) {
                    OtpInitiateResponseDto responseDto = new OtpInitiateResponseDto();
                    responseDto.setStatus("Success");
                    responseDto.setTxnId(UUID.randomUUID().toString());
                    responseDto.setMessage("OTP sent successfully");
                    return responseDto;
                }

                @Override
                public OtpValidateResponseDto validateOtp(OtpValidateRequestDto requestDto) {
                    OtpValidateResponseDto responseDto = new OtpValidateResponseDto();
                    responseDto.setStatus("Success");
                    responseDto.setMessage("OTP validation successful");
                    return responseDto;
                }

                @Override
                public KycDataDto getKycData(String aadhaarNumber) {
                    KycDataDto kycDataDto = new KycDataDto();
                    kycDataDto.setName("Test User");
                    kycDataDto.setDob("1990-01-01");
                    kycDataDto.setGender("M");
                    kycDataDto.setAddress("123 Test Street, Test City, Test State - 123456");
                    kycDataDto.setEmail("test@example.com");
                    kycDataDto.setMobile("9999999999");
                    kycDataDto.setPhoto(generateSamplePhoto());
                    return kycDataDto;
                }
            };
        }
    }

    @BeforeEach
    public void setup() {
        validAadhaarNumber = "123456789012";
        validOtp = "123456";
        validTxnId = UUID.randomUUID().toString();

        otpInitiateRequestDto = new OtpInitiateRequestDto();
        otpInitiateRequestDto.setAadhaarNumber(validAadhaarNumber);

        otpValidateRequestDto = new OtpValidateRequestDto();
        otpValidateRequestDto.setAadhaarNumber(validAadhaarNumber);
        otpValidateRequestDto.setOtp(validOtp);
        otpValidateRequestDto.setTxnId(validTxnId);
    }

    @Test
    @DisplayName("Should initiate OTP successfully")
    public void testInitiateOtp() throws Exception {
        String requestJson = objectMapper.writeValueAsString(otpInitiateRequestDto);

        MvcResult result = mockMvc.perform(post("/api/v1/uidai/otp/initiate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Success"))
        .andExpect(jsonPath("$.txnId").exists())
        .andExpect(jsonPath("$.message").value("OTP sent successfully"))
        .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        OtpInitiateResponseDto responseDto = objectMapper.readValue(responseJson, OtpInitiateResponseDto.class);

        assertNotNull(responseDto.getTxnId());
        assertEquals("Success", responseDto.getStatus());
    }

    @Test
    @DisplayName("Should return bad request for invalid Aadhaar number during OTP initiation")
    public void testInitiateOtpWithInvalidAadhaar() throws Exception {
        otpInitiateRequestDto.setAadhaarNumber("12345"); // Invalid Aadhaar number (too short)

        String requestJson = objectMapper.writeValueAsString(otpInitiateRequestDto);

        mockMvc.perform(post("/api/v1/uidai/otp/initiate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestJson))
        .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate OTP successfully")
    public void testValidateOtp() throws Exception {
        String requestJson = objectMapper.writeValueAsString(otpValidateRequestDto);

        MvcResult result = mockMvc.perform(post("/api/v1/uidai/otp/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Success"))
        .andExpect(jsonPath("$.message").value("OTP validation successful"))
        .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        OtpValidateResponseDto responseDto = objectMapper.readValue(responseJson, OtpValidateResponseDto.class);

        assertEquals("Success", responseDto.getStatus());
        assertEquals("OTP validation successful", responseDto.getMessage());
    }

    @Test
    @DisplayName("Should return bad request for invalid OTP during validation")
    public void testValidateOtpWithInvalidOtp() throws Exception {
        otpValidateRequestDto.setOtp("12345"); // Invalid OTP (too short)

        String requestJson = objectMapper.writeValueAsString(otpValidateRequestDto);

        mockMvc.perform(post("/api/v1/uidai/otp/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestJson))
        .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return KYC data successfully")
    public void testGetKycData() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/uidai/kyc")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"aadhaarNumber\":\"" + validAadhaarNumber + "\", \"txnId\":\"" + validTxnId + "\"}"))
        .andExpect(status().isOk())
        .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        KycDataDto kycDataDto = objectMapper.readValue(responseJson, KycDataDto.class);

        assertNotNull(kycDataDto);
        assertEquals("Test User", kycDataDto.getName());
        assertEquals("1990-01-01", kycDataDto.getDob());
        assertEquals("M", kycDataDto.getGender());
        assertTrue(kycDataDto.getAddress().contains("Test Street"));
        assertEquals("test@example.com", kycDataDto.getEmail());
        assertEquals("9999999999", kycDataDto.getMobile());
        assertNotNull(kycDataDto.getPhoto());
    }

    @Test
    @DisplayName("Should mask Aadhaar number correctly")
    public void testMaskAadhaarNumber() throws Exception {
        // Test with valid Aadhaar number
        MvcResult result = mockMvc.perform(post("/api/v1/uidai/mask-aadhaar")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"aadhaarNumber\":\"" + validAadhaarNumber + "\"}"))
        .andExpect(status().isOk())
        .andReturn();

        String maskedAadhaar = result.getResponse().getContentAsString();
        assertEquals("XXXX-XXXX-9012", maskedAadhaar);

        // Test with null Aadhaar number
        result = mockMvc.perform(post("/api/v1/uidai/mask-aadhaar")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"aadhaarNumber\":null}"))
        .andExpect(status().isOk())
        .andReturn();

        maskedAadhaar = result.getResponse().getContentAsString();
        assertEquals("****", maskedAadhaar);
    }
}