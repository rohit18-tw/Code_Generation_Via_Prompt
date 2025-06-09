package com.ekyc.service.repository;

import com.ekyc.service.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
* Repository interface for OtpVerification entity.
* Provides methods to perform CRUD operations and custom queries on OtpVerification data.
*/
@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

    /**
    * Find OTP verification by reference ID.
    *
    * @param referenceId the reference ID
    * @return optional containing the OTP verification if found
    */
    Optional<OtpVerification> findByReferenceId(UUID referenceId);

    /**
    * Find OTP verification by phone number.
    *
    * @param phoneNumber the phone number
    * @return optional containing the OTP verification if found
    */
    Optional<OtpVerification> findByPhoneNumber(String phoneNumber);

    /**
    * Find OTP verification by phone number and OTP.
    *
    * @param phoneNumber the phone number
    * @param otp the OTP
    * @return optional containing the OTP verification if found
    */
    Optional<OtpVerification> findByPhoneNumberAndOtp(String phoneNumber, String otp);

    /**
    * Find OTP verification by reference ID and OTP.
    *
    * @param referenceId the reference ID
    * @param otp the OTP
    * @return optional containing the OTP verification if found
    */
    Optional<OtpVerification> findByReferenceIdAndOtp(UUID referenceId, String otp);

    /**
    * Find all OTP verifications that have expired.
    *
    * @param currentDateTime the current date and time
    * @return list of expired OTP verifications
    */
    List<OtpVerification> findByExpiryTimeBefore(LocalDateTime currentDateTime);

    /**
    * Find all OTP verifications by status.
    *
    * @param status the status
    * @return list of OTP verifications with the specified status
    */
    List<OtpVerification> findByStatus(String status);

    /**
    * Update OTP verification status.
    *
    * @param id the ID of the OTP verification
    * @param status the new status
    * @return the number of rows affected
    */
    @Modifying
    @Transactional
    @Query("UPDATE OtpVerification o SET o.status = :status WHERE o.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") String status);

    /**
    * Increment retry count for OTP verification.
    *
    * @param id the ID of the OTP verification
    * @return the number of rows affected
    */
    @Modifying
    @Transactional
    @Query("UPDATE OtpVerification o SET o.retryCount = o.retryCount + 1 WHERE o.id = :id")
    int incrementRetryCount(@Param("id") UUID id);

    /**
    * Update OTP and expiry time.
    *
    * @param id the ID of the OTP verification
    * @param otp the new OTP
    * @param expiryTime the new expiry time
    * @return the number of rows affected
    */
    @Modifying
    @Transactional
    @Query("UPDATE OtpVerification o SET o.otp = :otp, o.expiryTime = :expiryTime, o.retryCount = 0 WHERE o.id = :id")
    int updateOtpAndExpiryTime(@Param("id") UUID id, @Param("otp") String otp, @Param("expiryTime") LocalDateTime expiryTime);

    /**
    * Delete expired OTP verifications.
    *
    * @param expiryTime the expiry time threshold
    * @return the number of rows affected
    */
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpVerification o WHERE o.expiryTime < :expiryTime")
    int deleteExpiredOtps(@Param("expiryTime") LocalDateTime expiryTime);

    /**
    * Count OTP verifications by phone number and creation time after specified time.
    * Used for rate limiting.
    *
    * @param phoneNumber the phone number
    * @param createdAfter the time threshold
    * @return the count of OTP verifications
    */
    @Query("SELECT COUNT(o) FROM OtpVerification o WHERE o.phoneNumber = :phoneNumber AND o.createdAt > :createdAfter")
    int countByPhoneNumberAndCreatedAtAfter(@Param("phoneNumber") String phoneNumber, @Param("createdAfter") LocalDateTime createdAfter);
}