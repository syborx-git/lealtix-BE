-- Migración V13: Agregar soporte para ventas generales y cupones en órdenes

-- 1. Hacer nullable el campo customer_id en client_order para soportar ventas generales/anónimas
ALTER TABLE client_order
ALTER COLUMN customer_id DROP NOT NULL;

-- 2. Agregar campo coupon_id a client_order
ALTER TABLE client_order
ADD COLUMN coupon_id BIGINT;

-- 3. Agregar índices para optimizar queries de dashboard
CREATE INDEX IF NOT EXISTS idx_client_order_coupon_id ON client_order(coupon_id);
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_coupon ON client_order(tenant_id, coupon_id);

-- 4. Agregar campo estimated_cost a campaign para cálculo de ROI
ALTER TABLE campaign
ADD COLUMN estimated_cost NUMERIC(10, 2);

-- 5. Comentarios sobre las tablas para documentación
COMMENT ON COLUMN client_order.customer_id IS 'ID del cliente (nullable para ventas generales/anónimas)';
COMMENT ON COLUMN client_order.coupon_id IS 'ID del cupón usado en la orden (opcional)';
COMMENT ON COLUMN campaign.estimated_cost IS 'Costo estimado de la campaña para cálculo de ROI';
