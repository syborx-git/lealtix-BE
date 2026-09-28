-- =====================================================
-- V53: Campos adicionales para mesa y product_sub_receta
-- - mesa: rotacion (orientacion sobre el plano del local)
-- - product_sub_receta: modificable y precio (sub-recetas)
-- =====================================================

ALTER TABLE mesa 
    ADD COLUMN IF NOT EXISTS rotacion INT DEFAULT 0;

ALTER TABLE product_sub_receta 
    ADD COLUMN IF NOT EXISTS modificable BOOLEAN DEFAULT FALSE;

ALTER TABLE product_sub_receta 
    ADD COLUMN IF NOT EXISTS precio NUMERIC(10,2);
