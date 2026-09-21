-- V010: Agregar columna qr_url a coupon
-- Fecha: 2025-12-28
-- Agrega la columna qr_url para almacenar la URL del código QR

ALTER TABLE coupon
    ADD COLUMN IF NOT EXISTS qr_url VARCHAR(500);

-- Opcional: crear índice para búsquedas por qr_url
CREATE INDEX IF NOT EXISTS idx_coupon_qr_url ON coupon(qr_url);
