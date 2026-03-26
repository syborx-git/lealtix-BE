-- Script de migración para refactorización Campaign y Rewards
-- Fecha: 2025-12-27
-- Descripción: Separación de lógica de campañas y rewards
-- Base de datos: PostgreSQL

-- DEPRECATED: This development folder is no longer the canonical migrations location.
-- See src/main/resources/db/migration/V3__refactor_campaign_rewards.sql for the authoritative migration.
-- Do not edit files in this folder. Remove this folder or keep only backups.

-- 1. Crear tabla promotion_reward
CREATE TABLE IF NOT EXISTS promotion_reward (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL UNIQUE,
    reward_type VARCHAR(50) NOT NULL,
    numeric_value DECIMAL(10, 2),
    product_id BIGINT,
    buy_quantity INT,
    free_quantity INT,
    custom_config TEXT,
    description VARCHAR(500),
    min_purchase_amount DECIMAL(10, 2),
    usage_limit INT,
    usage_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_promotion_reward_campaign FOREIGN KEY (campaign_id) REFERENCES campaign(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_promotion_reward_campaign ON promotion_reward(campaign_id);
CREATE INDEX IF NOT EXISTS idx_promotion_reward_type ON promotion_reward(reward_type);

-- 2. Crear tabla coupon
CREATE TABLE IF NOT EXISTS coupon (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    campaign_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    redeemed_at TIMESTAMP NULL,
    redemption_metadata TEXT,
    CONSTRAINT fk_coupon_campaign FOREIGN KEY (campaign_id) REFERENCES campaign(id) ON DELETE CASCADE,
    CONSTRAINT fk_coupon_customer FOREIGN KEY (customer_id) REFERENCES tenant_customer(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_coupon_code ON coupon(code);
CREATE INDEX IF NOT EXISTS idx_coupon_campaign ON coupon(campaign_id);
CREATE INDEX IF NOT EXISTS idx_coupon_customer ON coupon(customer_id);
CREATE INDEX IF NOT EXISTS idx_coupon_status ON coupon(status);
CREATE INDEX IF NOT EXISTS idx_coupon_expires_at ON coupon(expires_at);

-- 3. Migrar datos existentes de campaign a promotion_reward
INSERT INTO promotion_reward (campaign_id, reward_type, numeric_value, description, usage_count, created_at, updated_at)
SELECT
    c.id,
    CASE
        WHEN c.promo_type = 'DISCOUNT' THEN 'PERCENT_DISCOUNT'
        WHEN c.promo_type = 'AMOUNT' THEN 'FIXED_AMOUNT'
        WHEN c.promo_type = 'FREE_ITEM' THEN 'FREE_PRODUCT'
        WHEN c.promo_type = 'BOGO' THEN 'BUY_X_GET_Y'
        ELSE 'CUSTOM'
    END,
    CASE
        WHEN c.promo_type IN ('DISCOUNT', 'AMOUNT') THEN
            CASE
                WHEN REGEXP_REPLACE(c.promo_value, '[^0-9.]', '', 'g') ~ '^[0-9]+\.?[0-9]*$'
                THEN CAST(REGEXP_REPLACE(c.promo_value, '[^0-9.]', '', 'g') AS DECIMAL(10,2))
                ELSE NULL
            END
        ELSE NULL
    END,
    CONCAT('Migrado desde campaña: ', c.title),
    0,
    NOW(),
    NOW()
FROM campaign c
WHERE c.promo_type IS NOT NULL AND c.status != 'DRAFT'
ON CONFLICT (campaign_id) DO NOTHING;

-- 4. Actualizar tabla campaign_result para agregar last_click_at
ALTER TABLE campaign_result
ADD COLUMN IF NOT EXISTS last_click_at TIMESTAMP NULL;

-- Notas de migración:
-- - Las campañas en DRAFT pueden no tener reward, esto es válido
-- - Solo campañas ACTIVE/SCHEDULED deben tener promotion_reward
-- - Los cupones se generarán dinámicamente al asignar campañas a clientes
-- - Verificar foreign keys según el nombre real de tenant_customer