-- Agregar precio del adicional a product_additional
-- precio: monto extra que se cobra al cliente cuando agrega este insumo

ALTER TABLE product_additional
ADD COLUMN precio DECIMAL(10, 2) DEFAULT 0.00;