-- V11: Crear tabla client_order para almacenar las órdenes/comandas
-- Fecha: 2026-02-16
-- Descripción: Tabla de cabecera para órdenes de clientes con multi-tenancy

CREATE TABLE IF NOT EXISTS client_order (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Relaciones
    customer_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    
    -- Información de la orden
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    
    -- Montos
    subtotal NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    descuento NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    
    -- Auditoría
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_client_order_customer 
        FOREIGN KEY (customer_id) REFERENCES tenant_customer(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_order_tenant 
        FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT chk_client_order_estado 
        CHECK (estado IN ('PENDIENTE', 'PAGADA', 'CANCELADA')),
    CONSTRAINT chk_client_order_amounts 
        CHECK (subtotal >= 0 AND descuento >= 0 AND total >= 0)
);

-- Índices para mejorar rendimiento de búsquedas comunes
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_id ON client_order(tenant_id);
CREATE INDEX IF NOT EXISTS idx_client_order_customer_id ON client_order(customer_id);
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_customer ON client_order(tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_fecha ON client_order(tenant_id, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_estado ON client_order(tenant_id, estado);
CREATE INDEX IF NOT EXISTS idx_client_order_fecha ON client_order(fecha DESC);
