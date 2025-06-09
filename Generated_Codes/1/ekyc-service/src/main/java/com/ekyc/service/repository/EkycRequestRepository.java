package com.ekyc.service.repository;

import com.ekyc.service.entity.EkycRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
* Repository interface for {@link EkycRequest} entity.
* Provides methods to interact with the eKYC request data in the database.
*/
@Repository
public interface EkycRequestRepository extends JpaRepository<EkycRequest, UUID> {

    /**
    * Find an eKYC request by its unique reference ID.
    *
    * @param referenceId the reference ID to search for
    * @return an Optional containing the eKYC request if found, or empty if not found
    */
    Optional<EkycRequest> findByReferenceId(String referenceId);

    /**
    * Find all eKYC requests for a specific customer.
    *
    * @param customerId the customer ID to search for
    * @param pageable pagination information
    * @return a Page of eKYC requests
    */
    Page<EkycRequest> findByCustomerId(String customerId, Pageable pageable);

    /**
    * Find all eKYC requests with a specific status.
    *
    * @param status the status to search for
    * @param pageable pagination information
    * @return a Page of eKYC requests
    */
    Page<EkycRequest> findByStatus(String status, Pageable pageable);

    /**
    * Find all eKYC requests for a specific customer with a specific status.
    *
    * @param customerId the customer ID to search for
    * @param status the status to search for
    * @param pageable pagination information
    * @return a Page of eKYC requests
    */
    Page<EkycRequest> findByCustomerIdAndStatus(String customerId, String status, Pageable pageable);

    /**
    * Find all eKYC requests created between the specified dates.
    *
    * @param startDate the start date
    * @param endDate the end date
    * @param pageable pagination information
    * @return a Page of eKYC requests
    */
    Page<EkycRequest> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
    * Find all eKYC requests by document type.
    *
    * @param documentType the document type to search for
    * @param pageable pagination information
    * @return a Page of eKYC requests
    */
    Page<EkycRequest> findByDocumentType(String documentType, Pageable pageable);

    /**
    * Find all eKYC requests that have been pending for longer than the specified time.
    *
    * @param timestamp the timestamp to compare against
    * @return a list of eKYC requests
    */
    @Query("SELECT e FROM EkycRequest e WHERE e.status = 'PENDING' AND e.createdAt < :timestamp")
    List<EkycRequest> findPendingRequestsOlderThan(@Param("timestamp") LocalDateTime timestamp);

    /**
    * Update the status of an eKYC request.
    *
    * @param id the ID of the eKYC request
    * @param status the new status
    * @param updatedAt the timestamp of the update
    * @return the number of rows affected
    */
    @Modifying
    @Query("UPDATE EkycRequest e SET e.status = :status, e.updatedAt = :updatedAt WHERE e.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") String status, @Param("updatedAt") LocalDateTime updatedAt);

    /**
    * Count the number of eKYC requests by status.
    *
    * @param status the status to count
    * @return the count of eKYC requests
    */
    long countByStatus(String status);

    /**
    * Count the number of eKYC requests by customer ID.
    *
    * @param customerId the customer ID to count
    * @return the count of eKYC requests
    */
    long countByCustomerId(String customerId);

    /**
    * Find all eKYC requests by document number (exact match).
    * Note: This should be used carefully as document numbers are sensitive information.
    *
    * @param documentNumber the document number to search for
    * @return a list of eKYC requests
    */
    List<EkycRequest> findByDocumentNumber(String documentNumber);

    /**
    * Find the most recent eKYC request for a specific customer.
    *
    * @param customerId the customer ID to search for
    * @return an Optional containing the most recent eKYC request if found, or empty if not found
    */
    @Query("SELECT e FROM EkycRequest e WHERE e.customerId = :customerId ORDER BY e.createdAt DESC")
    Optional<EkycRequest> findMostRecentByCustomerId(@Param("customerId") String customerId);

    /**
    * Delete all expired eKYC requests (older than the specified time).
    *
    * @param expiryTime the timestamp before which requests are considered expired
    * @return the number of rows affected
    */
    @Modifying
    @Query("DELETE FROM EkycRequest e WHERE e.createdAt < :expiryTime")
    int deleteExpiredRequests(@Param("expiryTime") LocalDateTime expiryTime);
}