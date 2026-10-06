package com.lealtixservice.repository;

import com.lealtixservice.entity.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de consultas para el Reporte 2.4: Alertas de Stock Minimo y Critico.
 */
@Repository
public interface ReporteStockMinimoRepository extends JpaRepository<Insumo, Long> {

    /**
     * Obtiene todos los insumos activos del tenant con sus existencias en bodega,
     * cocina, barra, stock minimo y costo promedio de restocks.
     *
     * 0: id (Long)
     * 1: nombre (String)
     * 2: unidad (String)
     * 3: stock_bodega (Double)
     * 4: stock_distribuido (Double)
     * 5: stock_cocina (Double)
     * 6: stock_barra (Double)
     * 7: stock_minimo (Double)
     * 8: es_bebida (Boolean)
     * 9: costo_promedio (Double)
     */
    @Query(value = """
            SELECT
                i.id,
                i.nombre,
                COALESCE(i.unidad, 'pieza') AS unidad,
                COALESCE(i.stock_bodega, 0) AS stock_bodega,
                COALESCE(i.stock, 0) AS stock_distribuido,
                COALESCE(i.stock_cocina, 0) AS stock_cocina,
                COALESCE(i.stock_barra, 0) AS stock_barra,
                COALESCE(i.stock_minimo, 0) AS stock_minimo,
                i.es_bebida,
                COALESCE((
                    SELECT AVG(rh.costo_total / NULLIF(rh.cantidad, 0))
                    FROM restock_history rh
                    WHERE rh.insumo_id = i.id
                ), 0) AS costo_promedio
            FROM insumo i
            WHERE i.tenant_id = :tenantId
              AND i.is_active = true
            ORDER BY i.nombre ASC
            """, nativeQuery = true)
    List<Object[]> listarInsumosConStock(@Param("tenantId") Long tenantId);

    /**
     * Obtiene productos de menu que tengan configurado stock minimo.
     *
     * 0: id (Long)
     * 1: nombre (String)
     * 2: unidad (String)
     * 3: stock (Double)
     * 4: stock_minimo (Double)
     * 5: precio (BigDecimal)
     * 6: es_sub_receta (Boolean)
     */
    @Query(value = """
            SELECT
                p.id,
                p.nombre,
                COALESCE(p.unidad, 'pieza') AS unidad,
                COALESCE(p.stock, 0) AS stock,
                COALESCE(p.stock_minimo, 0) AS stock_minimo,
                COALESCE(p.precio, 0) AS precio,
                COALESCE(p.es_sub_receta, false) AS es_sub_receta
            FROM tenant_menu_product p
            JOIN tenant_menu_category c ON c.id = p.category_id
            WHERE c.tenant_id = :tenantId
              AND p.stock_minimo > 0
            ORDER BY p.nombre ASC
            """, nativeQuery = true)
    List<Object[]> listarProductosConStockMinimo(@Param("tenantId") Long tenantId);
}
