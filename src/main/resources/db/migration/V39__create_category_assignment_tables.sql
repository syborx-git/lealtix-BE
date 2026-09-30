-- =====================================================
-- V39: Tablas puente para multicategoría
-- Fecha: 2026-09-05
-- Descripción: Un producto de menú (platillo/bebida) o un
--              insumo puede pertenecer a VARIAS categorías.
--              La columna category_id (categoría principal) se
--              conserva; estas tablas guardan la lista completa
--              de categorías (principal + extras).
-- =====================================================

CREATE TABLE IF NOT EXISTS tenant_menu_product_category (
    product_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (product_id, category_id),
    CONSTRAINT fk_mpc_product  FOREIGN KEY (product_id)  REFERENCES tenant_menu_product (id)  ON DELETE CASCADE,
    CONSTRAINT fk_mpc_category FOREIGN KEY (category_id) REFERENCES tenant_menu_category (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS insumo_category (
    insumo_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (insumo_id, category_id),
    CONSTRAINT fk_ic_insumo   FOREIGN KEY (insumo_id)   REFERENCES insumo (id)            ON DELETE CASCADE,
    CONSTRAINT fk_ic_category FOREIGN KEY (category_id) REFERENCES tenant_menu_category (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mpc_product  ON tenant_menu_product_category (product_id);
CREATE INDEX IF NOT EXISTS idx_mpc_category ON tenant_menu_product_category (category_id);
CREATE INDEX IF NOT EXISTS idx_ic_insumo    ON insumo_category (insumo_id);
CREATE INDEX IF NOT EXISTS idx_ic_category  ON insumo_category (category_id);