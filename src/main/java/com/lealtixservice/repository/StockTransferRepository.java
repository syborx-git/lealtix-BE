package com.lealtixservice.repository;

import com.lealtixservice.entity.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    List<StockTransfer> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}