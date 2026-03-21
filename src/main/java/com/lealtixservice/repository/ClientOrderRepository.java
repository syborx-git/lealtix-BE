package com.lealtixservice.repository;

import com.lealtixservice.entity.ClientOrder;
import com.lealtixservice.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
     * Obtener la última orden de un cliente (para "lo de siempre")
     * Spring Data JPA automaticamente limita a 1 resultado con 'First'
     */
    Optional<ClientOrder> findFirstByCustomerIdAndTenantIdOrderByFechaDesc(Long customerId, Long tenantId);

    /**
     * Buscar órdenes pendientes de pago de un tenant
     */
    List<ClientOrder> findByTenantIdAndEstadoAndFechaBetween(Long tenantId, OrderStatus estado, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Contar órdenes por estado y tenant
     */
    Long countByTenantIdAndEstado(Long tenantId, OrderStatus estado);

    // ==================== NUEVAS QUERIES PARA MÉTRICAS DE FIDELIZACIÓN ====================

    /**
     * KPI 1: Tasa de Recompra - Total de clientes únicos identificados
     */
    @Query("SELECT COUNT(DISTINCT o.customer.id) FROM ClientOrder o " +
           "WHERE o.tenant.id = :tenantId " +
           "AND o.customer IS NOT NULL " +
           "AND o.fecha BETWEEN :from AND :to")
    Long countUniqueCustomersWithOrders(@Param("tenantId") Long tenantId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    /**
     * KPI 1: Tasa de Recompra - Clientes con más de una orden
     */
    @Query(value = "SELECT COUNT(*) FROM (" +
           "SELECT customer_id " +
           "FROM client_order " +
           "WHERE tenant_id = :tenantId " +
           "AND customer_id IS NOT NULL " +
           "AND fecha BETWEEN :from AND :to " +
           "GROUP BY customer_id " +
           "HAVING COUNT(*) > 1) AS repeat_customers",
           nativeQuery = true)
    Long countRepeatCustomers(@Param("tenantId") Long tenantId,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

    /**
     * KPI 2: Ventas Identificadas vs Generales - Estadísticas de órdenes identificadas
     */
    @Query("SELECT COUNT(o), COALESCE(SUM(o.total), 0), COALESCE(AVG(o.total), 0) " +
           "FROM ClientOrder o " +
           "WHERE o.tenant.id = :tenantId " +
           "AND o.customer IS NOT NULL " +
           "AND o.fecha BETWEEN :from AND :to")
    Object[] getIdentifiedSalesStats(@Param("tenantId") Long tenantId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    /**
     * KPI 2: Ventas Identificadas vs Generales - Estadísticas de ventas generales
     */
    @Query("SELECT COUNT(o), COALESCE(SUM(o.total), 0), COALESCE(AVG(o.total), 0) " +
           "FROM ClientOrder o " +
           "WHERE o.tenant.id = :tenantId " +
           "AND o.customer IS NULL " +
           "AND o.fecha BETWEEN :from AND :to")
    Object[] getGeneralSalesStats(@Param("tenantId") Long tenantId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    /**
     * KPI 3: LTV (Customer Lifetime Value) - Top clientes por valor total
     */
    @Query("SELECT o.customer.id, " +
           "o.customer.name, " +
           "o.customer.email, " +
           "COALESCE(SUM(o.total), 0), " +
           "COUNT(o.id), " +
           "COALESCE(AVG(o.total), 0), " +
           "MIN(o.fecha), " +
           "MAX(o.fecha) " +
           "FROM ClientOrder o " +
           "WHERE o.tenant.id = :tenantId " +
           "AND o.customer IS NOT NULL " +
           "AND o.fecha BETWEEN :from AND :to " +
           "GROUP BY o.customer.id, o.customer.name, o.customer.email " +
           "ORDER BY SUM(o.total) DESC")
    List<Object[]> getCustomerLTV(@Param("tenantId") Long tenantId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    /**
     * KPI 4: Tasa de Conversión de Cupón - Órdenes con cupón por campaña
     */
    @Query(value = "SELECT c.campaign_id, cam.title, COUNT(c.id) AS total_issued, " +
           "SUM(CASE WHEN c.status = 'REDEEMED' THEN 1 ELSE 0 END) AS total_redeemed, " +
           "COUNT(DISTINCT o.id) AS orders_with_coupon, COALESCE(SUM(o.total), 0) AS revenue " +
           "FROM coupon c " +
           "JOIN campaign cam ON cam.id = c.campaign_id " +
           "LEFT JOIN client_order o ON o.coupon_id = c.id AND o.tenant_id = :tenantId AND o.fecha BETWEEN :from AND :to " +
           "WHERE cam.business_id = :tenantId " +
           "AND c.created_at BETWEEN :from AND :to " +
           "GROUP BY c.campaign_id, cam.title",
           nativeQuery = true)
    List<Object[]> getCouponConversionRate(@Param("tenantId") Long tenantId,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    /**
     * KPI 6: ROI por Campaña - Ingresos generados por campaña
     */
    @Query("SELECT o.couponId, COUNT(o.id), COALESCE(SUM(o.total), 0) " +
           "FROM ClientOrder o " +
           "WHERE o.tenant.id = :tenantId " +
           "AND o.couponId IS NOT NULL " +
           "AND o.fecha BETWEEN :from AND :to " +
           "GROUP BY o.couponId")
    List<Object[]> getRevenuePerCoupon(@Param("tenantId") Long tenantId,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    /**
     * Debug: Contar órdenes generales (sin cliente) en el rango de fechas
     */
    @Query("SELECT COUNT(o) FROM ClientOrder o " +
           "WHERE o.tenant.id = :tenantId " +
           "AND o.customer IS NULL " +
           "AND o.fecha BETWEEN :from AND :to")
    Long countGeneralOrders(@Param("tenantId") Long tenantId,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to);

    /**
     * Debug: Contar órdenes identificadas (con cliente) en el rango de fechas
     */
    @Query("SELECT COUNT(o) FROM ClientOrder o " +
           "WHERE o.tenant.id = :tenantId " +
           "AND o.customer IS NOT NULL " +
           "AND o.fecha BETWEEN :from AND :to")
    Long countIdentifiedOrders(@Param("tenantId") Long tenantId,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

    /**
     * Debug: Obtener todas las órdenes en el rango para validación
     */
    @Query("SELECT COUNT(o) FROM ClientOrder o " +
           "WHERE o.tenant.id = :tenantId " +
           "AND o.fecha BETWEEN :from AND :to")
    Long countAllOrders(@Param("tenantId") Long tenantId,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

    /**
     * Debug: Obtener los minutos y máximos de fecha en la BD para este tenant
     */
    @Query(value = "SELECT MIN(fecha) as min_fecha, MAX(fecha) as max_fecha FROM client_order WHERE tenant_id = :tenantId",
           nativeQuery = true)
    Object[] getDateRangeInDB(@Param("tenantId") Long tenantId);

    /**
     * Debug: Contar todas las órdenes para un tenant sin filtro de fecha
     */
    @Query("SELECT COUNT(o) FROM ClientOrder o WHERE o.tenant.id = :tenantId")
    Long countAllOrdersByTenant(@Param("tenantId") Long tenantId);

    /**
     * Debug: Obtener órdenes del rango para inspeccionar
     */
    @Query(value = "SELECT id, tenant_id, customer_id, fecha, estado, total FROM client_order " +
           "WHERE tenant_id = :tenantId AND fecha BETWEEN :from AND :to " +
           "ORDER BY fecha DESC LIMIT 10",
           nativeQuery = true)
    List<Object[]> getOrdersInRangeDebug(@Param("tenantId") Long tenantId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    /**
     * Resumen de ventas totales: TODAS las órdenes sin filtros
     * Retorna: [0]=totalSales, [1]=avgTicket, [2]=transactionCount
     * Contempla:
     * - Órdenes con o sin cliente (customer_id IS NULL)
     * - Órdenes con o sin cupón (coupon_id IS NULL)
     */
    @Query("SELECT COALESCE(SUM(o.total), 0), COALESCE(AVG(o.total), 0), COUNT(o) " +
           "FROM ClientOrder o " +
           "WHERE o.tenant.id = :tenantId " +
           "AND o.fecha BETWEEN :from AND :to")
    Object[] getSalesSummary(@Param("tenantId") Long tenantId,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to);

    /**
     * Resumen de ventas con cupón redimido
     * Retorna: [0]=totalSales, [1]=avgTicket, [2]=transactionCount
     */
    @Query("SELECT COALESCE(SUM(o.total), 0), COALESCE(AVG(o.total), 0), COUNT(o) " +
           "FROM ClientOrder o " +
           "WHERE o.tenant.id = :tenantId " +
           "AND o.couponId IS NOT NULL " +
           "AND o.fecha BETWEEN :from AND :to")
    Object[] getSalesSummaryWithCoupon(@Param("tenantId") Long tenantId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    /**
     * Resumen de ventas sin cupón
     * Retorna: [0]=totalSales, [1]=avgTicket, [2]=transactionCount
     */
    @Query("SELECT COALESCE(SUM(o.total), 0), COALESCE(AVG(o.total), 0), COUNT(o) " +
           "FROM ClientOrder o " +
           "WHERE o.tenant.id = :tenantId " +
           "AND o.couponId IS NULL " +
           "AND o.fecha BETWEEN :from AND :to")
    Object[] getSalesSummaryWithoutCoupon(@Param("tenantId") Long tenantId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);
}