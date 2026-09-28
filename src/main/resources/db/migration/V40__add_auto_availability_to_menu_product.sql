-- V40: control automático de disponibilidad por stock.
-- Sin esta columna el admin no podría excluir un producto del manejo automático.
ALTER TABLE tenant_menu_product ADD COLUMN auto_availability BOOLEAN NOT NULL DEFAULT TRUE;