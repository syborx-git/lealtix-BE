package com.lealtixservice.repository;

import com.lealtixservice.entity.ClientOrder;
import com.lealtixservice.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClientOrderRepository extends JpaRepository<ClientOrder, UUID>, JpaSpecificationExecutor<ClientOrder> {

    /**
     * Buscar órdenes por tenant_id y customer_id
     */
    Page<ClientOrder> findByTenantIdAndCustomerId(Long tenantId, Long customerId, Pageable pageable);

    /**
     * Buscar todas las órdenes de un tenant
     */
    Page<ClientOrder> findByTenantId(Long tenantId, Pageable pageable);

    /**
     * Buscar órdenes por tenant_id y estado
     */
    Page<ClientOrder> findByTenantIdAndEstado(Long tenantId, OrderStatus estado, Pageable pageable);

    /**
     * Buscar órdenes de un cliente por fecha descendente
     */
    List<ClientOrder> findByCustomerIdOrderByFechaDesc(Long customerId);

    /**
     * Buscar órdenes pendientes de pago de un tenant
     */
    List<ClientOrder> findByTenantIdAndEstadoAndFechaBetween(Long tenantId, OrderStatus estado, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Contar órdenes por estado y tenant
     */
    Long countByTenantIdAndEstado(Long tenantId, OrderStatus estado);
}
