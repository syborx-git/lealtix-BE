package com.lealtixservice.repository;

import com.lealtixservice.entity.LoyaltyStampHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoyaltyStampHistoryRepository extends JpaRepository<LoyaltyStampHistory, Long> {

    @Query("SELECT h FROM LoyaltyStampHistory h " +
           "JOIN FETCH h.customer c " +
           "LEFT JOIN FETCH h.registeredByUser u " +
           "WHERE h.tenant.id = :tenantId " +
           "ORDER BY h.createdAt DESC")
    List<LoyaltyStampHistory> findByTenantIdOrderByCreatedAtDesc(@Param("tenantId") Long tenantId);

    @Query("SELECT h FROM LoyaltyStampHistory h " +
           "JOIN FETCH h.customer c " +
           "LEFT JOIN FETCH h.registeredByUser u " +
           "WHERE h.card.id = :cardId " +
           "ORDER BY h.createdAt DESC")
    List<LoyaltyStampHistory> findByCardIdOrderByCreatedAtDesc(@Param("cardId") Long cardId);

    List<LoyaltyStampHistory> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}

