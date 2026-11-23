-- V81: Agregar bandera disponible_kiosko para productos vendibles en Kiosko / Autoservicio

-- 1. Agregar columna disponible_kiosko por defecto en false
ALTER TABLE tenant_menu_product 
ADD COLUMN IF NOT EXISTS disponible_kiosko BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Índice para consultas y filtrado eficiente de artículos para kiosko
CREATE INDEX IF NOT EXISTS idx_tenant_menu_product_kiosk 
ON tenant_menu_product (category_id, disponible_kiosko) 
WHERE disponible_kiosko = TRUE;

-- 3. Activar demostración inicial solicitada:
--    Platillos: DESAYUNOS y ENCHILADAS
--    Bebidas: BEBIDAS y CERVEZA
UPDATE tenant_menu_product p
SET disponible_kiosko = TRUE
FROM tenant_menu_category c
WHERE (p.category_id = c.id OR EXISTS (
    SELECT 1 FROM tenant_menu_product_category pc 
    WHERE pc.product_id = p.id AND pc.category_id = c.id
))
AND (
    UPPER(c.nombre) LIKE '%DESAYUNO%' OR 
    UPPER(c.nombre) LIKE '%ENCHILADA%' OR 
    UPPER(c.nombre) = 'BEBIDAS' OR 
    UPPER(c.nombre) LIKE '%CERVEZA%'
)
AND p.is_active = TRUE
AND (p.es_sub_receta = FALSE OR p.es_sub_receta IS NULL);
