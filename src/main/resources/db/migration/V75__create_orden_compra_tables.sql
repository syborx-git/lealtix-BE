-- V75: Modulo de Restock Automatico y Lista de Compras (Ordenes de Compra)

CREATE TABLE IF NOT EXISTS orden_compra (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    folio VARCHAR(50) NOT NULL,
    titulo VARCHAR(150),
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    total_estimado NUMERIC(12,2) DEFAULT 0.00,
    total_real NUMERIC(12,2) DEFAULT 0.00,
    notas TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    surtida_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orden_compra_tenant ON orden_compra (tenant_id);
CREATE INDEX IF NOT EXISTS idx_orden_compra_estado ON orden_compra (tenant_id, estado);

CREATE TABLE IF NOT EXISTS orden_compra_item (
    id BIGSERIAL PRIMARY KEY,
    orden_compra_id BIGINT NOT NULL,
    insumo_id BIGINT NOT NULL,
    insumo_nombre VARCHAR(150) NOT NULL,
    area_destino VARCHAR(30) NOT NULL DEFAULT 'COCINA',
    unidad VARCHAR(30),
    stock_actual NUMERIC(12,3) DEFAULT 0.000,
    stock_minimo NUMERIC(12,3) DEFAULT 0.000,
    cantidad_sugerida NUMERIC(12,3) NOT NULL,
    cantidad_comprada NUMERIC(12,3),
    costo_unitario_estimado NUMERIC(12,2) DEFAULT 0.00,
    costo_unitario_real NUMERIC(12,2) DEFAULT 0.00,
    subtotal_estimado NUMERIC(12,2) DEFAULT 0.00,
    subtotal_real NUMERIC(12,2) DEFAULT 0.00,
    surtido BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_orden_compra_item_orden FOREIGN KEY (orden_compra_id) REFERENCES orden_compra (id) ON DELETE CASCADE,
    CONSTRAINT fk_orden_compra_item_insumo FOREIGN KEY (insumo_id) REFERENCES insumo (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_orden_compra_item_orden ON orden_compra_item (orden_compra_id);
CREATE INDEX IF NOT EXISTS idx_orden_compra_item_insumo ON orden_compra_item (insumo_id);

-- Enriquecer restock_history con orden_compra_id y area_destino
ALTER TABLE restock_history ADD COLUMN IF NOT EXISTS orden_compra_id BIGINT;
ALTER TABLE restock_history ADD COLUMN IF NOT EXISTS area_destino VARCHAR(30);

