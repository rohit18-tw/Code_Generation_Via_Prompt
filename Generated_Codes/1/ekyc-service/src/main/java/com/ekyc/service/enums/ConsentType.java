package com.ekyc.service.enums;

import java.util.Arrays;
import java.util.Optional;

/**
* Enumeration representing the consent types in the eKYC system.
* Used to track whether a user has provided consent for various operations.
*/
public enum ConsentType {
    YES("yes"),
    NO("no");

    private final String value;

    /**
    * Constructor for ConsentType enum.
    *
    * @param value The string representation of the consent type
    */
    ConsentType(String value) {
        this.value = value;
    }

    /**
    * Gets the string value of the consent type.
    *
    * @return The string representation of the consent type
    */
    public String getValue() {
        return value;
    }

    /**
    * Finds a ConsentType enum by its string value.
    *
    * @param value The string value to search for
    * @return An Optional containing the matching ConsentType, or empty if not found
    */
    public static Optional<ConsentType> fromValue(String value) {
        return Arrays.stream(ConsentType.values())
        .filter(consentType -> consentType.value.equalsIgnoreCase(value))
        .findFirst();
    }

    /**
    * Converts a string value to a ConsentType enum.
    *
    * @param value The string value to convert
    * @return The matching ConsentType, or null if not found
    */
    public static ConsentType fromString(String value) {
        return fromValue(value).orElse(null);
    }

    /**
    * Returns the string representation of this ConsentType.
    *
    * @return The string value of this ConsentType
    */
    @Override
    public String toString() {
        return value;
    }
}