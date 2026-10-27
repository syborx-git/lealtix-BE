-- =====================================================
-- V68: Agregar stock_bodega_remanente a stock_transfer_history
-- Fecha: 2026-10-19
-- Descripcion: Guarda el stock remanente en bodega al momento
--              de realizar una transferencia hacia cocina o barra.
-- =====================================================

ALTER TABLE stock_transfer_history
    ADD COLUMN IF NOT EXISTS stock_bodega_remanente NUMERIC(14,3);

-- Inicializar valores previos con el stock_bodega del insumo correspondiente
UPDATE stock_transfer_history st
SET stock_bodega_remanente = COALESCE(i.stock_bodega, 0)
FROM insumo i
WHERE st.insumo_id = i.id AND st.stock_bodega_remanente IS NULL;
