package com.lealtixservice.repository;

import com.lealtixservice.entity.ClientOrder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Consultas analiticas del reporte 1.1 (Dashboard de Ventas y Tendencias).
 *
 * Criterio de negocio transversal: se excluyen las ordenes PENDIENTE (aun no
 * enviadas a cocina) y CANCELADA, porque no son venta. El resto de estados
 * (CONFIRMADA, PAGADA, EN_PREPARACION, LISTO) si representan venta.
 * La columna de fecha de negocio es client_order.fecha, no created_at.
 */
/**
 * Solo se expone el marker de Spring Data: este repositorio no duplica el CRUD
 * de ClientOrderRepository, unicamente las consultas analiticas del reporte.
 */
@Repository
public interface ReporteVentasRepository extends org.springframework.data.repository.Repository<ClientOrder, UUID> {

    String ESTADOS_VENTA = "'CONFIRMADA','PAGADA','EN_PREPARACION','LISTO'";

    /**
     * Filtro de estados de venta, compartido por las cuatro consultas.
     *
     * Vive fuera de los text blocks a proposito: concatenar un identificador
     * dentro de un text block obliga a cerrar y reabrir el bloque en la misma
     * linea, y un "el segundo \"\"\" que abre bloque tiene que ir seguido de salto
     * de linea. Mezclar las dos cosas es facil de estropear y el fallo aparece
     * como error de compilacion, no como SQL mal formado.
     */
    String FILTRO_ESTADOS = " AND o.estado IN (" + ESTADOS_VENTA + ")";

    /**
     * KPIs de cabecera: volumen, ingresos brutos/netos, descuentos,
     * ticket promedio, clientes unicos e importe cobrado.
     *
     * Se declara como List<Object[]> y no como Object[] a proposito: en una query
     * nativa el tipo Object[] no se resuelve de forma fiable en Spring Data y el
     * servicio terminaba leyendo una fila vacia (KPIs en cero) mientras las
     * tablas de detalle si mostraban los importes.
     */
    @Query(value = """
            SELECT
                COUNT(*)                                                          AS ordenes,
                COALESCE(SUM(o.subtotal), 0)                                      AS bruto,
                COALESCE(SUM(o.descuento), 0)                                     AS descuentos,
                COALESCE(SUM(o.total), 0)                                         AS neto,
                COALESCE(AVG(o.total), 0)                                         AS ticket_promedio,
                COUNT(DISTINCT o.customer_id)                                     AS clientes,
                COALESCE(SUM(CASE WHEN o.paid_method IS NOT NULL THEN o.total ELSE 0 END), 0) AS cobrado
            FROM client_order o
            WHERE o.tenant_id = :tenantId
              AND o.fecha >= :from AND o.fecha < :toExclusivo
            """ + FILTRO_ESTADOS, nativeQuery = true)
    List<Object[]> resumenVentas(
            @Param("tenantId") Long tenantId,
            @Param("from") LocalDateTime from,
            @Param("toExclusivo") LocalDateTime toExclusivo
    );

    /**
     * Serie temporal para la grafica de tendencias.
     * date_trunc permite dia, semana o mes con la misma consulta.
     */
    @Query(value = """
            SELECT
                CAST(date_trunc(:granularidad, o.fecha) AS DATE) AS periodo,
                COUNT(*)                                            AS ordenes,
                COALESCE(SUM(o.subtotal), 0)                       AS bruto,
                COALESCE(SUM(o.total), 0)                           AS neto
            FROM client_order o
            WHERE o.tenant_id = :tenantId
              AND o.fecha >= :from AND o.fecha < :toExclusivo
            """ + FILTRO_ESTADOS + """
            GROUP BY periodo
            ORDER BY periodo
            """, nativeQuery = true)
    List<Object[]> serieVentas(
            @Param("tenantId") Long tenantId,
            @Param("from") LocalDateTime from,
            @Param("toExclusivo") LocalDateTime toExclusivo,
            @Param("granularidad") String granularidad
    );

    /**
     * Ventas por categoria de producto.
     * La categoria se resuelve por la tabla puente y, si el producto no tiene
     * asignacion en ella, se cae a tenant_menu_product.category_id.
     */
    @Query(value = """
            SELECT
                COALESCE(c.nombre, 'Sin categoria')  AS categoria,
                SUM(i.cantidad)                        AS unidades,
                SUM(i.cantidad * i.precio_unitario)    AS ingresos
            FROM client_order_item i
            JOIN client_order o ON o.id = i.order_id
            JOIN tenant_menu_product p ON p.id = i.product_id
            LEFT JOIN tenant_menu_product_category pc ON pc.product_id = p.id
            LEFT JOIN tenant_menu_category c ON c.id = COALESCE(pc.category_id, p.category_id)
            WHERE o.tenant_id = :tenantId
              AND o.fecha >= :from AND o.fecha < :toExclusivo
            """ + FILTRO_ESTADOS + """
            GROUP BY COALESCE(c.nombre, 'Sin categoria')
            ORDER BY ingresos DESC
            """, nativeQuery = true)
    List<Object[]> ventasPorCategoria(
            @Param("tenantId") Long tenantId,
            @Param("from") LocalDateTime from,
            @Param("toExclusivo") LocalDateTime toExclusivo
    );

    /**
     * Productos mas vendidos por ingresos, para la tabla de detalle y el Excel.
     */
    @Query(value = """
            SELECT
                p.nombre                                  AS producto,
                SUM(i.cantidad)                           AS unidades,
                SUM(i.cantidad * i.precio_unitario)       AS ingresos
            FROM client_order_item i
            JOIN client_order o ON o.id = i.order_id
            JOIN tenant_menu_product p ON p.id = i.product_id
            WHERE o.tenant_id = :tenantId
              AND o.fecha >= :from AND o.fecha < :toExclusivo
            """ + FILTRO_ESTADOS + """
            GROUP BY p.nombre
            ORDER BY ingresos DESC
            LIMIT :limite
            """, nativeQuery = true)
    List<Object[]> topProductos(
            @Param("tenantId") Long tenantId,
            @Param("from") LocalDateTime from,
            @Param("toExclusivo") LocalDateTime toExclusivo,
            @Param("limite") int limite
    );
}
