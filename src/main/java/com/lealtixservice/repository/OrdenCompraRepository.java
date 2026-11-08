package com.lealtixservice.repository;

import com.lealtixservice.entity.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Long> {

    List<OrdenCompra> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<OrdenCompra> findByTenantIdAndEstadoOrderByCreatedAtDesc(Long tenantId, String estado);

    Optional<OrdenCompra> findFirstByTenantIdAndEstadoOrderByCreatedAtDesc(Long tenantId, String estado);

    long countByTenantId(Long tenantId);
}

