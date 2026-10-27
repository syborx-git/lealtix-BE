-- V67: Sub-recetas consideradas como insumos y Categorías de Cancelación para tickets cancelados

-- 1. Soporte de sub-recetas en tabla insumo
ALTER TABLE insumo ADD COLUMN IF NOT EXISTS es_sub_receta BOOLEAN NOT NULL DEFAULT FALSE;

-- Sincronizar sub-recetas existentes de tenant_menu_product a la tabla insumo
INSERT INTO insumo (tenant_id, nombre, unidad, producto_id, es_sub_receta, es_bebida, stock, stock_cocina, stock_bodega, stock_barra, stock_minimo, is_active, created_at, updated_at)
SELECT c.tenant_id, p.nombre, COALESCE(NULLIF(TRIM(p.unidad), ''), 'pieza'), p.id, TRUE, FALSE, COALESCE(p.stock, 0.0), COALESCE(p.stock, 0.0), 0.0, 0.0, COALESCE(p.stock_minimo, 0.0), TRUE, NOW(), NOW()
FROM tenant_menu_product p
JOIN tenant_menu_category c ON c.id = p.category_id
WHERE p.es_sub_receta = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM insumo i WHERE i.producto_id = p.id AND i.es_sub_receta = TRUE
  );

-- 2. Categorías y comentarios específicos de cancelación en client_order
ALTER TABLE client_order ADD COLUMN IF NOT EXISTS cancellation_category VARCHAR(100);
ALTER TABLE client_order ADD COLUMN IF NOT EXISTS cancellation_comment TEXT;

-- Clasificar órdenes canceladas históricas
UPDATE client_order
SET cancellation_category = CASE
    WHEN cancellation_reason ILIKE '%demora%' OR cancellation_reason ILIKE '%tard%' OR cancellation_reason ILIKE '%espera%' THEN 'Alto tiempo de espera'
    WHEN cancellation_reason ILIKE '%repetido%' OR cancellation_reason ILIKE '%duplicad%' OR cancellation_reason ILIKE '%error%' THEN 'Error de captura / comanda duplicada'
    WHEN cancellation_reason ILIKE '%desist%' OR cancellation_reason ILIKE '%retir%' OR cancellation_reason ILIKE '%quem%' THEN 'Cliente desistió / Cambio de opinión'
    WHEN cancellation_reason ILIKE '%agotad%' OR cancellation_reason ILIKE '%falta%' THEN 'Falta de insumos / Producto agotado'
    WHEN cancellation_reason ILIKE '%defect%' OR cancellation_reason ILIKE '%mal%' THEN 'Platillo o bebida defectuosa'
    ELSE 'Otro motivo'
END,
cancellation_comment = cancellation_reason
WHERE estado = 'CANCELADA' AND (cancellation_category IS NULL OR cancellation_category = '');
