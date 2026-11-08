-- V74: Agregar área de distribución y campos de bebidas a tenant_menu_product, y retirar tablas legacy de bebida

-- 1. Agregar columnas a tenant_menu_product
ALTER TABLE tenant_menu_product ADD COLUMN IF NOT EXISTS area VARCHAR(30) NOT NULL DEFAULT 'COCINA';
ALTER TABLE tenant_menu_product ADD COLUMN IF NOT EXISTS tipo_bebida VARCHAR(30);
ALTER TABLE tenant_menu_product ADD COLUMN IF NOT EXISTS insumo_id BIGINT;

-- 2. Agregar columna area_distribucion a insumo
ALTER TABLE insumo ADD COLUMN IF NOT EXISTS area_distribucion VARCHAR(30) DEFAULT 'ALMACEN';

-- 3. Clave foránea opcional para insumo_id
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tenant_menu_product_insumo'
    ) THEN
        ALTER TABLE tenant_menu_product
            ADD CONSTRAINT fk_tenant_menu_product_insumo
            FOREIGN KEY (insumo_id) REFERENCES insumo (id) ON DELETE SET NULL;
    END IF;
END $$;

-- 4. Backfill de área en tenant_menu_product
-- 4.1 Bebidas enlazadas directamente a insumos con es_bebida=true
UPDATE tenant_menu_product p
SET area = 'BARRA', tipo_bebida = 'directa', insumo_id = i.id
FROM insumo i
WHERE i.producto_id = p.id AND i.es_bebida = true;

-- 4.2 Bebidas asociadas por recetas con insumo de bebida
UPDATE tenant_menu_product p
SET area = 'BARRA', tipo_bebida = 'directa', insumo_id = r.insumo_id
FROM product_recipe r
JOIN insumo i ON r.insumo_id = i.id
WHERE r.dish_product_id = p.id AND i.es_bebida = true AND (p.area IS NULL OR p.area = 'COCINA');

-- 4.3 Bebidas identificadas por categorías exclusivas de barra
UPDATE tenant_menu_product p
SET area = 'BARRA'
WHERE p.category_id IN (
    SELECT c.id FROM tenant_menu_category c
    WHERE LOWER(c.nombre) LIKE '%bebida%'
       OR LOWER(c.nombre) LIKE '%barra%'
       OR LOWER(c.nombre) LIKE '%cerveza%'
       OR LOWER(c.nombre) LIKE '%coctel%'
       OR LOWER(c.nombre) LIKE '%cocktail%'
       OR LOWER(c.nombre) LIKE '%vino%'
       OR LOWER(c.nombre) LIKE '%cafeter%'
);

-- 4.4 Sub-recetas asignadas a ALMACEN
UPDATE tenant_menu_product
SET area = 'ALMACEN'
WHERE es_sub_receta = true;

-- 4.5 Todo producto restante se establece a COCINA
UPDATE tenant_menu_product
SET area = 'COCINA'
WHERE area IS NULL;

-- 5. Backfill de area_distribucion en insumo
UPDATE insumo
SET area_distribucion = 'BARRA'
WHERE es_bebida = true;

UPDATE insumo
SET area_distribucion = 'ALMACEN'
WHERE area_distribucion IS NULL;

-- 6. Eliminar tablas legacy de bebidas
DROP TABLE IF EXISTS bebida_receta CASCADE;
DROP TABLE IF EXISTS bebida CASCADE;

-- 7. Índice para optimizar consultas de inventario y pedidos por área
CREATE INDEX IF NOT EXISTS idx_tenant_menu_product_area ON tenant_menu_product (area);

