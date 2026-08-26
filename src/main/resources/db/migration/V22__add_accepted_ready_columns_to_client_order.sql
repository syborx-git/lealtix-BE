-- =====================================================
-- V22: Agregar columnas de timestamps para estados de órdenes
-- Fecha: 2026-03-26
-- Descripción: Agrega columnas accepted_at y ready_at a client_order para tracking de transiciones de estado
-- =====================================================

-- Agregar columnas de timestamps para estados
ALTER TABLE client_order
ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP NULL,
ADD COLUMN IF NOT EXISTS ready_at TIMESTAMP NULL;

-- Crear índices para queries de órdenes aceptadas y listas
CREATE INDEX IF NOT EXISTS idx_client_order_accepted_at ON client_order(accepted_at);
CREATE INDEX IF NOT EXISTS idx_client_order_ready_at ON client_order(ready_at);
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_accepted_at ON client_order(tenant_id, accepted_at);
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_ready_at ON client_order(tenant_id, ready_at);
