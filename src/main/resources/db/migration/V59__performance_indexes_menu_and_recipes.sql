-- ============================================================================
-- V55: Indices de rendimiento para consultas de Menú, Recetas, Insumos y Comandas
-- ============================================================================

-- Recetas: Búsqueda de ingredientes por platillo y por insumo
CREATE INDEX IF NOT EXISTS idx_product_recipe_dish ON product_recipe(dish_product_id);
CREATE INDEX IF NOT EXISTS idx_product_recipe_insumo ON product_recipe(insumo_id);

-- Adicionales: Búsqueda por platillo e insumo
CREATE INDEX IF NOT EXISTS idx_product_additional_dish ON product_additional(dish_product_id);
CREATE INDEX IF NOT EXISTS idx_product_additional_insumo ON product_additional(insumo_id);

-- Cross Selling: Búsqueda de sugerencias por tenant y estado activo
CREATE INDEX IF NOT EXISTS idx_product_cross_selling_tenant_active 
    ON product_cross_selling(tenant_id, is_active);
CREATE INDEX IF NOT EXISTS idx_product_cross_selling_product_tenant 
    ON product_cross_selling(product_id, tenant_id);

-- Productos del menú: Búsqueda por categoría principal y estado
CREATE INDEX IF NOT EXISTS idx_tenant_menu_product_category 
    ON tenant_menu_product(category_id);
CREATE INDEX IF NOT EXISTS idx_tenant_menu_product_active 
    ON tenant_menu_product(is_active);

-- Insumos: Búsqueda por tenant
CREATE INDEX IF NOT EXISTS idx_insumo_tenant ON insumo(tenant_id);

-- Sub-recetas
CREATE INDEX IF NOT EXISTS idx_product_sub_receta_dish ON product_sub_receta(dish_product_id);
CREATE INDEX IF NOT EXISTS idx_product_sub_receta_sub ON product_sub_receta(sub_receta_id);
