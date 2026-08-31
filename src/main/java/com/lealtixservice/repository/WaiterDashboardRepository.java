package com.lealtixservice.repository;

import com.lealtixservice.entity.TenantMenuProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio especializado para consultas del dashboard del mesero.
 * Contiene queries específicas para agregación de datos de ventas, clientes y productos.
 */
@Repository
public interface WaiterDashboardRepository extends JpaRepository<TenantMenuProduct, Long> {
    
    /**
     * Obtiene el total de ventas de clientes identificados en el día actual.
     * Retorna: [0]=totalSales identificadas
     */
    @Query(value = "SELECT COALESCE(SUM(co.total), 0) FROM client_order co " +
           "WHERE co.tenant_id = :tenantId " +
           "AND co.customer_id IS NOT NULL " +
           "AND DATE(co.fecha) = CURRENT_DATE",
           nativeQuery = true)
    Object[] getTotalIdentifiedSalesForToday(@Param("tenantId") Long tenantId);
    
    /**
     * Obtiene el total general de ventas (identificadas + anónimas) para hoy.
     * Retorna: [0]=totalSales
     */
    @Query(value = "SELECT COALESCE(SUM(co.total), 0) FROM client_order co " +
           "WHERE co.tenant_id = :tenantId " +
           "AND DATE(co.fecha) = CURRENT_DATE",
           nativeQuery = true)
    Object[] getTotalGeneralSalesForToday(@Param("tenantId") Long tenantId);
    
    /**
     * Cuenta nuevos clientes registrados hoy.
     */
    @Query(value = "SELECT COUNT(DISTINCT tc.id) FROM tenant_customer tc " +
           "WHERE tc.tenant_id = :tenantId " +
           "AND DATE(tc.created_at) = CURRENT_DATE",
           nativeQuery = true)
    Long countNewClientsToday(@Param("tenantId") Long tenantId);
    
    /**
     * Cuenta órdenes procesadas hoy.
     */
    @Query(value = "SELECT COUNT(co.id) FROM client_order co " +
           "WHERE co.tenant_id = :tenantId " +
           "AND DATE(co.fecha) = CURRENT_DATE",
           nativeQuery = true)
    Long countOrdersForToday(@Param("tenantId") Long tenantId);
    
    /**
     * Calcula el porcentaje de ventas identificadas.
     * Si el total es 0, retorna 0%.
     */
    @Query(value = "SELECT CASE " +
           "WHEN COALESCE(SUM(co.total), 0) = 0 THEN 0 " +
           "ELSE (SELECT COALESCE(SUM(co2.total), 0) FROM client_order co2 " +
           "      WHERE co2.tenant_id = :tenantId AND co2.customer_id IS NOT NULL AND DATE(co2.fecha) = CURRENT_DATE) * 100.0 / " +
           "     COALESCE(SUM(co.total), 0) " +
           "END " +
           "FROM client_order co " +
           "WHERE co.tenant_id = :tenantId AND DATE(co.fecha) = CURRENT_DATE",
           nativeQuery = true)
    Double calculateSalesIdentifiedPercentage(@Param("tenantId") Long tenantId);
    
    /**
     * Calcula la tasa de recompra de hoy.
     * Porcentaje de clientes que han comprado más de una vez.
     */
    @Query(value = "SELECT CASE " +
           "WHEN COUNT(DISTINCT tc.id) = 0 THEN 0 " +
           "ELSE (SELECT COUNT(DISTINCT co.customer_id) FROM client_order co " +
           "      WHERE co.tenant_id = :tenantId AND co.customer_id IN " +
           "      (SELECT customer_id FROM client_order WHERE tenant_id = :tenantId AND customer_id IS NOT NULL GROUP BY customer_id HAVING COUNT(*) > 1) " +
           "      AND DATE(co.fecha) = CURRENT_DATE) * 100.0 / " +
           "     COUNT(DISTINCT tc.id) " +
           "END " +
           "FROM tenant_customer tc " +
           "WHERE tc.tenant_id = :tenantId AND DATE(tc.created_at) = CURRENT_DATE",
           nativeQuery = true)
    Double calculateRepurchaseRate(@Param("tenantId") Long tenantId);
    
    /**
     * Obtiene los top clientes VIP (por LTV - Life Time Value).
     * Excluye clientes que visitaron en los últimos 7 días.
     * Retorna: [0]=id, [1]=name, [2]=email, [3]=phone, [4]=ltv, [5]=lastVisitDate, [6]=visitCount, [7]=averageTicket
     */
    @Query(value = "SELECT tc.id, tc.name, tc.email, tc.phone, " +
           "COALESCE(SUM(co.total), 0) as ltv, " +
           "MAX(co.fecha) as lastVisitDate, " +
           "COUNT(co.id) as visitCount, " +
           "COALESCE(AVG(co.total), 0) as averageTicket " +
           "FROM tenant_customer tc " +
           "LEFT JOIN client_order co ON tc.id = co.customer_id AND co.tenant_id = :tenantId " +
           "WHERE tc.tenant_id = :tenantId " +
           "GROUP BY tc.id, tc.name, tc.email, tc.phone " +
           "HAVING MAX(co.fecha) IS NULL OR MAX(co.fecha) < (NOW() - INTERVAL '7 days') " +
           "ORDER BY ltv DESC " +
           "LIMIT :limit",
           nativeQuery = true)
    List<Object[]> getVipClients(@Param("tenantId") Long tenantId, @Param("limit") int limit);
    
    /**
     * Obtiene los productos recomendados para venta cruzada desde la tabla product_cross_selling.
     * Retorna: [0]=id, [1]=nombre, [2]=precio, [3]=img_url, [4]=category
     */
    @Query(value = "SELECT p.id, p.nombre, p.precio, p.img_url, " +
           "COALESCE(c.nombre, 'Sin categoría') as category " +
           "FROM tenant_menu_product p " +
           "INNER JOIN product_cross_selling cs ON p.id = cs.suggested_product_id " +
           "INNER JOIN tenant_menu_category c ON p.category_id = c.id " +
           "WHERE cs.tenant_id = c.tenant_id " +
           "AND cs.tenant_id = :tenantId " +
           "GROUP BY p.id, p.nombre, p.precio, p.img_url, c.nombre " +
           "LIMIT :limit",
           nativeQuery = true)
    List<Object[]> getCrossSellProducts(@Param("tenantId") Long tenantId, @Param("limit") int limit);
    
    // ==================== NUEVAS QUERIES CON RANGO DE FECHAS ====================
    
    /**
     * Obtiene el total de ventas de clientes identificados en un rango de fechas.
     * Retorna: [0]=totalSales identificadas
     */
    @Query(value = "SELECT COALESCE(SUM(co.total), 0) FROM client_order co " +
           "WHERE co.tenant_id = :tenantId " +
           "AND co.customer_id IS NOT NULL " +
           "AND co.fecha BETWEEN :from AND :to",
           nativeQuery = true)
    Object[] getTotalIdentifiedSalesByDateRange(@Param("tenantId") Long tenantId,
                                                 @Param("from") LocalDateTime from,
                                                 @Param("to") LocalDateTime to);
    
    /**
     * Obtiene el total general de ventas (identificadas + anónimas) en un rango de fechas.
     * Retorna: [0]=totalSales
     */
    @Query(value = "SELECT COALESCE(SUM(co.total), 0) FROM client_order co " +
           "WHERE co.tenant_id = :tenantId " +
           "AND co.fecha BETWEEN :from AND :to",
           nativeQuery = true)
    Object[] getTotalGeneralSalesByDateRange(@Param("tenantId") Long tenantId,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);
    
    /**
     * Cuenta nuevos clientes registrados en un rango de fechas.
     */
    @Query(value = "SELECT COUNT(DISTINCT tc.id) FROM tenant_customer tc " +
           "WHERE tc.tenant_id = :tenantId " +
           "AND tc.created_at BETWEEN :from AND :to",
           nativeQuery = true)
    Long countNewClientsByDateRange(@Param("tenantId") Long tenantId,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);
    
    /**
     * Cuenta órdenes procesadas en un rango de fechas.
     */
    @Query(value = "SELECT COUNT(co.id) FROM client_order co " +
           "WHERE co.tenant_id = :tenantId " +
           "AND co.fecha BETWEEN :from AND :to",
           nativeQuery = true)
    Long countOrdersByDateRange(@Param("tenantId") Long tenantId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);
    
    /**
     * Calcula el porcentaje de ventas identificadas en un rango de fechas.
     */
    @Query(value = "SELECT CASE " +
           "WHEN COALESCE(SUM(co.total), 0) = 0 THEN 0 " +
           "ELSE (SELECT COALESCE(SUM(co2.total), 0) FROM client_order co2 " +
           "      WHERE co2.tenant_id = :tenantId AND co2.customer_id IS NOT NULL AND co2.fecha BETWEEN :from AND :to) * 100.0 / " +
           "     COALESCE(SUM(co.total), 0) " +
           "END " +
           "FROM client_order co " +
           "WHERE co.tenant_id = :tenantId AND co.fecha BETWEEN :from AND :to",
           nativeQuery = true)
    Double calculateSalesIdentifiedPercentageByDateRange(@Param("tenantId") Long tenantId,
                                                          @Param("from") LocalDateTime from,
                                                          @Param("to") LocalDateTime to);
    
    /**
     * Calcula la tasa de recompra en un rango de fechas.
     * Porcentaje de clientes que han comprado más de una vez en el rango.
     */
    @Query(value = "SELECT CASE " +
           "WHEN COUNT(DISTINCT co.customer_id) = 0 THEN 0 " +
           "ELSE (SELECT COUNT(DISTINCT co2.customer_id) FROM client_order co2 " +
           "      WHERE co2.tenant_id = :tenantId AND co2.customer_id IN " +
           "      (SELECT customer_id FROM client_order WHERE tenant_id = :tenantId AND customer_id IS NOT NULL AND fecha BETWEEN :from AND :to GROUP BY customer_id HAVING COUNT(*) > 1) " +
           "      AND co2.fecha BETWEEN :from AND :to) * 100.0 / " +
           "     COUNT(DISTINCT co.customer_id) " +
           "END " +
           "FROM client_order co " +
           "WHERE co.tenant_id = :tenantId AND co.customer_id IS NOT NULL AND co.fecha BETWEEN :from AND :to",
           nativeQuery = true)
    Double calculateRepurchaseRateByDateRange(@Param("tenantId") Long tenantId,
                                               @Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);
}