-- V73: Agregar columna tiene_variantes a tenant_menu_product
ALTER TABLE tenant_menu_product ADD COLUMN IF NOT EXISTS tiene_variantes BOOLEAN DEFAULT FALSE;
