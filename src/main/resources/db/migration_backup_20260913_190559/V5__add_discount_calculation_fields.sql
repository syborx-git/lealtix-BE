-- Migración V5: Agregar campos de cálculo de descuentos en coupon_redemption
-- Fecha: 2026-01-03
-- Descripción: Agrega campos para almacenar el cálculo de descuentos al redimir cupones

-- Agregar campos de montos
ALTER TABLE coupon_redemption
ADD COLUMN IF NOT EXISTS original_amount NUMERIC(10, 2),
ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(10, 2),
ADD COLUMN IF NOT EXISTS final_amount NUMERIC(10, 2);

-- Agregar campos de tipo y valor del cupón
ALTER TABLE coupon_redemption
ADD COLUMN IF NOT EXISTS coupon_type VARCHAR(50),
ADD COLUMN IF NOT EXISTS coupon_value NUMERIC(10, 2);

-- Crear índice para consultas de reportes por tipo de cupón
CREATE INDEX IF NOT EXISTS idx_redemption_coupon_type ON coupon_redemption(coupon_type);
