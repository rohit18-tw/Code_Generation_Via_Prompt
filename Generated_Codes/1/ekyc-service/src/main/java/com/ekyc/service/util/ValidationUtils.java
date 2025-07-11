package com.ekyc.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Utility class for validation operations in the eKYC service.
* Provides methods to validate various types of data such as email, phone numbers,
* dates, and document numbers.
*/
@Component
public class ValidationUtils {

    private static final Logger logger = LoggerFactory.getLogger(ValidationUtils.class);

    // Regular expression patterns for validation
    private static final Pattern EMAIL_PATTERN =
    Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    private static final Pattern PHONE_PATTERN =
    Pattern.compile("^\\+?[0-9]{10,15}$");

    private static final Pattern ALPHANUMERIC_PATTERN =
    Pattern.compile("^[a-zA-Z0-9]*$");

    private static final Pattern NAME_PATTERN =
    Pattern.compile("^[a-zA-Z\\s'-]{2,50}$");

    private static final Pattern PAN_PATTERN =
    Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");

    private static final Pattern AADHAAR_PATTERN =
    Pattern.compile("^[0-9]{12}$");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
    * Validates if the provided string is not null and not empty.
    *
    * @param str the string to validate
    * @return true if the string is not null and not empty, false otherwise
    */
    public boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
    * Validates if the provided string is a valid email address.
    *
    * @param email the email address to validate
    * @return true if the email is valid, false otherwise
    */
    public boolean isValidEmail(String email) {
        if (!isNotEmpty(email)) {
            return false;
        }

        Matcher matcher = EMAIL_PATTERN.matcher(email);
        boolean isValid = matcher.matches();

        if (!isValid) {
            logger.debug("Invalid email format: {}", maskEmail(email));
        }

        return isValid;
    }

    /**
    * Validates if the provided string is a valid phone number.
    *
    * @param phoneNumber the phone number to validate
    * @return true if the phone number is valid, false otherwise
    */
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (!isNotEmpty(phoneNumber)) {
            return false;
        }

        Matcher matcher = PHONE_PATTERN.matcher(phoneNumber);
        boolean isValid = matcher.matches();

        if (!isValid) {
            logger.debug("Invalid phone number format: {}", maskPhoneNumber(phoneNumber));
        }

        return isValid;
    }

    /**
    * Validates if the provided string is a valid date in the format yyyy-MM-dd.
    *
    * @param dateStr the date string to validate
    * @return true if the date is valid, false otherwise
    */
    public boolean isValidDate(String dateStr) {
        if (!isNotEmpty(dateStr)) {
            return false;
        }

        try {
            LocalDate.parse(dateStr, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            logger.debug("Invalid date format: {}", dateStr);
            return false;
        }
    }

    /**
    * Validates if the provided date is not in the future.
    *
    * @param dateStr the date string to validate in the format yyyy-MM-dd
    * @return true if the date is not in the future, false otherwise
    */
    public boolean isNotFutureDate(String dateStr) {
        if (!isValidDate(dateStr)) {
            return false;
        }

        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
        LocalDate today = LocalDate.now();

        boolean isValid = !date.isAfter(today);

        if (!isValid) {
            logger.debug("Date is in the future: {}", dateStr);
        }

        return isValid;
    }

    /**
    * Validates if the provided string is alphanumeric.
    *
    * @param str the string to validate
    * @return true if the string is alphanumeric, false otherwise
    */
    public boolean isAlphanumeric(String str) {
        if (!isNotEmpty(str)) {
            return false;
        }

        return ALPHANUMERIC_PATTERN.matcher(str).matches();
    }

    /**
    * Validates if the provided string is a valid name.
    *
    * @param name the name to validate
    * @return true if the name is valid, false otherwise
    */
    public boolean isValidName(String name) {
        if (!isNotEmpty(name)) {
            return false;
        }

        boolean isValid = NAME_PATTERN.matcher(name).matches();

        if (!isValid) {
            logger.debug("Invalid name format: {}", name);
        }

        return isValid;
    }

    /**
    * Validates if the provided string is a valid PAN (Permanent Account Number).
    *
    * @param pan the PAN to validate
    * @return true if the PAN is valid, false otherwise
    */
    public boolean isValidPAN(String pan) {
        if (!isNotEmpty(pan)) {
            return false;
        }

        boolean isValid = PAN_PATTERN.matcher(pan).matches();

        if (!isValid) {
            logger.debug("Invalid PAN format: {}", maskPAN(pan));
        }

        return isValid;
    }

    /**
    * Validates if the provided string is a valid Aadhaar number.
    *
    * @param aadhaar the Aadhaar number to validate
    * @return true if the Aadhaar number is valid, false otherwise
    */
    public boolean isValidAadhaar(String aadhaar) {
        if (!isNotEmpty(aadhaar)) {
            return false;
        }

        boolean isValid = AADHAAR_PATTERN.matcher(aadhaar).matches();

        if (!isValid) {
            logger.debug("Invalid Aadhaar format: {}", maskAadhaar(aadhaar));
        }

        return isValid;
    }

    /**
    * Validates if the provided age is within the specified range.
    *
    * @param age the age to validate
    * @param minAge the minimum age allowed
    * @param maxAge the maximum age allowed
    * @return true if the age is within the specified range, false otherwise
    */
    public boolean isValidAge(int age, int minAge, int maxAge) {
        boolean isValid = age >= minAge && age <= maxAge;

        if (!isValid) {
            logger.debug("Age {} is outside the valid range of {} to {}", age, minAge, maxAge);
        }

        return isValid;
    }

    /**
    * Calculates age from a date of birth.
    *
    * @param dobStr the date of birth string in the format yyyy-MM-dd
    * @return the calculated age, or -1 if the date is invalid
    */
    public int calculateAge(String dobStr) {
        if (!isValidDate(dobStr)) {
            return -1;
        }

        LocalDate dob = LocalDate.parse(dobStr, DATE_FORMATTER);
        LocalDate today = LocalDate.now();

        return today.getYear() - dob.getYear() - (today.getDayOfYear() < dob.getDayOfYear() ? 1 : 0);
    }

    /**
    * Masks an email address for logging purposes.
    *
    * @param email the email address to mask
    * @return the masked email address
    */
    private String maskEmail(String email) {
        if (!isNotEmpty(email)) {
            return "";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***@" + (atIndex < 0 ? "" : email.substring(atIndex + 1));
        }

        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }

    /**
    * Masks a phone number for logging purposes.
    *
    * @param phoneNumber the phone number to mask
    * @return the masked phone number
    */
    private String maskPhoneNumber(String phoneNumber) {
        if (!isNotEmpty(phoneNumber)) {
            return "";
        }

        int length = phoneNumber.length();
        if (length <= 4) {
            return "****";
        }

        return "****" + phoneNumber.substring(length - 4);
    }

    /**
    * Masks a PAN for logging purposes.
    *
    * @param pan the PAN to mask
    * @return the masked PAN
    */
    private String maskPAN(String pan) {
        if (!isNotEmpty(pan)) {
            return "";
        }

        int length = pan.length();
        if (length <= 4) {
            return "****";
        }

        return pan.substring(0, 2) + "****" + pan.substring(length - 2);
    }

    /**
    * Masks an Aadhaar number for logging purposes.
    *
    * @param aadhaar the Aadhaar number to mask
    * @return the masked Aadhaar number
    */
    private String maskAadhaar(String aadhaar) {
        if (!isNotEmpty(aadhaar)) {
            return "";
        }

        int length = aadhaar.length();
        if (length <= 4) {
            return "****";
        }

        return "****" + aadhaar.substring(length - 4);
    }
}