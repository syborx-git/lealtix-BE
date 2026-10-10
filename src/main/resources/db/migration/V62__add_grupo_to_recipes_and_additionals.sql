-- V62: Agregar campo 'grupo' a product_recipe y product_additional
-- Permite agrupar modificables y adicionales por sección (ej. Proteína, Salsas, Guarnición, Toppings)

ALTER TABLE product_recipe ADD COLUMN IF NOT EXISTS grupo VARCHAR(100);
ALTER TABLE product_additional ADD COLUMN IF NOT EXISTS grupo VARCHAR(100);
