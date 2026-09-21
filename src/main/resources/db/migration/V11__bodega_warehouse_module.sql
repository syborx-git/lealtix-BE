-- =====================================================================
-- V11: Módulo Bodega (almacén de insumos)
-- Bodega concentra el stock de entrada y distribuye a cocina/barra.
-- Se mantiene insumo.stock = distribuido (cocina + barra) para que el
-- POS (consumo/disponibilidad) siga dibujando de lo distribuido.
-- =====================================================================

-- -----------------------------------------------------------------------
-- 1. Columnas de ubicación en insumo
-- -----------------------------------------------------------------------
ALTER TABLE insumo
    ADD COLUMN IF NOT EXISTS stock_bodega DOUBLE PRECISION NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS stock_cocina  DOUBLE PRECISION NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS stock_barra   DOUBLE PRECISION NOT NULL DEFAULT 0;

-- -----------------------------------------------------------------------
-- 2. Backfill de datos existentes
--    El stock actual se asigna a cocina (insumos) o barra (bebidas),
--    manteniendo la invarianza stock = cocina + barra.
-- -----------------------------------------------------------------------
UPDATE insumo SET stock_cocina = stock WHERE es_bebida = FALSE AND stock_cocina = 0;
UPDATE insumo SET stock_barra  = stock WHERE es_bebida = TRUE  AND stock_barra  = 0;