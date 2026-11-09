-- ============================================================================
-- V77: Soporte para método de pago American Express (AMEX) en cortes de caja diarios
-- ============================================================================

ALTER TABLE cortes_caja_diarios
    ADD COLUMN IF NOT EXISTS sistema_amex NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS real_amex NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS diferencia_amex NUMERIC(12, 2) NOT NULL DEFAULT 0.00;

COMMENT ON COLUMN cortes_caja_diarios.sistema_amex IS 'Monto total cobrado con American Express (AMEX) registrado por el sistema';
COMMENT ON COLUMN cortes_caja_diarios.real_amex IS 'Monto total declarado en vouchers físicos de American Express (AMEX)';
COMMENT ON COLUMN cortes_caja_diarios.diferencia_amex IS 'Diferencia de arqueo para American Express (real - sistema)';
