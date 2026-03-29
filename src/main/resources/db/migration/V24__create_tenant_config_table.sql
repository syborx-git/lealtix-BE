-- =====================================================
-- V24: Agregar columnas faltantes a tenant_config
-- Fecha: 2026-03-26
-- Descripción: Agrega columnas faltantes a tabla tenant_config existente
-- =====================================================

-- Agregar columnas a tenant_config si no existen
ALTER TABLE tenant_config
ADD COLUMN IF NOT EXISTS history VARCHAR(500),
ADD COLUMN IF NOT EXISTS vision VARCHAR(500),
ADD COLUMN IF NOT EXISTS bussines_email VARCHAR(150),
ADD COLUMN IF NOT EXISTS twitter VARCHAR(255),
ADD COLUMN IF NOT EXISTS facebook VARCHAR(255),
ADD COLUMN IF NOT EXISTS linkedin VARCHAR(255),
ADD COLUMN IF NOT EXISTS instagram VARCHAR(255),
ADD COLUMN IF NOT EXISTS tiktok VARCHAR(255),
ADD COLUMN IF NOT EXISTS schedules TEXT,
ADD COLUMN IF NOT EXISTS kitchen_module_enabled BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS kitchen_enabled_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Crear índices para tenant_config
CREATE INDEX IF NOT EXISTS idx_tenant_config_tenant ON tenant_config(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_config_kitchen_enabled ON tenant_config(kitchen_module_enabled);
CREATE INDEX IF NOT EXISTS idx_tenant_config_kitchen_enabled_at ON tenant_config(kitchen_enabled_at);
CREATE INDEX IF NOT EXISTS idx_tenant_config_updated_at ON tenant_config(updated_at);
