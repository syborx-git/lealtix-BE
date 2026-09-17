-- V15: Solicitudes de stock (Restock Requests) de sub-almacenes (Cocina/Barra) hacia Bodega central
CREATE TABLE IF NOT EXISTS stock_request (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    insumo_id BIGINT,
    insumo_nombre VARCHAR(120),
    area VARCHAR(20) NOT NULL,
    cantidad NUMERIC(14,3) NOT NULL DEFAULT 0,
    prioridad VARCHAR(20) NOT NULL DEFAULT 'MEDIA',
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_stock_request_tenant_estado ON stock_request (tenant_id, estado);
CREATE INDEX IF NOT EXISTS idx_stock_request_area ON stock_request (area);