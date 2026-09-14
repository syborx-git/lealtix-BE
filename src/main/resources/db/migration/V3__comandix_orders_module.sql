-- =====================================================================
-- V3: Módulo Comandix — Órdenes (estado final consolidado)
-- Consolida: V11, V12, V13(client_order), V16(source), V22, V26, V32, V33, V35
-- =====================================================================

-- -----------------------------------------------------------------------
-- 1. Tabla client_order (estado FINAL con todas las columnas)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS client_order (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Relaciones
    customer_id          BIGINT,                         -- nullable (V13: ventas anónimas)
    tenant_id            BIGINT          NOT NULL,

    -- Info de la orden
    fecha                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado               VARCHAR(20)     NOT NULL DEFAULT 'PENDIENTE',
    source               VARCHAR(20)     DEFAULT 'MANUAL',  -- V16: MANUAL | CHATBOT

    -- Montos
    subtotal             NUMERIC(10,2)   NOT NULL DEFAULT 0.00,
    descuento            NUMERIC(10,2)   NOT NULL DEFAULT 0.00,
    total                NUMERIC(10,2)   NOT NULL DEFAULT 0.00,

    -- Cupón aplicado (V13)
    coupon_id            BIGINT,

    -- Timestamps de estado (V22)
    accepted_at          TIMESTAMP       NULL,
    ready_at             TIMESTAMP       NULL,

    -- Pago (V32)
    paid_method          VARCHAR(20),
    payment_reference    VARCHAR(255),
    paid_by              BIGINT,
    paid_at              TIMESTAMP,

    -- Cancelación (V33)
    cancelled_by         VARCHAR(255),
    cancelled_at         TIMESTAMP,
    cancellation_reason  VARCHAR(500),

    -- Auditoría
    created_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_client_order_customer
        FOREIGN KEY (customer_id)   REFERENCES tenant_customer(id) ON DELETE CASCADE,
    CONSTRAINT fk_client_order_tenant
        FOREIGN KEY (tenant_id)     REFERENCES tenant(id)          ON DELETE CASCADE,
    CONSTRAINT fk_client_order_paid_by
        FOREIGN KEY (paid_by)       REFERENCES app_user(id)        ON DELETE SET NULL,

    -- Estado final (incluye todos los valores de V11 + V26)
    CONSTRAINT chk_client_order_estado
        CHECK (estado IN ('PENDIENTE','CONFIRMADA','PAGADA','CANCELADA','EN_PREPARACION','LISTO')),
    CONSTRAINT chk_client_order_amounts
        CHECK (subtotal >= 0 AND descuento >= 0 AND total >= 0)
);

-- Comentarios de documentación
COMMENT ON COLUMN client_order.customer_id IS 'ID del cliente (nullable para ventas generales/anónimas)';
COMMENT ON COLUMN client_order.coupon_id   IS 'ID del cupón usado en la orden (opcional)';
COMMENT ON COLUMN client_order.paid_method IS 'Método de pago: CASH, CARD, TRANSFER, MIXED';
COMMENT ON COLUMN client_order.paid_by     IS 'ID del usuario que registró el pago';

-- Índices client_order
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_id        ON client_order(tenant_id);
CREATE INDEX IF NOT EXISTS idx_client_order_customer_id      ON client_order(customer_id);
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_customer  ON client_order(tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_fecha     ON client_order(tenant_id, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_estado    ON client_order(tenant_id, estado);
CREATE INDEX IF NOT EXISTS idx_client_order_fecha            ON client_order(fecha DESC);
CREATE INDEX IF NOT EXISTS idx_client_order_coupon_id        ON client_order(coupon_id);
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_coupon    ON client_order(tenant_id, coupon_id);
CREATE INDEX IF NOT EXISTS idx_client_order_source           ON client_order(source);
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_source    ON client_order(tenant_id, source);
CREATE INDEX IF NOT EXISTS idx_client_order_accepted_at      ON client_order(accepted_at);
CREATE INDEX IF NOT EXISTS idx_client_order_ready_at         ON client_order(ready_at);
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_accepted_at ON client_order(tenant_id, accepted_at);
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_ready_at  ON client_order(tenant_id, ready_at);
CREATE INDEX IF NOT EXISTS idx_client_order_paid_method      ON client_order(paid_method);
CREATE INDEX IF NOT EXISTS idx_client_order_paid_at          ON client_order(paid_at);
CREATE INDEX IF NOT EXISTS idx_client_order_tenant_paid_at   ON client_order(tenant_id, paid_at);
CREATE INDEX IF NOT EXISTS idx_client_order_cancelled_by     ON client_order(cancelled_by);
CREATE INDEX IF NOT EXISTS idx_client_order_cancelled_at     ON client_order(cancelled_at);
CREATE INDEX IF NOT EXISTS idx_client_order_cancellation     ON client_order(estado, cancelled_at) WHERE estado = 'CANCELADA';

-- -----------------------------------------------------------------------
-- 2. Tabla client_order_item (estado FINAL con columnas de V12 + V35)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS client_order_item (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Relaciones
    order_id                  UUID    NOT NULL,
    product_id                BIGINT  NOT NULL,

    -- Info del ítem
    cantidad                  INTEGER         NOT NULL,
    precio_unitario           NUMERIC(10,2)   NOT NULL,
    comentarios               TEXT,

    -- Configuración de ingredientes (V35)
    excluded_ingredient_ids   TEXT,
    additional_ingredient_ids TEXT,

    -- Auditoría
    created_at                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_client_order_item_order
        FOREIGN KEY (order_id)    REFERENCES client_order(id)       ON DELETE CASCADE,
    CONSTRAINT fk_client_order_item_product
        FOREIGN KEY (product_id)  REFERENCES tenant_menu_product(id) ON DELETE RESTRICT,
    CONSTRAINT chk_client_order_item_cantidad
        CHECK (cantidad > 0),
    CONSTRAINT chk_client_order_item_precio
        CHECK (precio_unitario > 0)
);

CREATE INDEX IF NOT EXISTS idx_client_order_item_order_id    ON client_order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_client_order_item_product_id  ON client_order_item(product_id);
CREATE INDEX IF NOT EXISTS idx_client_order_item_created_at  ON client_order_item(created_at DESC);

-- Poblar source en órdenes existentes sin valor
UPDATE client_order SET source = 'MANUAL' WHERE source IS NULL;
