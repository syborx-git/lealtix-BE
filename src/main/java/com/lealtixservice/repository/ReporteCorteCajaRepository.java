package com.lealtixservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reporte 1.2 - Corte de Caja.
 *
 * FUENTE DE VERDAD DE LOS PAGOS
 * -----------------------------
 * El sistema registra el cobro de dos formas distintas y por eso NO se puede
 * consultar solo client_order.paid_method:
 *
 *  1. Pago simple (ClientOrderServiceImpl): solo se actualizan las columnas
 *     paid_method / paid_at / paid_by de client_order. No genera filas de
 *     comanda_pago.
 *  2. Division de cuenta (ComandaAsientoServiceImpl): se genera UNA fila por
 *     asiento pagado en comanda_pago, cada una con su propio metodo, importe y
 *     cajero. Si los asientos se pagan con metodos distintos, el paid_method de
 *     client_order solo conserva el ultimo, por lo que el desglose real vive en
 *     comanda_pago.
 *
 * Por eso todas las consultas parten de un CTE "pagos" con UNION ALL: filas de
 * comanda_pago mas las ordenes pagadas que NO tienen ninguna fila en comanda_pago.
 * Asi el total cobrado nunca se duplica.
 *
 * NOTA sobre propinas: viven en dos columnas segun como se cobro la cuenta,
 * client_order.propina (pago directo) y comanda_pago.propina (division de
 * cuenta). Se suman en la misma CTE para reportarlas una sola vez, y se exponen
 * aparte del total cobrado porque la propina no es ingreso del restaurante.
 *
 * RANGOS: todos los metodos reciben "to" ya convertido a limite EXCLUSIVO
 * (medianoche del dia siguiente) con DateRangeResolver.aExclusivo(...), y filtran
 * "columna >= :from AND columna < :toExclusivo". Nunca usar BETWEEN: deja fuera los
 * registros con fraccion de segundo dentro del ultimo dia.
 */
@Repository
public interface ReporteCorteCajaRepository extends JpaRepository<com.lealtixservice.entity.ClientOrder, java.util.UUID> {

    /**
     * Totales del corte: cobrado total, desglose por metodo, propinas, operaciones
     * y cajeros.
     * Indices: 0 total, 1 efectivo, 2 tarjeta, 3 transferencia, 4 mixto,
     * 5 propinas, 6 operaciones, 7 cajeros distintos.
     */
    @Query(value = """
            WITH pagos AS (
                SELECT cp.paid_method AS metodo, cp.total AS monto, cp.paid_by AS cajero,
                       COALESCE(cp.propina, 0) AS propina
                FROM comanda_pago cp
                JOIN client_order co ON co.id = cp.order_id
                WHERE co.tenant_id = :tenantId
                  AND cp.estado = 'PAGADA'
                  AND cp.paid_at IS NOT NULL
                  AND cp.paid_at >= :from AND cp.paid_at < :toExclusivo
                UNION ALL
                SELECT co.paid_method, co.total, co.paid_by,
                       COALESCE(co.propina, 0)
                FROM client_order co
                WHERE co.tenant_id = :tenantId
                  AND co.paid_at IS NOT NULL
                  AND co.paid_at >= :from AND co.paid_at < :toExclusivo
                  AND NOT EXISTS (SELECT 1 FROM comanda_pago cp2 WHERE cp2.order_id = co.id)
            )
            SELECT COALESCE(SUM(monto), 0)                              AS total_cobrado,
                   COALESCE(SUM(CASE WHEN metodo = 'CASH'     THEN monto ELSE 0 END), 0) AS efectivo,
                   COALESCE(SUM(CASE WHEN metodo = 'CARD'     THEN monto ELSE 0 END), 0) AS tarjeta,
                   COALESCE(SUM(CASE WHEN metodo = 'TRANSFER' THEN monto ELSE 0 END), 0) AS transferencia,
                   COALESCE(SUM(CASE WHEN metodo = 'MIXED'    THEN monto ELSE 0 END), 0) AS mixto,
                   COALESCE(SUM(propina), 0)                            AS propinas,
                   COUNT(*)                                             AS operaciones,
                   COUNT(DISTINCT cajero)                               AS cajeros
            FROM pagos
            """, nativeQuery = true)
    /**
     * Resumen agregado del periodo.
     * Indices: 0 total, 1 efectivo, 2 tarjeta, 3 transferencia, 4 mixto, 5 propinas,
     * 6 operaciones, 7 cajeros.
     *
     * Se declara como List<Object[]> y no como Object[] a proposito: en una query
     * nativa, el tipo Object[] no se resuelve de forma fiable en Spring Data y el
     * servicio terminaba leyendo una fila vacia (KPIs en cero) mientras las
     * tablas por metodo si mostraban los importes.
     */
    List<Object[]> resumenCorte(@Param("tenantId") Long tenantId,
                                @Param("from") LocalDateTime from,
                                @Param("toExclusivo") LocalDateTime toExclusivo);

    /**
     * Desglose por metodo de pago.
     * Indices: 0 metodo, 1 operaciones, 2 total, 3 propinas.
     */
    @Query(value = """
            WITH pagos AS (
                SELECT cp.paid_method AS metodo, cp.total AS monto, cp.paid_by AS cajero,
                       COALESCE(cp.propina, 0) AS propina
                FROM comanda_pago cp
                JOIN client_order co ON co.id = cp.order_id
                WHERE co.tenant_id = :tenantId
                  AND cp.estado = 'PAGADA'
                  AND cp.paid_at IS NOT NULL
                  AND cp.paid_at >= :from AND cp.paid_at < :toExclusivo
                UNION ALL
                SELECT co.paid_method, co.total, co.paid_by,
                       COALESCE(co.propina, 0)
                FROM client_order co
                WHERE co.tenant_id = :tenantId
                  AND co.paid_at IS NOT NULL
                  AND co.paid_at >= :from AND co.paid_at < :toExclusivo
                  AND NOT EXISTS (SELECT 1 FROM comanda_pago cp2 WHERE cp2.order_id = co.id)
            )
            SELECT COALESCE(metodo, 'SIN_METODO') AS metodo,
                   COUNT(*)                        AS operaciones,
                   COALESCE(SUM(monto), 0)         AS total,
                   COALESCE(SUM(propina), 0)       AS propinas
            FROM pagos
            GROUP BY COALESCE(metodo, 'SIN_METODO')
            ORDER BY total DESC
            """, nativeQuery = true)
    List<Object[]> pagosPorMetodo(@Param("tenantId") Long tenantId,
                                  @Param("from") LocalDateTime from,
                                  @Param("toExclusivo") LocalDateTime toExclusivo);

    /**
     * Desglose por cajero, que es la base de la conciliacion: quien cobro y cuanto
     * por cada metodo. Indices: 0 nombre, 1 email, 2 operaciones, 3 efectivo,
     * 4 tarjeta, 5 transferencia, 6 mixto, 7 total, 8 propinas.
     */
    @Query(value = """
            WITH pagos AS (
                SELECT cp.paid_method AS metodo, cp.total AS monto, cp.paid_by AS cajero,
                       COALESCE(cp.propina, 0) AS propina
                FROM comanda_pago cp
                JOIN client_order co ON co.id = cp.order_id
                WHERE co.tenant_id = :tenantId
                  AND cp.estado = 'PAGADA'
                  AND cp.paid_at IS NOT NULL
                  AND cp.paid_at >= :from AND cp.paid_at < :toExclusivo
                UNION ALL
                SELECT co.paid_method, co.total, co.paid_by,
                       COALESCE(co.propina, 0)
                FROM client_order co
                WHERE co.tenant_id = :tenantId
                  AND co.paid_at IS NOT NULL
                  AND co.paid_at >= :from AND co.paid_at < :toExclusivo
                  AND NOT EXISTS (SELECT 1 FROM comanda_pago cp2 WHERE cp2.order_id = co.id)
            )
            SELECT COALESCE(u.full_name, 'Sin cajero asignado')  AS nombre,
                   COALESCE(u.email, '')                        AS email,
                   COUNT(*)                                     AS operaciones,
                   COALESCE(SUM(CASE WHEN p.metodo = 'CASH'     THEN p.monto ELSE 0 END), 0) AS efectivo,
                   COALESCE(SUM(CASE WHEN p.metodo = 'CARD'     THEN p.monto ELSE 0 END), 0) AS tarjeta,
                   COALESCE(SUM(CASE WHEN p.metodo = 'TRANSFER' THEN p.monto ELSE 0 END), 0) AS transferencia,
                   COALESCE(SUM(CASE WHEN p.metodo = 'MIXED'    THEN p.monto ELSE 0 END), 0) AS mixto,
                   COALESCE(SUM(p.monto), 0)                    AS total,
                   COALESCE(SUM(p.propina), 0)                  AS propinas
            FROM pagos p
            LEFT JOIN app_user u ON u.id = p.cajero
            GROUP BY COALESCE(u.full_name, 'Sin cajero asignado'), COALESCE(u.email, '')
            ORDER BY total DESC
            """, nativeQuery = true)
    List<Object[]> pagosPorCajero(@Param("tenantId") Long tenantId,
                                  @Param("from") LocalDateTime from,
                                  @Param("toExclusivo") LocalDateTime toExclusivo);

    /**
     * Anulaciones del periodo, para cruzar el corte contra lo que se dio por perdido.
     * client_order no tiene columna de folio, asi que se usa el prefijo del UUID,
     * que es el mismo criterio que usa ComandaAsientoServiceImpl.
     *
     * El cast se escribe como CAST(... AS text) y no como "co.id::text": en una
     * @Query nativa, Hibernate parsea los dos puntos como inicio de un parametro
     * con nombre y la consulta revienta con un error de sintaxis en el servidor.
     *
     * El filtro de fecha NO usa COALESCE(cancelled_at, fecha) porque esa funcion
     * sobre la columna impide usar el indice (medido: 6.0 ms vs 0.15 ms con 60k
     * ordenes). Se desdobla en un OR: por cancelled_at, o por fecha cuando
     * cancelled_at viene nulo.
     *
     * Indices: 0 referencia, 1 fecha, 2 motivo, 3 usuario, 4 monto.
     */
    @Query(value = """
            SELECT UPPER(LEFT(CAST(co.id AS text), 8)) AS referencia,
                   COALESCE(co.cancelled_at, co.fecha) AS fecha,
                   COALESCE(co.cancellation_reason, 'Sin motivo registrado') AS motivo,
                   COALESCE(co.cancelled_by, '')     AS usuario,
                   COALESCE(co.total, 0)             AS monto
            FROM client_order co
            WHERE co.tenant_id = :tenantId
              AND co.estado = 'CANCELADA'
              AND (   (co.cancelled_at >= :from AND co.cancelled_at < :toExclusivo)
                   OR (co.cancelled_at IS NULL AND co.fecha >= :from AND co.fecha < :toExclusivo) )
            ORDER BY COALESCE(co.cancelled_at, co.fecha) DESC
            LIMIT :limite
            """, nativeQuery = true)
    List<Object[]> anulaciones(@Param("tenantId") Long tenantId,
                               @Param("from") LocalDateTime from,
                               @Param("toExclusivo") LocalDateTime toExclusivo,
                               @Param("limite") int limite);

    /**
     * Conteo y monto de ordenes canceladas en el periodo.
     * Mismo filtro sargable que anulaciones(), por la misma razon.
     * Indices: 0 cantidad, 1 monto.
     */
    @Query(value = """
            SELECT COUNT(*)          AS anulaciones,
                   COALESCE(SUM(co.total), 0) AS monto_anulado
            FROM client_order co
            WHERE co.tenant_id = :tenantId
              AND co.estado = 'CANCELADA'
              AND (   (co.cancelled_at >= :from AND co.cancelled_at < :toExclusivo)
                   OR (co.cancelled_at IS NULL AND co.fecha >= :from AND co.fecha < :toExclusivo) )
            """, nativeQuery = true)
    List<Object[]> resumenAnulaciones(@Param("tenantId") Long tenantId,
                                      @Param("from") LocalDateTime from,
                                      @Param("toExclusivo") LocalDateTime toExclusivo);
}
