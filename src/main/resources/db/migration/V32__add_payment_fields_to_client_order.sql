-- =====================================================
-- V32: Agregar campos de pago a client_order
-- Fecha: 2026-04-25
-- Descripción: Permite registrar el método de pago, referencia, usuario que registró y timestamp
-- Sin procesar transacciones en línea - solo registro simple
-- =====================================================

-- Agregar columnas de pago
ALTER TABLE client_order
ADD COLUMN IF NOT EXISTS paid_method VARCHAR(20),
ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(255),
ADD COLUMN IF NOT EXISTS paid_by BIGINT,
ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;

-- Foreign key para paid_by (usuario que registró el pago)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.constraint_column_usage 
        WHERE table_name='client_order' AND constraint_name='fk_client_order_paid_by'
    ) THEN
        ALTER TABLE client_order
        ADD CONSTRAINT fk_client_order_paid_by 
          FOREIGN KEY (paid_by) REFERENCES app_user(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Índices para mejorar consultas
CREATE INDEX IF NOT EXISTS idx_client_order_paid_method ON client_order(paid_method);
CREATE INDEX IF NOT EXISTS idx_client_order_paid_at ON client_order(paid_at);
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_paid_at ON client_order(tenant_id, paid_at);

-- Comentarios para documentación
COMMENT ON COLUMN client_order.paid_method IS 'Método de pago: CASH, CARD, TRANSFER, MIXED';
COMMENT ON COLUMN client_order.payment_reference IS 'Referencia del comprobante (número autorización, UUID transferencia, etc)';
COMMENT ON COLUMN client_order.paid_by IS 'ID del usuario que registró el pago';
COMMENT ON COLUMN client_order.paid_at IS 'Timestamp cuando se registró el pago';
