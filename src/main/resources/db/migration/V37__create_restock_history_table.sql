-- =====================================================
-- V37: Crear tabla restock_history
-- Fecha: 2026-09-01
-- Descripción: Historial de reabastecimientos de insumos con
--              el costo total invertido, para el cálculo automático
--              de costos operacionales (materia prima) de los últimos 2 meses.
-- =====================================================

CREATE TABLE IF NOT EXISTS restock_history (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    insumo_id BIGINT,
    insumo_nombre VARCHAR(100),
    cantidad NUMERIC(14,2) NOT NULL DEFAULT 0,
    costo_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_restock_history_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_restock_history_insumo FOREIGN KEY (insumo_id) REFERENCES insumo(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_restock_history_tenant ON restock_history(tenant_id);
CREATE INDEX IF NOT EXISTS idx_restock_history_created ON restock_history(created_at);
CREATE INDEX IF NOT EXISTS idx_restock_history_tenant_created ON restock_history(tenant_id, created_at);
