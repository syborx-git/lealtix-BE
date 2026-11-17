package com.lealtixservice.repository;

import com.lealtixservice.entity.TenantLoyaltyCardConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantLoyaltyCardConfigRepository extends JpaRepository<TenantLoyaltyCardConfig, Long> {
    Optional<TenantLoyaltyCardConfig> findByTenantId(Long tenantId);
    boolean existsByTenantId(Long tenantId);
}

