package com.lealtixservice.repository;

import com.lealtixservice.entity.RestockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RestockHistoryRepository extends JpaRepository<RestockHistory, Long> {

    @Query("SELECT COALESCE(SUM(r.costoTotal), 0) FROM RestockHistory r " +
            "WHERE r.tenantId = :tenantId AND r.createdAt >= :since")
    BigDecimal sumCostoTotalSince(@Param("tenantId") Long tenantId, @Param("since") LocalDateTime since);

    List<RestockHistory> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
