-- V16: Normalización de bebidas (catálogo POS) — sin modificar la tabla insumo (materia prima).
-- Tipo de bebida: 'directa' (se vende por pieza con stock físico directo)
--                 'preparada' (disponibilidad calculada desde su receta vs stock de barra).

CREATE TABLE IF NOT EXISTS bebida (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500),
    precio_venta NUMERIC(12,2) NOT NULL DEFAULT 0,
    tipo_bebida VARCHAR(20) NOT NULL DEFAULT 'directa',
    unidad VARCHAR(20) NOT NULL DEFAULT 'pieza',
    -- Bebidas directas: insumo físico que se descuenta 1:1 con cada venta (ej. botella/lata).
    insumo_id BIGINT,
    stock_minimo NUMERIC(14,3) NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_bebida_tipo CHECK (tipo_bebida IN ('directa', 'preparada')),
    CONSTRAINT uq_bebida_tenant_nombre UNIQUE (tenant_id, nombre),
    CONSTRAINT fk_bebida_insumo FOREIGN KEY (insumo_id) REFERENCES insumo (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_bebida_tenant ON bebida (tenant_id);

-- Receta de bebidas preparadas: cantidad de cada insumo para preparar 1 pieza (solo tipo 'preparada').
CREATE TABLE IF NOT EXISTS bebida_receta (
    id BIGSERIAL PRIMARY KEY,
    bebida_id BIGINT NOT NULL,
    insumo_id BIGINT NOT NULL,
    cantidad NUMERIC(14,3) NOT NULL DEFAULT 1,
    modificable BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bebida_receta_bebida FOREIGN KEY (bebida_id) REFERENCES bebida (id) ON DELETE CASCADE,
    CONSTRAINT fk_bebida_receta_insumo FOREIGN KEY (insumo_id) REFERENCES insumo (id) ON DELETE CASCADE,
    CONSTRAINT uq_bebida_receta UNIQUE (bebida_id, insumo_id)
);

CREATE INDEX IF NOT EXISTS idx_bebida_receta_bebida ON bebida_receta (bebida_id);
CREATE INDEX IF NOT EXISTS idx_bebida_receta_insumo ON bebida_receta (insumo_id);