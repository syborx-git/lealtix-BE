-- =====================================================================
-- V70: Agregar soporte para rondas en client_order_item
-- Permite agregar nuevos productos a una comanda abierta (Ronda 2, Ronda 3...)
-- enviándolos como un nuevo ticket a cocina/barra bajo la misma comanda.
-- =====================================================================

ALTER TABLE client_order_item ADD COLUMN IF NOT EXISTS ronda INTEGER NOT NULL DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_client_order_item_ronda ON client_order_item(order_id, ronda);

COMMENT ON COLUMN client_order_item.ronda IS 'Número de ronda en que se ordenó el ítem (1 = comanda inicial, 2+ = adiciones/rondas posteriores)';
