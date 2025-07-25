package com.ekyc.service.enums;

import java.util.Arrays;
import java.util.Optional;

/**
* Enumeration representing the verification status of eKYC processes.
* This enum tracks the state of verification requests through the system.
*/
public enum VerificationStatus {
    /**
    * Indicates that the verification process has been successfully completed
    * and the identity has been verified.
    */
    VERIFIED("Verification successful"),

    /**
    * Indicates that the verification process has failed due to
    * invalid documents, data mismatch, or other verification errors.
    */
    FAILED("Verification failed"),

    /**
    * Indicates that the verification process is currently in progress
    * and awaiting completion.
    */
    IN_PROGRESS("Verification in progress");

    private final String description;

    /**
    * Constructor for VerificationStatus enum.
    *
    * @param description A human-readable description of the status
    */
    VerificationStatus(String description) {
        this.description = description;
    }

    /**
    * Gets the description of the verification status.
    *
    * @return A human-readable description of the status
    */
    public String getDescription() {
        return description;
    }

    /**
    * Finds a VerificationStatus by its name, case-insensitive.
    *
    * @param name The name of the verification status to find
    * @return An Optional containing the matching VerificationStatus, or empty if not found
    */
    public static Optional<VerificationStatus> findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }

        return Arrays.stream(VerificationStatus.values())
        .filter(status -> status.name().equalsIgnoreCase(name.trim()))
        .findFirst();
    }

    /**
    * Checks if the verification process is complete (either VERIFIED or FAILED).
    *
    * @return true if the status is VERIFIED or FAILED, false if it's IN_PROGRESS
    */
    public boolean isComplete() {
        return this == VERIFIED || this == FAILED;
    }

    /**
    * Checks if the verification was successful.
    *
    * @return true if the status is VERIFIED, false otherwise
    */
    public boolean isSuccessful() {
        return this == VERIFIED;
    }

    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}