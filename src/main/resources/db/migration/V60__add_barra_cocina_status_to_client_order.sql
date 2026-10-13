-- =====================================================================
-- V60: Separación de estados independientes para Barra y Cocina
-- Permite que una comanda mixta (platillos + bebidas) maneje el ciclo
-- de vida de preparación y despacho de forma independiente en cada área.
-- =====================================================================

ALTER TABLE client_order
    ADD COLUMN IF NOT EXISTS barra_estado VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS cocina_estado VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS barra_ready_at TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS cocina_ready_at TIMESTAMP NULL;

-- Backfill para órdenes históricas finalizadas o canceladas
UPDATE client_order
SET barra_estado = estado,
    cocina_estado = estado
WHERE estado IN ('LISTO', 'PAGADA', 'CANCELADA')
  AND (barra_estado IS NULL OR cocina_estado IS NULL);

-- Backfill para órdenes activas existentes (CONFIRMADA, EN_PREPARACION, PENDIENTE):
-- Inicializar ambos estados para mantener compatibilidad
UPDATE client_order
SET barra_estado = estado,
    cocina_estado = estado
WHERE estado IN ('CONFIRMADA', 'EN_PREPARACION', 'PENDIENTE')
  AND barra_estado IS NULL
  AND cocina_estado IS NULL;
