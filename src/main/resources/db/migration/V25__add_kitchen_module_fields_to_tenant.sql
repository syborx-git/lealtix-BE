-- =====================================================
-- V25: Agregar campos de módulo cocina a tabla tenant
-- Fecha: 2026-03-26
-- Descripción: Agrega columnas kitchen_module_enabled y kitchen_enabled_at a tabla tenant
-- =====================================================

-- Agregar columnas a tenant si no existen
ALTER TABLE tenant
ADD COLUMN IF NOT EXISTS kitchen_module_enabled BOOLEAN,
ADD COLUMN IF NOT EXISTS kitchen_enabled_at TIMESTAMP;

-- Crear índices para mejorar búsquedas
CREATE INDEX IF NOT EXISTS idx_tenant_kitchen_enabled ON tenant(kitchen_module_enabled);
CREATE INDEX IF NOT EXISTS idx_tenant_kitchen_enabled_at ON tenant(kitchen_enabled_at);
