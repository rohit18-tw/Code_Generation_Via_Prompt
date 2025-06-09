package com.mock.uidai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Service for trace logging with PII (Personally Identifiable Information) masking.
* This service provides methods to mask sensitive information in logs to comply with
* privacy regulations and security best practices.
*/
@Service
public class TraceLoggerService {

    private static final Logger logger = LoggerFactory.getLogger(TraceLoggerService.class);

    // Set of fields that should be masked in logs
    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
    "aadhaar", "uid", "name", "dob", "gender", "phone", "email", "address",
    "pincode", "photo", "biometric", "fingerprint", "iris", "face",
    "password", "otp", "pan", "accountNumber", "cardNumber"
    ));

    // Patterns for common PII data formats
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\b[2-9]{1}[0-9]{11}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{10}\\b");
    private static final Pattern PAN_PATTERN = Pattern.compile("\\b[A-Z]{5}[0-9]{4}[A-Z]{1}\\b");

    /**
    * Logs a message with INFO level after masking any PII data.
    *
    * @param message The message to be logged
    */
    public void info(String message) {
        if (message == null) {
            return;
        }
        logger.info(maskSensitiveData(message));
    }

    /**
    * Logs a message with DEBUG level after masking any PII data.
    *
    * @param message The message to be logged
    */
    public void debug(String message) {
        if (message == null) {
            return;
        }
        logger.debug(maskSensitiveData(message));
    }

    /**
    * Logs a message with ERROR level after masking any PII data.
    *
    * @param message The message to be logged
    */
    public void error(String message) {
        if (message == null) {
            return;
        }
        logger.error(maskSensitiveData(message));
    }

    /**
    * Logs a message with ERROR level after masking any PII data, including the exception stack trace.
    *
    * @param message The message to be logged
    * @param throwable The exception to be logged
    */
    public void error(String message, Throwable throwable) {
        if (message == null) {
            return;
        }
        logger.error(maskSensitiveData(message), throwable);
    }

    /**
    * Logs a message with WARN level after masking any PII data.
    *
    * @param message The message to be logged
    */
    public void warn(String message) {
        if (message == null) {
            return;
        }
        logger.warn(maskSensitiveData(message));
    }

    /**
    * Masks sensitive data in the provided string.
    * This includes JSON fields, URL parameters, and common PII patterns.
    *
    * @param data The string that may contain sensitive information
    * @return The string with sensitive information masked
    */
    public String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        String maskedData = data;

        // Mask JSON fields
        maskedData = maskJsonFields(maskedData);

        // Mask URL parameters
        maskedData = maskUrlParameters(maskedData);

        // Mask common PII patterns
        maskedData = maskPatterns(maskedData);

        return maskedData;
    }

    /**
    * Masks sensitive JSON fields in the provided string.
    * Looks for patterns like "field":"value" or "field": "value" and masks the values
    * for fields that are in the SENSITIVE_FIELDS set.
    *
    * @param data The string that may contain JSON with sensitive fields
    * @return The string with sensitive JSON field values masked
    */
    private String maskJsonFields(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        String maskedData = data;

        for (String field : SENSITIVE_FIELDS) {
            // Match JSON field patterns with various formats and whitespace
            String regex = "\"" + field + "\"\\s*:\\s*\"([^\"]+)\"";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(maskedData);

            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String value = matcher.group(1);
                String maskedValue = maskValue(value);
                matcher.appendReplacement(sb, "\"" + field + "\":\"" + maskedValue + "\"");
            }
            matcher.appendTail(sb);
            maskedData = sb.toString();

            // Also match numeric values without quotes
            regex = "\"" + field + "\"\\s*:\\s*(\\d+)";
            pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            matcher = pattern.matcher(maskedData);

            sb = new StringBuffer();
            while (matcher.find()) {
                String value = matcher.group(1);
                String maskedValue = maskValue(value);
                matcher.appendReplacement(sb, "\"" + field + "\":" + maskedValue);
            }
            matcher.appendTail(sb);
            maskedData = sb.toString();
        }

        return maskedData;
    }

    /**
    * Masks sensitive URL parameters in the provided string.
    * Looks for patterns like field=value in URLs and masks the values
    * for parameters that are in the SENSITIVE_FIELDS set.
    *
    * @param data The string that may contain URL parameters with sensitive values
    * @return The string with sensitive URL parameter values masked
    */
    private String maskUrlParameters(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        String maskedData = data;

        for (String field : SENSITIVE_FIELDS) {
            // Match URL parameter patterns
            String regex = "\\b" + field + "=([^&\\s]+)";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(maskedData);

            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String value = matcher.group(1);
                String maskedValue = maskValue(value);
                matcher.appendReplacement(sb, field + "=" + maskedValue);
            }
            matcher.appendTail(sb);
            maskedData = sb.toString();
        }

        return maskedData;
    }

    /**
    * Masks common PII patterns in the provided string regardless of context.
    * This includes Aadhaar numbers, email addresses, phone numbers, and PAN numbers.
    *
    * @param data The string that may contain PII patterns
    * @return The string with PII patterns masked
    */
    private String maskPatterns(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        String maskedData = data;

        // Mask Aadhaar numbers
        maskedData = AADHAAR_PATTERN.matcher(maskedData).replaceAll(match -> maskValue(match.group()));

        // Mask email addresses
        maskedData = EMAIL_PATTERN.matcher(maskedData).replaceAll(match -> maskEmail(match.group()));

        // Mask phone numbers
        maskedData = PHONE_PATTERN.matcher(maskedData).replaceAll(match -> maskValue(match.group()));

        // Mask PAN numbers
        maskedData = PAN_PATTERN.matcher(maskedData).replaceAll(match -> maskValue(match.group()));

        return maskedData;
    }

    /**
    * Masks a value by showing only the first and last characters and replacing the rest with asterisks.
    * For short values (less than 4 characters), all characters are masked.
    *
    * @param value The value to be masked
    * @return The masked value
    */
    private String maskValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        if (value.length() <= 4) {
            return "****";
        }

        return value.charAt(0) +
        "*".repeat(value.length() - 2) +
        value.charAt(value.length() - 1);
    }

    /**
    * Specially masks email addresses by showing the first and last character of the username
    * and the domain name, but masking everything else.
    *
    * @param email The email address to be masked
    * @return The masked email address
    */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty() || !email.contains("@")) {
            return maskValue(email);
        }

        String[] parts = email.split("@", 2);
        String username = parts[0];
        String domain = parts[1];

        String maskedUsername = maskValue(username);

        return maskedUsername + "@" + domain;
    }
}