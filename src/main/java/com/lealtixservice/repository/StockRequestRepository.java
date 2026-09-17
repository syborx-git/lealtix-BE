package com.lealtixservice.repository;

import com.lealtixservice.entity.StockRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockRequestRepository extends JpaRepository<StockRequest, Long> {

    List<StockRequest> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<StockRequest> findByTenantIdAndEstadoOrderByCreatedAtDesc(Long tenantId, String estado);

    long countByTenantIdAndEstadoAndArea(Long tenantId, String estado, String area);
}