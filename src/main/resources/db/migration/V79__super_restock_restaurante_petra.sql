-- ============================================================================
-- V79: Super restock masivo para pruebas continuas en Restaurante Petra
-- Asigna inventario prácticamente ilimitado a todos los insumos, sub-recetas,
-- platillos y bebidas para pruebas de comandas sin agotamiento de stock.
-- ============================================================================

DO $$
DECLARE
    v_petra_ids BIGINT[];
BEGIN
    -- Identificar todos los tenant_ids asociados a Restaurante Petra
    SELECT ARRAY_AGG(DISTINCT tid) INTO v_petra_ids
    FROM (
        SELECT id AS tid FROM tenant WHERE LOWER(slug) LIKE '%petra%' OR LOWER(nombre_negocio) LIKE '%petra%'
        UNION
        SELECT tenant_id AS tid FROM tenant_user WHERE LOWER(email) LIKE '%petra%'
        UNION
        SELECT t.id AS tid FROM tenant t JOIN app_user u ON u.id = t.user_id WHERE LOWER(u.email) LIKE '%petra%'
        UNION
        SELECT 1::BIGINT AS tid -- Tenant 1 es Petra por defecto
    ) sub
    WHERE tid IS NOT NULL;

    IF v_petra_ids IS NOT NULL AND ARRAY_LENGTH(v_petra_ids, 1) > 0 THEN

        -- 1. Insumos a nivel masivo (500 kg en cocina, 500 kg en bodega, 100 kg en barra)
        UPDATE insumo
        SET 
          stock_bodega = CASE 
            WHEN LOWER(TRIM(COALESCE(unidad, ''))) LIKE '%gram%' OR LOWER(TRIM(COALESCE(unidad, ''))) IN ('g', 'gr') THEN 500000.0
            WHEN LOWER(TRIM(COALESCE(unidad, ''))) LIKE '%mili%' OR LOWER(TRIM(COALESCE(unidad, ''))) IN ('ml', 'l', 'litro', 'litros') THEN 500000.0
            ELSE 10000.0
          END,
          stock_cocina = CASE 
            WHEN LOWER(TRIM(COALESCE(unidad, ''))) LIKE '%gram%' OR LOWER(TRIM(COALESCE(unidad, ''))) IN ('g', 'gr') THEN 500000.0
            WHEN LOWER(TRIM(COALESCE(unidad, ''))) LIKE '%mili%' OR LOWER(TRIM(COALESCE(unidad, ''))) IN ('ml', 'l', 'litro', 'litros') THEN 500000.0
            ELSE 10000.0
          END,
          stock_barra = CASE 
            WHEN LOWER(TRIM(COALESCE(unidad, ''))) LIKE '%gram%' OR LOWER(TRIM(COALESCE(unidad, ''))) IN ('g', 'gr') THEN 100000.0
            WHEN LOWER(TRIM(COALESCE(unidad, ''))) LIKE '%mili%' OR LOWER(TRIM(COALESCE(unidad, ''))) IN ('ml', 'l', 'litro', 'litros') THEN 100000.0
            ELSE 5000.0
          END,
          stock = CASE 
            WHEN LOWER(TRIM(COALESCE(unidad, ''))) LIKE '%gram%' OR LOWER(TRIM(COALESCE(unidad, ''))) IN ('g', 'gr') THEN 600000.0
            WHEN LOWER(TRIM(COALESCE(unidad, ''))) LIKE '%mili%' OR LOWER(TRIM(COALESCE(unidad, ''))) IN ('ml', 'l', 'litro', 'litros') THEN 600000.0
            ELSE 15000.0
          END
        WHERE tenant_id = ANY(v_petra_ids);

        -- 2. Sub-recetas preparadas a nivel masivo (5,000 a 50,000 unidades)
        UPDATE tenant_menu_product
        SET 
          stock = CASE 
            WHEN LOWER(TRIM(COALESCE(unidad, ''))) LIKE '%gram%' OR LOWER(TRIM(COALESCE(unidad, ''))) IN ('g', 'gr') THEN 50000.0
            WHEN LOWER(TRIM(COALESCE(unidad, ''))) LIKE '%mili%' OR LOWER(TRIM(COALESCE(unidad, ''))) IN ('ml', 'l', 'litro', 'litros') THEN 50000.0
            ELSE 5000.0
          END,
          is_active = TRUE
        WHERE category_id IN (SELECT id FROM tenant_menu_category WHERE tenant_id = ANY(v_petra_ids))
          AND es_sub_receta = TRUE;

        -- 3. Platillos y bebidas base a 5,000 unidades y todos activos
        UPDATE tenant_menu_product
        SET 
          stock = 5000.0,
          is_active = TRUE
        WHERE category_id IN (SELECT id FROM tenant_menu_category WHERE tenant_id = ANY(v_petra_ids))
          AND (es_sub_receta = FALSE OR es_sub_receta IS NULL);

        -- 4. Asegurar módulos habilitados
        UPDATE tenant
        SET 
          kitchen_module_enabled = TRUE,
          is_active = TRUE
        WHERE id = ANY(v_petra_ids);

    END IF;
END $$;
