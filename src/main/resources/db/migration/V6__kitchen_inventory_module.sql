-- =====================================================================
-- V6: Módulo Cocina e Inventario (consolidado)
-- Consolida: V34, V38, V39, V40, V44, V37
-- =====================================================================

-- -----------------------------------------------------------------------
-- 1. Modificaciones a product_additional (V34)
-- -----------------------------------------------------------------------
ALTER TABLE product_additional
    ADD COLUMN IF NOT EXISTS precio DECIMAL(10,2) DEFAULT 0.00;

-- -----------------------------------------------------------------------
-- 2. Modificaciones a insumo (V38)
-- -----------------------------------------------------------------------
ALTER TABLE insumo
    ADD COLUMN IF NOT EXISTS es_bebida    BOOLEAN    NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS precio_venta NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS producto_id  BIGINT;
-- producto_id: enlace al tenant_menu_product (solo para bebidas vendibles en Comandix)

-- -----------------------------------------------------------------------
-- 3. Tablas puente de multicategoría (V39)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tenant_menu_product_category (
    product_id  BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (product_id, category_id),
    CONSTRAINT fk_mpc_product  FOREIGN KEY (product_id)  REFERENCES tenant_menu_product(id)  ON DELETE CASCADE,
    CONSTRAINT fk_mpc_category FOREIGN KEY (category_id) REFERENCES tenant_menu_category(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS insumo_category (
    insumo_id   BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (insumo_id, category_id),
    CONSTRAINT fk_ic_insumo   FOREIGN KEY (insumo_id)   REFERENCES insumo(id)               ON DELETE CASCADE,
    CONSTRAINT fk_ic_category FOREIGN KEY (category_id) REFERENCES tenant_menu_category(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mpc_product  ON tenant_menu_product_category(product_id);
CREATE INDEX IF NOT EXISTS idx_mpc_category ON tenant_menu_product_category(category_id);
CREATE INDEX IF NOT EXISTS idx_ic_insumo    ON insumo_category(insumo_id);
CREATE INDEX IF NOT EXISTS idx_ic_category  ON insumo_category(category_id);

-- -----------------------------------------------------------------------
-- 4. Modificaciones a tenant_menu_product (V40 + V44)
-- -----------------------------------------------------------------------
ALTER TABLE tenant_menu_product
    ADD COLUMN IF NOT EXISTS auto_availability BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS es_sub_receta     BOOLEAN DEFAULT FALSE;

-- -----------------------------------------------------------------------
-- 5. Tabla product_sub_receta (V44)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_sub_receta (
    id               BIGSERIAL PRIMARY KEY,
    dish_product_id  BIGINT    NOT NULL,
    sub_receta_id    BIGINT    NOT NULL,
    CONSTRAINT fk_psr_dish      FOREIGN KEY (dish_product_id) REFERENCES tenant_menu_product(id),
    CONSTRAINT fk_psr_subreceta FOREIGN KEY (sub_receta_id)   REFERENCES tenant_menu_product(id),
    CONSTRAINT uk_product_sub_receta UNIQUE (dish_product_id, sub_receta_id)
);

CREATE INDEX IF NOT EXISTS idx_psr_dish      ON product_sub_receta(dish_product_id);
CREATE INDEX IF NOT EXISTS idx_psr_subreceta ON product_sub_receta(sub_receta_id);

-- -----------------------------------------------------------------------
-- 6. Tabla restock_history (V37)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS restock_history (
    id            BIGSERIAL     PRIMARY KEY,
    tenant_id     BIGINT        NOT NULL,
    insumo_id     BIGINT,
    insumo_nombre VARCHAR(100),
    cantidad      NUMERIC(14,2) NOT NULL DEFAULT 0,
    costo_total   NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_restock_history_tenant FOREIGN KEY (tenant_id)  REFERENCES tenant(id)  ON DELETE CASCADE,
    CONSTRAINT fk_restock_history_insumo FOREIGN KEY (insumo_id)  REFERENCES insumo(id)  ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_restock_history_tenant         ON restock_history(tenant_id);
CREATE INDEX IF NOT EXISTS idx_restock_history_created        ON restock_history(created_at);
CREATE INDEX IF NOT EXISTS idx_restock_history_tenant_created ON restock_history(tenant_id, created_at);
