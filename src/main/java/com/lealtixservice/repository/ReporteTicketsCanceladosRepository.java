package com.lealtixservice.repository;

import com.lealtixservice.entity.ClientOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Consultas analíticas nativas para el Reporte 4.1: Auditoría de Tickets Cancelados.
 */
@Repository
public interface ReporteTicketsCanceladosRepository extends JpaRepository<ClientOrder, UUID> {

    /**
     * Resumen de órdenes canceladas en el periodo: cantidad, monto total y promedio.
     * Indices: 0 cantidad, 1 total, 2 promedio.
     */
    @Query(value = """
            SELECT COUNT(*)                   AS cancelaciones,
                   COALESCE(SUM(co.total), 0) AS monto_total,
                   COALESCE(AVG(co.total), 0) AS promedio_ticket
            FROM client_order co
            WHERE co.tenant_id = :tenantId
              AND co.estado = 'CANCELADA'
              AND (   (co.cancelled_at >= :from AND co.cancelled_at < :toExclusivo)
                   OR (co.cancelled_at IS NULL AND co.fecha >= :from AND co.fecha < :toExclusivo) )
            """, nativeQuery = true)
    List<Object[]> resumenCancelaciones(@Param("tenantId") Long tenantId,
                                        @Param("from") LocalDateTime from,
                                        @Param("toExclusivo") LocalDateTime toExclusivo);

    /**
     * Total de órdenes generadas en el periodo (activas, pagadas o canceladas)
     * para calcular la tasa de cancelación sobre la operación total.
     */
    @Query(value = """
            SELECT COUNT(*) AS total_ordenes
            FROM client_order co
            WHERE co.tenant_id = :tenantId
              AND co.fecha >= :from AND co.fecha < :toExclusivo
            """, nativeQuery = true)
    Long totalOrdenesGeneradas(@Param("tenantId") Long tenantId,
                               @Param("from") LocalDateTime from,
                               @Param("toExclusivo") LocalDateTime toExclusivo);

    /**
     * Desglose de cancelaciones agrupadas por motivo registrado.
     * Indices: 0 motivo, 1 operaciones, 2 total_monto.
     */
    @Query(value = """
            SELECT COALESCE(NULLIF(TRIM(co.cancellation_reason), ''), 'Sin motivo registrado') AS motivo,
                   COUNT(*)                                                                     AS operaciones,
                   COALESCE(SUM(co.total), 0)                                                  AS total_monto
            FROM client_order co
            WHERE co.tenant_id = :tenantId
              AND co.estado = 'CANCELADA'
              AND (   (co.cancelled_at >= :from AND co.cancelled_at < :toExclusivo)
                   OR (co.cancelled_at IS NULL AND co.fecha >= :from AND co.fecha < :toExclusivo) )
            GROUP BY COALESCE(NULLIF(TRIM(co.cancellation_reason), ''), 'Sin motivo registrado')
            ORDER BY total_monto DESC, operaciones DESC
            """, nativeQuery = true)
    List<Object[]> cancelacionesPorMotivo(@Param("tenantId") Long tenantId,
                                          @Param("from") LocalDateTime from,
                                          @Param("toExclusivo") LocalDateTime toExclusivo);

    /**
     * Desglose de cancelaciones agrupadas por el usuario/cajero responsable.
     * Indices: 0 responsable, 1 operaciones, 2 total_monto.
     */
    @Query(value = """
            SELECT COALESCE(NULLIF(TRIM(co.cancelled_by), ''), 'No especificado') AS responsable,
                   COUNT(*)                                                        AS operaciones,
                   COALESCE(SUM(co.total), 0)                                     AS total_monto
            FROM client_order co
            WHERE co.tenant_id = :tenantId
              AND co.estado = 'CANCELADA'
              AND (   (co.cancelled_at >= :from AND co.cancelled_at < :toExclusivo)
                   OR (co.cancelled_at IS NULL AND co.fecha >= :from AND co.fecha < :toExclusivo) )
            GROUP BY COALESCE(NULLIF(TRIM(co.cancelled_by), ''), 'No especificado')
            ORDER BY total_monto DESC, operaciones DESC
            """, nativeQuery = true)
    List<Object[]> cancelacionesPorResponsable(@Param("tenantId") Long tenantId,
                                               @Param("from") LocalDateTime from,
                                               @Param("toExclusivo") LocalDateTime toExclusivo);

    /**
     * Detalle cronológico de las comandas canceladas en el periodo.
     * Indices:
     * 0 id, 1 fecha_cancelacion, 2 fecha_creacion, 3 motivo, 4 responsable,
     * 5 mesa, 6 mesero, 7 total, 8 items_count, 9 items_resumen
     */
    @Query(value = """
            SELECT CAST(co.id AS VARCHAR)                                                           AS id,
                   COALESCE(co.cancelled_at, co.fecha)                                             AS fecha_cancelacion,
                   co.fecha                                                                         AS fecha_creacion,
                   COALESCE(NULLIF(TRIM(co.cancellation_reason), ''), 'Sin motivo registrado')      AS motivo,
                   COALESCE(NULLIF(TRIM(co.cancelled_by), ''), 'No especificado')                  AS responsable,
                   COALESCE(m.nombre, CASE WHEN m.numero IS NOT NULL THEN CONCAT('Mesa ', m.numero) ELSE 'Sin mesa' END) AS mesa,
                   COALESCE(NULLIF(TRIM(u.full_name), ''), u.email, 'Sin mesero')                   AS mesero,
                   COALESCE(co.total, 0)                                                            AS total,
                   COALESCE((SELECT COUNT(*) FROM client_order_item coi WHERE coi.order_id = co.id), 0) AS items_count,
                   COALESCE((
                       SELECT string_agg(CONCAT(sub.cantidad, 'x ', sub.nombre), ', ')
                       FROM (
                           SELECT coi2.cantidad, p2.nombre
                           FROM client_order_item coi2
                           JOIN tenant_menu_product p2 ON p2.id = coi2.product_id
                           WHERE coi2.order_id = co.id
                           LIMIT 3
                       ) sub
                   ), 'Sin items') AS items_resumen
            FROM client_order co
            LEFT JOIN mesa m ON m.id = co.mesa
            LEFT JOIN app_user u ON u.id = co.mesero_id
            WHERE co.tenant_id = :tenantId
              AND co.estado = 'CANCELADA'
              AND (   (co.cancelled_at >= :from AND co.cancelled_at < :toExclusivo)
                   OR (co.cancelled_at IS NULL AND co.fecha >= :from AND co.fecha < :toExclusivo) )
            ORDER BY COALESCE(co.cancelled_at, co.fecha) DESC
            LIMIT :limite
            """, nativeQuery = true)
    List<Object[]> detalleCancelaciones(@Param("tenantId") Long tenantId,
                                        @Param("from") LocalDateTime from,
                                        @Param("toExclusivo") LocalDateTime toExclusivo,
                                        @Param("limite") int limite);
}
