-- V65: Soporte para variantes en recetas y adicionales
-- Permite que un producto tenga variantes seleccionables como cards tipo sub-comanda en Comandix

ALTER TABLE product_additional ADD COLUMN IF NOT EXISTS is_variant BOOLEAN DEFAULT FALSE;

-- Índice para consultas rápidas de variantes
CREATE INDEX IF NOT EXISTS idx_product_additional_is_variant ON product_additional(dish_product_id, is_variant);
