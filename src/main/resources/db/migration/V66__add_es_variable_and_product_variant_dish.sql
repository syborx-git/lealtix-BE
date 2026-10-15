-- V66: Casilla es_variable en tenant_menu_product y tabla de variantes entre platillos (product_variant_dish)
ALTER TABLE tenant_menu_product ADD COLUMN IF NOT EXISTS es_variable BOOLEAN DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS product_variant_dish (
    id BIGSERIAL PRIMARY KEY,
    dish_product_id BIGINT NOT NULL REFERENCES tenant_menu_product(id) ON DELETE CASCADE,
    variant_dish_id BIGINT NOT NULL REFERENCES tenant_menu_product(id) ON DELETE CASCADE,
    precio NUMERIC(10, 2),
    cantidad DOUBLE PRECISION DEFAULT 1.0,
    unidad VARCHAR(20) DEFAULT 'pieza',
    grupo VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_product_variant_dish_parent ON product_variant_dish(dish_product_id);
CREATE INDEX IF NOT EXISTS idx_product_variant_dish_child ON product_variant_dish(variant_dish_id);
