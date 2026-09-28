-- =====================================================
-- V23: Agregar columnas faltantes en coupon, coupon_redemption y tenant_config
-- Fecha: 2026-03-26
-- Descripción: Agrega columnas que están definidas en las entidades JPA pero faltaban en la BD
-- =====================================================

-- 1. Agregar columnas a coupon
ALTER TABLE coupon
ADD COLUMN IF NOT EXISTS qr_token VARCHAR(64) UNIQUE,
ADD COLUMN IF NOT EXISTS redeemed_by VARCHAR(200);

-- 2. Agregar columna a coupon_redemption
ALTER TABLE coupon_redemption
ADD COLUMN IF NOT EXISTS purchase_amount NUMERIC(10, 2);

-- Crear índices para coupon
CREATE INDEX IF NOT EXISTS idx_coupon_qr_token ON coupon(qr_token);
CREATE INDEX IF NOT EXISTS idx_coupon_redeemed_by ON coupon(redeemed_by);
