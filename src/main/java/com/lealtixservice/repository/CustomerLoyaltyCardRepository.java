package com.lealtixservice.repository;

import com.lealtixservice.entity.CustomerLoyaltyCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerLoyaltyCardRepository extends JpaRepository<CustomerLoyaltyCard, Long> {

    Optional<CustomerLoyaltyCard> findByTenantIdAndCustomerId(Long tenantId, Long customerId);

    List<CustomerLoyaltyCard> findByTenantIdOrderByCurrentStampsDesc(Long tenantId);

    @Query("SELECT c FROM CustomerLoyaltyCard c " +
           "JOIN FETCH c.customer tc " +
           "WHERE c.tenant.id = :tenantId " +
           "ORDER BY c.hasPendingReward DESC, c.currentStamps DESC, c.updatedAt DESC")
    List<CustomerLoyaltyCard> findAllByTenantIdWithCustomer(@Param("tenantId") Long tenantId);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndHasPendingRewardTrue(Long tenantId);
}

