-- =====================================================================
-- V2: Módulo de Lealtad / Clientes (estado final consolidado)
-- Consolida: V2, V4, V5, V10, V23
-- -----------------------------------------------------------------------

-- -----------------------------------------------------------------------
-- 1. Modificaciones a tenant_customer (ya existente)
-- -----------------------------------------------------------------------

-- Columnas de aceptación de promociones (V2)
ALTER TABLE tenant_customer ADD COLUMN IF NOT EXISTS accepted_promotions BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tenant_customer ADD COLUMN IF NOT EXISTS accepted_at DATE NULL;

CREATE INDEX IF NOT EXISTS idx_tenant_customer_accepted_promotions
    ON tenant_customer(tenant_id, accepted_promotions);

-- Soft-delete (V4)
ALTER TABLE tenant_customer ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
CREATE INDEX IF NOT EXISTS idx_tenant_customer_active ON tenant_customer(tenant_id, active);

-- -----------------------------------------------------------------------
-- 2. Tabla coupon (V3 + V10 + V23 — estado final)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS coupon (
    id                   BIGSERIAL    PRIMARY KEY,
    code                 VARCHAR(100) NOT NULL UNIQUE,
    campaign_id          BIGINT       NOT NULL,
    customer_id          BIGINT       NOT NULL,
    status               VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    expires_at           TIMESTAMP    NULL,
    created_at           TIMESTAMP    NOT NULL,
    redeemed_at          TIMESTAMP    NULL,
    redemption_metadata  TEXT,
    qr_url               VARCHAR(500),
    qr_token             VARCHAR(64)  UNIQUE,
    redeemed_by          VARCHAR(200),
    CONSTRAINT fk_coupon_campaign  FOREIGN KEY (campaign_id)  REFERENCES campaign(id)       ON DELETE CASCADE,
    CONSTRAINT fk_coupon_customer  FOREIGN KEY (customer_id)  REFERENCES tenant_customer(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_coupon_code       ON coupon(code);
CREATE INDEX IF NOT EXISTS idx_coupon_campaign   ON coupon(campaign_id);
CREATE INDEX IF NOT EXISTS idx_coupon_customer   ON coupon(customer_id);
CREATE INDEX IF NOT EXISTS idx_coupon_status     ON coupon(status);
CREATE INDEX IF NOT EXISTS idx_coupon_expires_at ON coupon(expires_at);
CREATE INDEX IF NOT EXISTS idx_coupon_qr_url     ON coupon(qr_url);
CREATE INDEX IF NOT EXISTS idx_coupon_qr_token   ON coupon(qr_token);
CREATE INDEX IF NOT EXISTS idx_coupon_redeemed_by ON coupon(redeemed_by);

-- -----------------------------------------------------------------------
-- 3. Modificaciones a coupon_redemption (ya existente): V5 + V23
-- -----------------------------------------------------------------------
ALTER TABLE coupon_redemption
    ADD COLUMN IF NOT EXISTS original_amount  NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS discount_amount  NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS final_amount     NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS coupon_type      VARCHAR(50),
    ADD COLUMN IF NOT EXISTS coupon_value     NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS purchase_amount  NUMERIC(10,2);

CREATE INDEX IF NOT EXISTS idx_redemption_coupon_type ON coupon_redemption(coupon_type);
