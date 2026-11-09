-- ============================================================================
-- V78: Restricción y valores por defecto para stock_minimo en insumos
-- Regla:
--   - stock_minimo NUNCA puede ser <= 0 o nulo.
--   - Sugerido / default según unidad:
--       * Gramos (gramos, g, gr): 500
--       * Mililitros (mililitros, ml, l, litro, litros): 500
--       * Pieza (pieza, pza, etc.) u otras: 10
-- ============================================================================

-- 1. Actualizar registros existentes en 'insumo' que tengan stock_minimo <= 0 o NULL
UPDATE insumo
SET stock_minimo = CASE
    WHEN LOWER(TRIM(COALESCE(unidad, ''))) LIKE '%gram%'
         OR LOWER(TRIM(COALESCE(unidad, ''))) IN ('g', 'gr') THEN 500.0
    WHEN LOWER(TRIM(COALESCE(unidad, ''))) LIKE '%mili%'
         OR LOWER(TRIM(COALESCE(unidad, ''))) IN ('ml', 'l', 'litro', 'litros') THEN 500.0
    ELSE 10.0
END
WHERE stock_minimo IS NULL OR stock_minimo <= 0;

-- 2. Actualizar sub-recetas en 'tenant_menu_product' que tengan stock_minimo <= 0 o NULL
UPDATE tenant_menu_product
SET stock_minimo = CASE
    WHEN LOWER(TRIM(COALESCE(unidad, ''))) LIKE '%gram%'
         OR LOWER(TRIM(COALESCE(unidad, ''))) IN ('g', 'gr') THEN 500.0
    WHEN LOWER(TRIM(COALESCE(unidad, ''))) LIKE '%mili%'
         OR LOWER(TRIM(COALESCE(unidad, ''))) IN ('ml', 'l', 'litro', 'litros') THEN 500.0
    ELSE 10.0
END
WHERE es_sub_receta = TRUE AND (stock_minimo IS NULL OR stock_minimo <= 0);

-- 3. Establecer valor default de la columna en la tabla 'insumo'
ALTER TABLE insumo ALTER COLUMN stock_minimo SET DEFAULT 10.0;

-- 4. Crear función de trigger para auto-asignar stock_minimo por defecto si el usuario o código envía <= 0 o NULL
CREATE OR REPLACE FUNCTION trg_insumo_enforce_stock_minimo_default()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.stock_minimo IS NULL OR NEW.stock_minimo <= 0 THEN
        IF LOWER(TRIM(COALESCE(NEW.unidad, ''))) LIKE '%gram%'
           OR LOWER(TRIM(COALESCE(NEW.unidad, ''))) IN ('g', 'gr') THEN
            NEW.stock_minimo := 500.0;
        ELSIF LOWER(TRIM(COALESCE(NEW.unidad, ''))) LIKE '%mili%'
           OR LOWER(TRIM(COALESCE(NEW.unidad, ''))) IN ('ml', 'l', 'litro', 'litros') THEN
            NEW.stock_minimo := 500.0;
        ELSE
            NEW.stock_minimo := 10.0;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 5. Crear trigger BEFORE INSERT OR UPDATE en 'insumo'
DROP TRIGGER IF EXISTS trg_insumo_stock_minimo_before_ins_upd ON insumo;

CREATE TRIGGER trg_insumo_stock_minimo_before_ins_upd
BEFORE INSERT OR UPDATE ON insumo
FOR EACH ROW
EXECUTE FUNCTION trg_insumo_enforce_stock_minimo_default();

-- 6. Agregar restricción CHECK para garantizar que stock_minimo siempre sea > 0
ALTER TABLE insumo
    DROP CONSTRAINT IF EXISTS chk_insumo_stock_minimo_gt_zero;

ALTER TABLE insumo
    ADD CONSTRAINT chk_insumo_stock_minimo_gt_zero CHECK (stock_minimo > 0);

