package com.lealtixservice.repository;

import com.lealtixservice.entity.Merma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio de consultas optimizadas para el reporte 2.3: Auditoria de Mermas.
 */
@Repository
public interface ReporteMermasRepository extends JpaRepository<Merma, Long> {

    /**
     * Resumen de mermas del periodo:
     * 0: costo_total (Double)
     * 1: cantidad_total (Double)
     * 2: total_eventos (Long)
     * 3: costo_operativa (Double)
     * 4: costo_administrativa (Double)
     */
    @Query(value = """
            SELECT
                COALESCE(SUM(costo_total), 0) AS costo_total,
                COALESCE(SUM(cantidad), 0) AS cantidad_total,
                COUNT(*) AS total_eventos,
                COALESCE(SUM(CASE WHEN categoria_merma = 'COMANDADA' OR tipo_merma = 'OPERATIVA' THEN costo_total ELSE 0 END), 0) AS costo_operativa,
                COALESCE(SUM(CASE WHEN categoria_merma = 'ADMINISTRATIVA' THEN costo_total ELSE 0 END), 0) AS costo_administrativa
            FROM merma
            WHERE tenant_id = :tenantId
              AND fecha >= :from AND fecha < :toExclusivo
            """, nativeQuery = true)
    Object[] resumenMermas(@Param("tenantId") Long tenantId,
                           @Param("from") LocalDateTime from,
                           @Param("toExclusivo") LocalDateTime toExclusivo);

    /**
     * Agrupado por motivo.
     * 0: motivo (String), 1: eventos (Number), 2: cantidad (Number), 3: costo_total (Number)
     */
    @Query(value = """
            SELECT
                COALESCE(NULLIF(TRIM(motivo), ''), 'Sin motivo especificado') AS motivo,
                COUNT(*) AS eventos,
                COALESCE(SUM(cantidad), 0) AS cantidad,
                COALESCE(SUM(costo_total), 0) AS costo_total
            FROM merma
            WHERE tenant_id = :tenantId
              AND fecha >= :from AND fecha < :toExclusivo
            GROUP BY COALESCE(NULLIF(TRIM(motivo), ''), 'Sin motivo especificado')
            ORDER BY costo_total DESC
            """, nativeQuery = true)
    List<Object[]> mermasPorMotivo(@Param("tenantId") Long tenantId,
                                  @Param("from") LocalDateTime from,
                                  @Param("toExclusivo") LocalDateTime toExclusivo);

    /**
     * Agrupado por responsable (usuario) y origen.
     * 0: usuario (String), 1: origen (String), 2: eventos (Number), 3: costo_total (Number)
     */
    @Query(value = """
            SELECT
                COALESCE(NULLIF(TRIM(usuario_nombre), ''), 'Sin asignar') AS usuario,
                COALESCE(NULLIF(TRIM(origen), ''), 'Comanda') AS origen,
                COUNT(*) AS eventos,
                COALESCE(SUM(costo_total), 0) AS costo_total
            FROM merma
            WHERE tenant_id = :tenantId
              AND fecha >= :from AND fecha < :toExclusivo
            GROUP BY usuario_nombre, origen
            ORDER BY costo_total DESC
            """, nativeQuery = true)
    List<Object[]> mermasPorResponsable(@Param("tenantId") Long tenantId,
                                       @Param("from") LocalDateTime from,
                                       @Param("toExclusivo") LocalDateTime toExclusivo);

    /**
     * Agrupado por insumo / producto mermado.
     * 0: item (String), 1: unidad (String), 2: eventos (Number), 3: cantidad (Number), 4: costo_total (Number)
     */
    @Query(value = """
            SELECT
                COALESCE(NULLIF(TRIM(insumo_nombre), ''), NULLIF(TRIM(producto_nombre), ''), 'Sin nombre') AS item,
                COALESCE(NULLIF(TRIM(unidad), ''), 'pieza') AS unidad,
                COUNT(*) AS eventos,
                COALESCE(SUM(cantidad), 0) AS cantidad,
                COALESCE(SUM(costo_total), 0) AS costo_total
            FROM merma
            WHERE tenant_id = :tenantId
              AND fecha >= :from AND fecha < :toExclusivo
            GROUP BY COALESCE(NULLIF(TRIM(insumo_nombre), ''), NULLIF(TRIM(producto_nombre), ''), 'Sin nombre'),
                     COALESCE(NULLIF(TRIM(unidad), ''), 'pieza')
            ORDER BY costo_total DESC
            """, nativeQuery = true)
    List<Object[]> mermasPorInsumo(@Param("tenantId") Long tenantId,
                                  @Param("from") LocalDateTime from,
                                  @Param("toExclusivo") LocalDateTime toExclusivo);

    /**
     * Lista detallada cronológica de mermas.
     * 0: id, 1: fecha, 2: ticket, 3: usuario_nombre, 4: categoria_merma, 5: origen,
     * 6: item_nombre, 7: cantidad, 8: unidad, 9: costo_unitario, 10: costo_total, 11: motivo
     */
    @Query(value = """
            SELECT
                m.id,
                m.fecha,
                COALESCE(m.ticket, '—') AS ticket,
                COALESCE(m.usuario_nombre, '—') AS usuario_nombre,
                COALESCE(m.categoria_merma, 'COMANDADA') AS categoria_merma,
                COALESCE(m.origen, '—') AS origen,
                COALESCE(m.insumo_nombre, m.producto_nombre, '—') AS item_nombre,
                m.cantidad,
                COALESCE(m.unidad, 'pieza') AS unidad,
                COALESCE(m.costo_unitario, 0.0) AS costo_unitario,
                COALESCE(m.costo_total, 0.0) AS costo_total,
                COALESCE(m.motivo, '—') AS motivo
            FROM merma m
            WHERE m.tenant_id = :tenantId
              AND m.fecha >= :from AND m.fecha < :toExclusivo
            ORDER BY m.fecha DESC
            LIMIT :limite
            """, nativeQuery = true)
    List<Object[]> detalleMermas(@Param("tenantId") Long tenantId,
                                @Param("from") LocalDateTime from,
                                @Param("toExclusivo") LocalDateTime toExclusivo,
                                @Param("limite") int limite);
}
