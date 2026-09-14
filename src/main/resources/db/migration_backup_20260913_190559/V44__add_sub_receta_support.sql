-- =====================================================
-- V44: Sub-recetas (preparaciones intermedias, ej. salsas)
-- Fecha: 2026-09-09
-- =====================================================

-- Marca productos como sub-receta (no se venden individualmente)
ALTER TABLE tenant_menu_product ADD COLUMN IF NOT EXISTS es_sub_receta BOOLEAN DEFAULT FALSE;

-- Asignación de sub-recetas a platillos o bebidas (sus insumos se suman al platillo)
CREATE TABLE IF NOT EXISTS product_sub_receta (
    id BIGSERIAL PRIMARY KEY,
    dish_product_id BIGINT NOT NULL,
    sub_receta_id BIGINT NOT NULL,
    CONSTRAINT fk_psr_dish FOREIGN KEY (dish_product_id) REFERENCES tenant_menu_product(id),
    CONSTRAINT fk_psr_subreceta FOREIGN KEY (sub_receta_id) REFERENCES tenant_menu_product(id),
    CONSTRAINT uk_product_sub_receta UNIQUE (dish_product_id, sub_receta_id)
);

CREATE INDEX IF NOT EXISTS idx_psr_dish ON product_sub_receta(dish_product_id);
CREATE INDEX IF NOT EXISTS idx_psr_subreceta ON product_sub_receta(sub_receta_id);