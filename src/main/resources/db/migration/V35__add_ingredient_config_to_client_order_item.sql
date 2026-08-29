-- V35: Persistir configuración de ingredientes por ítem de orden
-- Fecha: 2026-08-29
-- Descripción: Guarda los IDs excluidos/adicionales de cada ítem para poder
-- revertir el descuento de inventario cuando se cancela la comanda.

ALTER TABLE client_order_item ADD COLUMN IF NOT EXISTS excluded_ingredient_ids TEXT;
ALTER TABLE client_order_item ADD COLUMN IF NOT EXISTS additional_ingredient_ids TEXT;