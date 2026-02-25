-- Migración V15: Agregar soporte para Venta Cruzada (Cross-Selling) de productos
-- Fecha: 2026-02-23
-- Descripción: Tabla para gestionar sugerencias de productos complementarios

-- 1. Crear tabla product_cross_selling
CREATE TABLE IF NOT EXISTS product_cross_selling (
    id BIGSERIAL PRIMARY KEY,
    
    -- Relaciones
    product_id BIGINT NOT NULL,
    suggested_product_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    
    -- Configuración
    display_order INTEGER DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Auditoría
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_product_cross_selling_product 
        FOREIGN KEY (product_id) REFERENCES tenant_menu_product(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_cross_selling_suggested 
        FOREIGN KEY (suggested_product_id) REFERENCES tenant_menu_product(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_cross_selling_tenant 
        FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT chk_product_cross_selling_different_products 
        CHECK (product_id != suggested_product_id),
    CONSTRAINT chk_product_cross_selling_display_order 
        CHECK (display_order > 0)
);

-- 2. Crear índices para optimizar consultas frecuentes
CREATE INDEX IF NOT EXISTS idx_product_cross_selling_product_id 
    ON product_cross_selling(product_id);

CREATE INDEX IF NOT EXISTS idx_product_cross_selling_tenant_id 
    ON product_cross_selling(tenant_id);

CREATE INDEX IF NOT EXISTS idx_product_cross_selling_active 
    ON product_cross_selling(product_id, tenant_id, is_active);

CREATE INDEX IF NOT EXISTS idx_product_cross_selling_order 
    ON product_cross_selling(product_id, display_order);

-- 3. Crear índice único para evitar sugerencias duplicadas
CREATE UNIQUE INDEX IF NOT EXISTS idx_product_cross_selling_unique 
    ON product_cross_selling(product_id, suggested_product_id, tenant_id);

-- 4. Comentarios para documentación
COMMENT ON TABLE product_cross_selling IS 'Gestiona las sugerencias de productos complementarios para venta cruzada';
COMMENT ON COLUMN product_cross_selling.product_id IS 'ID del producto principal que dispara la sugerencia';
COMMENT ON COLUMN product_cross_selling.suggested_product_id IS 'ID del producto sugerido como complemento';
COMMENT ON COLUMN product_cross_selling.tenant_id IS 'ID del tenant para aislamiento SaaS';
COMMENT ON COLUMN product_cross_selling.display_order IS 'Orden de visualización de las sugerencias';
COMMENT ON COLUMN product_cross_selling.is_active IS 'Indica si la sugerencia está activa';
