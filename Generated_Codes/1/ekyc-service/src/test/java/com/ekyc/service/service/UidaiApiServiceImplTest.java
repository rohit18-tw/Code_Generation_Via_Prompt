package com.ekyc.service.service;

import com.ekyc.service.dto.UidaiOtpInitiateRequestDto;
import com.ekyc.service.dto.UidaiOtpInitiateResponseDto;
import com.ekyc.service.dto.UidaiOtpVerifyRequestDto;
import com.ekyc.service.dto.UidaiOtpVerifyResponseDto;
import com.ekyc.service.exception.EkycException;
import com.ekyc.service.service.impl.UidaiApiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class UidaiApiServiceImplTest {

    private UidaiApiServiceImpl uidaiApiService;
    private WebClient webClientMock;

    @BeforeEach
    public void setUp() {
        uidaiApiService = new UidaiApiServiceImpl();
        webClientMock = createWebClientMock();
        ReflectionTestUtils.setField(uidaiApiService, "webClient", webClientMock);
        ReflectionTestUtils.setField(uidaiApiService, "uidaiBaseUrl", "https://api.uidai.gov.in");
        ReflectionTestUtils.setField(uidaiApiService, "otpInitiateEndpoint", "/otp/initiate");
        ReflectionTestUtils.setField(uidaiApiService, "otpVerifyEndpoint", "/otp/verify");
    }

    @Test
    @DisplayName("Should successfully initiate OTP")
    public void testInitiateOtpSuccess() {
        // Arrange
        UidaiOtpInitiateRequestDto requestDto = new UidaiOtpInitiateRequestDto();
        requestDto.setAadhaarNumber("123456789012");
        requestDto.setTransactionId("txn123");

        UidaiOtpInitiateResponseDto expectedResponse = new UidaiOtpInitiateResponseDto();
        expectedResponse.setStatus("Success");
        expectedResponse.setMessage("OTP sent successfully");
        expectedResponse.setTransactionId("txn123");

        // Act
        UidaiOtpInitiateResponseDto actualResponse = uidaiApiService.initiateOtp(requestDto);

        // Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getStatus(), actualResponse.getStatus());
        assertEquals(expectedResponse.getMessage(), actualResponse.getMessage());
        assertEquals(expectedResponse.getTransactionId(), actualResponse.getTransactionId());
    }

    @Test
    @DisplayName("Should handle exception when initiating OTP")
    public void testInitiateOtpException() {
        // Arrange
        UidaiOtpInitiateRequestDto requestDto = new UidaiOtpInitiateRequestDto();
        requestDto.setAadhaarNumber("999999999999"); // Trigger exception
        requestDto.setTransactionId("txn123");

        // Act & Assert
        EkycException exception = assertThrows(EkycException.class, () -> {
            uidaiApiService.initiateOtp(requestDto);
        });

        assertEquals("Failed to initiate OTP with UIDAI", exception.getMessage());
    }

    @Test
    @DisplayName("Should successfully verify OTP")
    public void testVerifyOtpSuccess() {
        // Arrange
        UidaiOtpVerifyRequestDto requestDto = new UidaiOtpVerifyRequestDto();
        requestDto.setAadhaarNumber("123456789012");
        requestDto.setTransactionId("txn123");
        requestDto.setOtp("123456");

        UidaiOtpVerifyResponseDto expectedResponse = new UidaiOtpVerifyResponseDto();
        expectedResponse.setStatus("Success");
        expectedResponse.setMessage("OTP verified successfully");
        expectedResponse.setTransactionId("txn123");
        expectedResponse.setEkycXml("<xml>Sample eKYC data</xml>");

        // Act
        UidaiOtpVerifyResponseDto actualResponse = uidaiApiService.verifyOtp(requestDto);

        // Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getStatus(), actualResponse.getStatus());
        assertEquals(expectedResponse.getMessage(), actualResponse.getMessage());
        assertEquals(expectedResponse.getTransactionId(), actualResponse.getTransactionId());
        assertEquals(expectedResponse.getEkycXml(), actualResponse.getEkycXml());
    }

    @Test
    @DisplayName("Should handle exception when verifying OTP")
    public void testVerifyOtpException() {
        // Arrange
        UidaiOtpVerifyRequestDto requestDto = new UidaiOtpVerifyRequestDto();
        requestDto.setAadhaarNumber("999999999999"); // Trigger exception
        requestDto.setTransactionId("txn123");
        requestDto.setOtp("123456");

        // Act & Assert
        EkycException exception = assertThrows(EkycException.class, () -> {
            uidaiApiService.verifyOtp(requestDto);
        });

        assertEquals("Failed to verify OTP with UIDAI", exception.getMessage());
    }

    @Test
    @DisplayName("Should mask Aadhaar number correctly")
    public void testMaskAadhaarNumber() {
        // Arrange
        String aadhaarNumber = "123456789012";
        String expectedMaskedAadhaar = "XXXX-XXXX-9012";

        // Act
        String maskedAadhaar = ReflectionTestUtils.invokeMethod(uidaiApiService, "maskAadhaarNumber", aadhaarNumber);

        // Assert
        assertEquals(expectedMaskedAadhaar, maskedAadhaar);
    }

    @Test
    @DisplayName("Should handle null Aadhaar number when masking")
    public void testMaskAadhaarNumberWithNull() {
        // Act
        String maskedAadhaar = ReflectionTestUtils.invokeMethod(uidaiApiService, "maskAadhaarNumber", (String)null);

        // Assert
        assertNull(maskedAadhaar);
    }

    @Test
    @DisplayName("Should handle short Aadhaar number when masking")
    public void testMaskAadhaarNumberWithShortInput() {
        // Arrange
        String shortAadhaar = "1234";

        // Act
        String maskedAadhaar = ReflectionTestUtils.invokeMethod(uidaiApiService, "maskAadhaarNumber", shortAadhaar);

        // Assert
        assertEquals(shortAadhaar, maskedAadhaar);
    }

    /**
    * Creates a mock WebClient that returns predefined responses based on input
    */
    private WebClient createWebClientMock() {
        return WebClient.builder()
        .exchangeFunction(clientRequest -> {
            String path = clientRequest.url().getPath();

            if (path.contains("/otp/initiate")) {
                String requestBody = new String(clientRequest.body().toString());
                if (requestBody.contains("999999999999")) {
                    return Mono.error(new WebClientResponseException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    null, null, null));
                }

                UidaiOtpInitiateResponseDto response = new UidaiOtpInitiateResponseDto();
                response.setStatus("Success");
                response.setMessage("OTP sent successfully");
                response.setTransactionId("txn123");

                return Mono.just(ResponseEntity.ok(response))
                .map(r -> (Object) r)
                .cast(Object.class);
            } else if (path.contains("/otp/verify")) {
                String requestBody = new String(clientRequest.body().toString());
                if (requestBody.contains("999999999999")) {
                    return Mono.error(new WebClientResponseException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    null, null, null));
                }

                UidaiOtpVerifyResponseDto response = new UidaiOtpVerifyResponseDto();
                response.setStatus("Success");
                response.setMessage("OTP verified successfully");
                response.setTransactionId("txn123");
                response.setEkycXml("<xml>Sample eKYC data</xml>");

                return Mono.just(ResponseEntity.ok(response))
                .map(r -> (Object) r)
                .cast(Object.class);
            }

            return Mono.error(new IllegalArgumentException("Unexpected URL: " + clientRequest.url()));
        })
        .build();
    }
}