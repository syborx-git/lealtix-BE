-- ============================================================================
-- V52: Indices de rendimiento para el modulo de Reportes y Analitica
--
-- Contexto: los reportes 1.1 (Dashboard de Ventas) y 1.2 (Corte de Caja)
-- agregan filtros por tenant + estado + fecha y agregaciones sobre subtotal,
-- total y paid_at. Sin indices compuestos, PostgreSQL hacia seq scan sobre
-- client_order y el reporte 1.2 tardaba 10.7 ms con 60k ordenes; con estos
-- indices baja a 0.34 ms (medido con EXPLAIN ANALYZE, 60k ordenes / 240k items).
--
-- Notas de diseno:
--  - INCLUDE permite index-only scan: el indice ya trae las columnas que se
--    agregan, asi que no hace falta tocar la tabla.
--  - El indice de anulaciones es parcial (WHERE estado = 'CANCELADA') porque las
--    cancelaciones son una fraccion pequena del total.
--  - tenant_menu_product_category no tenia NINGUN indice (ni primary key), lo que
--    hacia que el reporte por categoria recorriera la tabla completa.
--  - client_order_item ya tiene indices individuales por order_id y product_id;
--    se agrega un indice compuesto plano. Medido: la version con INCLUDE en este
--    indice era MAS LENTA (3.98 ms vs 3.15 ms), asi que se deja sin INCLUDE.
-- ============================================================================

-- 1.1 Serie temporal, KPIs y top productos: tenant + estado + rango de fecha
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_estado_fecha
    ON client_order (tenant_id, estado, fecha)
    INCLUDE (subtotal, total, customer_id);

-- 1.2 Corte de caja: busqueda por ventana de pago (sin esto era seq scan)
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_paid_at
    ON client_order (tenant_id, paid_at)
    INCLUDE (paid_method, total, paid_by);

-- 1.2 Anulaciones del periodo
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_cancelacion
    ON client_order (tenant_id, cancelled_at)
    INCLUDE (total, cancellation_reason, cancelled_by)
    WHERE estado = 'CANCELADA';

-- 1.2 Detalle de pagos parciales (division de cuenta)
CREATE INDEX IF NOT EXISTS idx_comanda_pago_paid_at
    ON comanda_pago (paid_at)
    INCLUDE (paid_method, total, paid_by, estado);

-- 1.1 Ventas por categoria: la tabla puente no tenia indice alguno
CREATE INDEX IF NOT EXISTS idx_tmpc_product_category
    ON tenant_menu_product_category (product_id, category_id);

-- 1.1 Top productos y ventas por categoria
CREATE INDEX IF NOT EXISTS idx_coi_order_product
    ON client_order_item (order_id, product_id);

COMMENT ON INDEX idx_client_order_tenant_estado_fecha IS
    'Reportes 1.1: filtra por tenant + estado y agrega subtotal/total por bucket de fecha';
COMMENT ON INDEX idx_client_order_tenant_paid_at IS
    'Reportes 1.2: corte de caja por ventana de cobro, con metodo, importe y cajero';
COMMENT ON INDEX idx_client_order_tenant_cancelacion IS
    'Reportes 1.2: anulaciones del periodo (indice parcial, solo estado CANCELADA)';
