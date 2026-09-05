-- =====================================================
-- V36: Agregar campo sueldo_mensual a tenant_user
-- Fecha: 2026-09-01
-- Descripción: Sueldo mensual de cada miembro para el cálculo
--              automático de costos operacionales (recurso humano).
--              Default 100 para los registros existentes.
-- =====================================================

ALTER TABLE tenant_user ADD COLUMN IF NOT EXISTS sueldo_mensual NUMERIC(12,2) NOT NULL DEFAULT 100.00;

-- Índice adicional (opcional) para agilizar sumatorias por tenant
CREATE INDEX IF NOT EXISTS idx_tenant_user_tenant_sueldo ON tenant_user(tenant_id, sueldo_mensual);
