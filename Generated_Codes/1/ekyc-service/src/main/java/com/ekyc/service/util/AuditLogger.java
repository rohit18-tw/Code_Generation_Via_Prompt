package com.ekyc.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
* Utility class for audit-ready logging with PII masking capabilities.
* This class provides methods to log messages with sensitive information masked
* for compliance with data protection regulations.
*/
@Component
public class AuditLogger {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogger.class);

    // Patterns for identifying PII data
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{10,12}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("\\b[A-Z]{1,2}[0-9]{6,9}\\b");

    // Map to store patterns and their replacement strategies
    private static final Map<Pattern, String> MASKING_PATTERNS = new HashMap<>();

    static {
        MASKING_PATTERNS.put(EMAIL_PATTERN, email -> maskEmail(email));
        MASKING_PATTERNS.put(PHONE_PATTERN, phone -> "***-***-" + phone.substring(Math.max(0, phone.length() - 4)));
        MASKING_PATTERNS.put(SSN_PATTERN, ssn -> "***-**-" + ssn.substring(Math.max(0, ssn.length() - 4)));
        MASKING_PATTERNS.put(CREDIT_CARD_PATTERN, cc -> "****-****-****-" + cc.substring(Math.max(0, cc.length() - 4)).replaceAll("[^0-9]", ""));
        MASKING_PATTERNS.put(PASSPORT_PATTERN, passport -> passport.substring(0, 2) + "******");
    }

    /**
    * Logs an informational message with PII masking.
    *
    * @param message The message to log
    */
    public void info(String message) {
        logger.info(maskPII(message));
    }

    /**
    * Logs an informational message with PII masking and includes the class name.
    *
    * @param clazz The class from which the log is originating
    * @param message The message to log
    */
    public void info(Class<?> clazz, String message) {
        Logger classLogger = LoggerFactory.getLogger(clazz);
        classLogger.info(maskPII(message));
    }

    /**
    * Logs a warning message with PII masking.
    *
    * @param message The message to log
    */
    public void warn(String message) {
        logger.warn(maskPII(message));
    }

    /**
    * Logs a warning message with PII masking and includes the class name.
    *
    * @param clazz The class from which the log is originating
    * @param message The message to log
    */
    public void warn(Class<?> clazz, String message) {
        Logger classLogger = LoggerFactory.getLogger(clazz);
        classLogger.warn(maskPII(message));
    }

    /**
    * Logs an error message with PII masking.
    *
    * @param message The message to log
    */
    public void error(String message) {
        logger.error(maskPII(message));
    }

    /**
    * Logs an error message with PII masking and includes the class name.
    *
    * @param clazz The class from which the log is originating
    * @param message The message to log
    */
    public void error(Class<?> clazz, String message) {
        Logger classLogger = LoggerFactory.getLogger(clazz);
        classLogger.error(maskPII(message));
    }

    /**
    * Logs an error message with PII masking and includes the exception details.
    *
    * @param message The message to log
    * @param throwable The exception to log
    */
    public void error(String message, Throwable throwable) {
        logger.error(maskPII(message), throwable);
    }

    /**
    * Logs an error message with PII masking and includes the class name and exception details.
    *
    * @param clazz The class from which the log is originating
    * @param message The message to log
    * @param throwable The exception to log
    */
    public void error(Class<?> clazz, String message, Throwable throwable) {
        Logger classLogger = LoggerFactory.getLogger(clazz);
        classLogger.error(maskPII(message), throwable);
    }

    /**
    * Logs a debug message with PII masking.
    *
    * @param message The message to log
    */
    public void debug(String message) {
        logger.debug(maskPII(message));
    }

    /**
    * Logs a debug message with PII masking and includes the class name.
    *
    * @param clazz The class from which the log is originating
    * @param message The message to log
    */
    public void debug(Class<?> clazz, String message) {
        Logger classLogger = LoggerFactory.getLogger(clazz);
        classLogger.debug(maskPII(message));
    }

    /**
    * Masks personally identifiable information (PII) in the given message.
    *
    * @param message The message that may contain PII
    * @return The message with PII masked
    */
    public String maskPII(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String maskedMessage = message;

        for (Map.Entry<Pattern, String> entry : MASKING_PATTERNS.entrySet()) {
            Pattern pattern = entry.getKey();
            java.util.regex.Matcher matcher = pattern.matcher(maskedMessage);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String match = matcher.group();
                String replacement = "";

                if (pattern.equals(EMAIL_PATTERN)) {
                    replacement = maskEmail(match);
                } else if (pattern.equals(PHONE_PATTERN)) {
                    replacement = "***-***-" + match.substring(Math.max(0, match.length() - 4));
                } else if (pattern.equals(SSN_PATTERN)) {
                    replacement = "***-**-" + match.substring(Math.max(0, match.length() - 4));
                } else if (pattern.equals(CREDIT_CARD_PATTERN)) {
                    replacement = "****-****-****-" + match.substring(Math.max(0, match.length() - 4)).replaceAll("[^0-9]", "");
                } else if (pattern.equals(PASSPORT_PATTERN)) {
                    replacement = match.substring(0, Math.min(2, match.length())) + "******";
                }

                matcher.appendReplacement(sb, replacement);
            }
            matcher.appendTail(sb);
            maskedMessage = sb.toString();
        }

        return maskedMessage;
    }

    /**
    * Masks an email address by showing only the first character of the local part
    * and the domain.
    *
    * @param email The email address to mask
    * @return The masked email address
    */
    private static String maskEmail(String email) {
        if (email == null || email.isEmpty() || !email.contains("@")) {
            return email;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        String maskedLocalPart = localPart.substring(0, 1) + "***";

        return maskedLocalPart + domain;
    }

    /**
    * Checks if a message contains PII.
    *
    * @param message The message to check
    * @return true if the message contains PII, false otherwise
    */
    public boolean containsPII(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        for (Pattern pattern : MASKING_PATTERNS.keySet()) {
            if (pattern.matcher(message).find()) {
                return true;
            }
        }

        return false;
    }

    /**
    * Logs an audit event with PII masking.
    *
    * @param userId The ID of the user performing the action
    * @param action The action being performed
    * @param resourceType The type of resource being accessed
    * @param resourceId The ID of the resource being accessed
    * @param details Additional details about the action
    */
    public void auditEvent(String userId, String action, String resourceType, String resourceId, String details) {
        String auditMessage = String.format(
        "AUDIT: User=%s, Action=%s, ResourceType=%s, ResourceId=%s, Details=%s",
        userId, action, resourceType, resourceId, maskPII(details)
        );
        logger.info(auditMessage);
    }
}