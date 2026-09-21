-- =====================================================
-- V13: Crear tabla stock_transfer_history
-- Fecha: 2026-09-16
-- Descripcion: Historial de transferencias de stock desde
--              bodega hacia los subalmacenes cocina/barra,
--              para el reporte de auditoria del apartado Reportes.
-- =====================================================

CREATE TABLE IF NOT EXISTS stock_transfer_history (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    insumo_id BIGINT,
    insumo_nombre VARCHAR(100),
    origen VARCHAR(20) NOT NULL DEFAULT 'bodega',
    destino VARCHAR(20),
    cantidad NUMERIC(14,3) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_stock_transfer_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_transfer_insumo FOREIGN KEY (insumo_id) REFERENCES insumo(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_stock_transfer_tenant ON stock_transfer_history(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_transfer_created ON stock_transfer_history(created_at);
CREATE INDEX IF NOT EXISTS idx_stock_transfer_tenant_created ON stock_transfer_history(tenant_id, created_at);