package com.ekyc.service.controller;

import com.ekyc.service.dto.EkycRequestDto;
import com.ekyc.service.dto.EkycResponseDto;
import com.ekyc.service.enums.VerificationStatus;
import com.ekyc.service.service.EkycService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(EkycController.class)
public class EkycControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private EkycService ekycService;

    @Autowired
    private ObjectMapper objectMapper;

    private EkycRequestDto validRequestDto;
    private EkycResponseDto responseDto;
    private String verificationId;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Initialize test data
        verificationId = UUID.randomUUID().toString();

        validRequestDto = new EkycRequestDto();
        validRequestDto.setFirstName("John");
        validRequestDto.setLastName("Doe");
        validRequestDto.setDateOfBirth("1990-01-01");
        validRequestDto.setIdNumber("AB123456");
        validRequestDto.setIdType("PASSPORT");
        validRequestDto.setCountry("USA");
        validRequestDto.setEmail("john.doe@example.com");
        validRequestDto.setPhoneNumber("+1234567890");

        responseDto = new EkycResponseDto();
        responseDto.setVerificationId(verificationId);
        responseDto.setStatus(VerificationStatus.PENDING);
        responseDto.setCreatedAt(LocalDateTime.now());
        responseDto.setUpdatedAt(LocalDateTime.now());
        responseDto.setMessage("Verification submitted successfully");
    }

    @Test
    @DisplayName("Should submit verification successfully")
    public void testSubmitVerification() throws Exception {
        given(ekycService.submitVerification(any(EkycRequestDto.class))).willReturn(responseDto);

        mockMvc.perform(post("/api/v1/ekyc/verifications")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequestDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.verificationId", is(verificationId)))
        .andExpect(jsonPath("$.status", is(VerificationStatus.PENDING.toString())))
        .andExpect(jsonPath("$.message", is("Verification submitted successfully")));

        verify(ekycService, times(1)).submitVerification(any(EkycRequestDto.class));
    }

    @Test
    @DisplayName("Should return bad request for invalid verification submission")
    public void testSubmitVerificationInvalidRequest() throws Exception {
        EkycRequestDto invalidRequest = new EkycRequestDto();
        // Missing required fields

        mockMvc.perform(post("/api/v1/ekyc/verifications")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());

        verify(ekycService, never()).submitVerification(any(EkycRequestDto.class));
    }

    @Test
    @DisplayName("Should handle service exception during verification submission")
    public void testSubmitVerificationServiceException() throws Exception {
        given(ekycService.submitVerification(any(EkycRequestDto.class)))
        .willThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/api/v1/ekyc/verifications")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequestDto)))
        .andExpect(status().isInternalServerError());

        verify(ekycService, times(1)).submitVerification(any(EkycRequestDto.class));
    }

    @Test
    @DisplayName("Should get verification status successfully")
    public void testGetVerificationStatus() throws Exception {
        given(ekycService.getVerificationStatus(verificationId)).willReturn(responseDto);

        mockMvc.perform(get("/api/v1/ekyc/verifications/{verificationId}", verificationId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verificationId", is(verificationId)))
        .andExpect(jsonPath("$.status", is(VerificationStatus.PENDING.toString())));

        verify(ekycService, times(1)).getVerificationStatus(verificationId);
    }

    @Test
    @DisplayName("Should return not found for non-existent verification ID")
    public void testGetVerificationStatusNotFound() throws Exception {
        String nonExistentId = "non-existent-id";
        given(ekycService.getVerificationStatus(nonExistentId))
        .willThrow(new IllegalArgumentException("Verification not found"));

        mockMvc.perform(get("/api/v1/ekyc/verifications/{verificationId}", nonExistentId))
        .andExpect(status().isNotFound());

        verify(ekycService, times(1)).getVerificationStatus(nonExistentId);
    }

    @Test
    @DisplayName("Should handle service exception during verification status retrieval")
    public void testGetVerificationStatusServiceException() throws Exception {
        given(ekycService.getVerificationStatus(verificationId))
        .willThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/api/v1/ekyc/verifications/{verificationId}", verificationId))
        .andExpect(status().isInternalServerError());

        verify(ekycService, times(1)).getVerificationStatus(verificationId);
    }

    @Test
    @DisplayName("Should get all verifications successfully")
    public void testGetAllVerifications() throws Exception {
        List<EkycResponseDto> verifications = Arrays.asList(responseDto);
        given(ekycService.getAllVerifications()).willReturn(verifications);

        mockMvc.perform(get("/api/v1/ekyc/verifications"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].verificationId", is(verificationId)));

        verify(ekycService, times(1)).getAllVerifications();
    }

    @Test
    @DisplayName("Should handle service exception during all verifications retrieval")
    public void testGetAllVerificationsServiceException() throws Exception {
        given(ekycService.getAllVerifications())
        .willThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/api/v1/ekyc/verifications"))
        .andExpect(status().isInternalServerError());

        verify(ekycService, times(1)).getAllVerifications();
    }

    @Test
    @DisplayName("Should resubmit verification successfully")
    public void testResubmitVerification() throws Exception {
        given(ekycService.resubmitVerification(eq(verificationId), any(EkycRequestDto.class)))
        .willReturn(responseDto);

        mockMvc.perform(put("/api/v1/ekyc/verifications/{verificationId}", verificationId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequestDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verificationId", is(verificationId)))
        .andExpect(jsonPath("$.status", is(VerificationStatus.PENDING.toString())));

        verify(ekycService, times(1)).resubmitVerification(eq(verificationId), any(EkycRequestDto.class));
    }

    @Test
    @DisplayName("Should return not found for resubmitting non-existent verification")
    public void testResubmitVerificationNotFound() throws Exception {
        String nonExistentId = "non-existent-id";
        given(ekycService.resubmitVerification(eq(nonExistentId), any(EkycRequestDto.class)))
        .willThrow(new IllegalArgumentException("Verification not found"));

        mockMvc.perform(put("/api/v1/ekyc/verifications/{verificationId}", nonExistentId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequestDto)))
        .andExpect(status().isNotFound());

        verify(ekycService, times(1)).resubmitVerification(eq(nonExistentId), any(EkycRequestDto.class));
    }

    @Test
    @DisplayName("Should return bad request for invalid resubmission state")
    public void testResubmitVerificationInvalidState() throws Exception {
        given(ekycService.resubmitVerification(eq(verificationId), any(EkycRequestDto.class)))
        .willThrow(new IllegalStateException("Cannot resubmit verification in current state"));

        mockMvc.perform(put("/api/v1/ekyc/verifications/{verificationId}", verificationId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequestDto)))
        .andExpect(status().isBadRequest());

        verify(ekycService, times(1)).resubmitVerification(eq(verificationId), any(EkycRequestDto.class));
    }

    @Test
    @DisplayName("Should handle service exception during verification resubmission")
    public void testResubmitVerificationServiceException() throws Exception {
        given(ekycService.resubmitVerification(eq(verificationId), any(EkycRequestDto.class)))
        .willThrow(new RuntimeException("Service error"));

        mockMvc.perform(put("/api/v1/ekyc/verifications/{verificationId}", verificationId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequestDto)))
        .andExpect(status().isInternalServerError());

        verify(ekycService, times(1)).resubmitVerification(eq(verificationId), any(EkycRequestDto.class));
    }
}