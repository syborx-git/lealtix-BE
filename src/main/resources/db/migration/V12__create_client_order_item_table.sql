-- V12: Crear tabla client_order_item para almacenar los detalles de las órdenes
-- Fecha: 2026-02-16
-- Descripción: Tabla de detalles de cada orden con información del producto, cantidad y precio

CREATE TABLE IF NOT EXISTS client_order_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Relaciones
    order_id UUID NOT NULL,
    product_id BIGINT NOT NULL,
    
    -- Información del item
    cantidad INTEGER NOT NULL,
    precio_unitario NUMERIC(10, 2) NOT NULL,
    comentarios TEXT,
    
    -- Auditoría
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_client_order_item_order 
        FOREIGN KEY (order_id) REFERENCES client_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_order_item_product 
        FOREIGN KEY (product_id) REFERENCES tenant_menu_product(id) ON DELETE RESTRICT,
    CONSTRAINT chk_client_order_item_cantidad 
        CHECK (cantidad > 0),
    CONSTRAINT chk_client_order_item_precio 
        CHECK (precio_unitario > 0)
);

-- Índices para mejorar rendimiento de búsquedas comunes
CREATE INDEX IF NOT EXISTS idx_client_order_item_order_id ON client_order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_client_order_item_product_id ON client_order_item(product_id);
CREATE INDEX IF NOT EXISTS idx_client_order_item_created_at ON client_order_item(created_at DESC);
