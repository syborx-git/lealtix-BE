-- =====================================================
-- V63: Sub-recetas como insumo producido (tamaño de lote y porciones en platillos)
-- =====================================================

-- Rendimiento estándar por lote de producción de la sub-receta (ej. 1000 ml)
ALTER TABLE tenant_menu_product 
    ADD COLUMN IF NOT EXISTS tamano_lote DOUBLE PRECISION DEFAULT 1.0;

-- Cantidad y unidad que el platillo consume de la sub-receta (ej. 120 ml)
ALTER TABLE product_sub_receta 
    ADD COLUMN IF NOT EXISTS cantidad DOUBLE PRECISION DEFAULT 1.0;

ALTER TABLE product_sub_receta 
    ADD COLUMN IF NOT EXISTS unidad VARCHAR(20);

-- Actualizar registros existentes para tener consistencia
UPDATE tenant_menu_product
SET tamano_lote = 1.0
WHERE es_sub_receta = TRUE AND (tamano_lote IS NULL OR tamano_lote <= 0);

UPDATE product_sub_receta
SET cantidad = 1.0
WHERE cantidad IS NULL OR cantidad <= 0;
