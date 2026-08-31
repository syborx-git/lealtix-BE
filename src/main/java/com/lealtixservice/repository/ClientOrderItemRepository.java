package com.lealtixservice.repository;

import com.lealtixservice.entity.ClientOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClientOrderItemRepository extends JpaRepository<ClientOrderItem, UUID> {

    /**
     * Buscar todos los items de una orden
     */
    List<ClientOrderItem> findByOrderId(UUID orderId);

    /**
     * Eliminar todos los items de una orden
     */
    void deleteByOrderId(UUID orderId);

    /**
     * Buscar items por product_id (para análisis de productos más vendidos)
     */
    List<ClientOrderItem> findByProductId(Long productId);

    /**
     * Contar items en una orden
     */
    Long countByOrderId(UUID orderId);

    // ==================== NUEVAS QUERIES PARA ANÁLISIS DE PERSONALIZACIÓN ====================

    /**
     * Ventas agrupadas por categoría de producto por tenant en un periodo.
     * Retorna: [0]=categoryName, [1]=totalSales
     */
    @Query("SELECT c.nombre, SUM(i.cantidad * i.precioUnitario) " +
           "FROM ClientOrderItem i " +
           "JOIN i.product p " +
           "JOIN p.category c " +
           "WHERE i.order.tenant.id = :tenantId " +
           "AND i.order.fecha BETWEEN :from AND :to " +
           "GROUP BY c.nombre " +
           "ORDER BY SUM(i.cantidad * i.precioUnitario) DESC")
    List<Object[]> findSalesByCategory(@Param("tenantId") Long tenantId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    /**
     * Productos más vendidos por tenant en un periodo.
     * Retorna: [0]=productName, [1]=totalQuantity, [2]=totalRevenue
     */
    @Query("SELECT p.nombre, SUM(i.cantidad), SUM(i.cantidad * i.precioUnitario) " +
           "FROM ClientOrderItem i " +
           "JOIN i.product p " +
           "WHERE i.order.tenant.id = :tenantId " +
           "AND i.order.fecha BETWEEN :from AND :to " +
           "GROUP BY p.nombre " +
           "ORDER BY SUM(i.cantidad) DESC")
    List<Object[]> findTopProducts(@Param("tenantId") Long tenantId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);

    /**
     * KPI 5: Análisis de Personalización - Obtener todos los comentarios no nulos
     * Se usa para análisis de frecuencia de palabras clave en el servicio
     */
    @Query("SELECT i.comentarios FROM ClientOrderItem i " +
           "WHERE i.order.tenant.id = :tenantId " +
           "AND i.comentarios IS NOT NULL " +
           "AND i.comentarios <> '' " +
           "AND i.createdAt BETWEEN :from AND :to")
    List<String> findAllCustomizationComments(@Param("tenantId") Long tenantId,
                                                @Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to);

    /**
     * Análisis de personalización por cliente - Para conocer preferencias individuales
     */
    @Query("SELECT i.comentarios FROM ClientOrderItem i " +
           "WHERE i.order.customer.id = :customerId " +
           "AND i.comentarios IS NOT NULL " +
           "AND i.comentarios <> '' " +
           "ORDER BY i.createdAt DESC")
    List<String> findCustomizationCommentsByCustomer(@Param("customerId") Long customerId);

    /**
     * Análisis de personalización por producto - Identificar qué productos se personalizan más
     */
    @Query("SELECT i.product.id, i.product.nombre, COUNT(i.id) " +
           "FROM ClientOrderItem i " +
           "WHERE i.order.tenant.id = :tenantId " +
           "AND i.comentarios IS NOT NULL " +
           "AND i.comentarios <> '' " +
           "AND i.createdAt BETWEEN :from AND :to " +
           "GROUP BY i.product.id, i.product.nombre " +
           "ORDER BY COUNT(i.id) DESC")
    List<Object[]> findMostCustomizedProducts(@Param("tenantId") Long tenantId,
                                                @Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to);
}
