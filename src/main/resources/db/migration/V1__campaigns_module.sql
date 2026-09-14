-- =====================================================================
-- V1: Módulo de Campañas (estado final consolidado)
-- Consolida: V1, V3, V6(email), V7, V8, V9, V13(campaign), V14
-- Nota: V17+V18 se cancelan entre sí → business_id se mantiene
-- =====================================================================

-- -----------------------------------------------------------------------
-- 1. Modificaciones a tabla campaign (ya existente antes de V1)
-- -----------------------------------------------------------------------

-- Columnas de draft (V1 original)
ALTER TABLE campaign ADD COLUMN IF NOT EXISTS is_draft BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE campaign ADD COLUMN IF NOT EXISTS published_at TIMESTAMP NULL;

-- is_automatic (V1 original — renombrar si venía de nombre camelCase)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'campaign' AND column_name = 'is_automatic') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'campaign' AND column_name = 'isAutomatic') THEN
            ALTER TABLE campaign RENAME COLUMN "isAutomatic" TO is_automatic;
        ELSE
            ALTER TABLE campaign ADD COLUMN is_automatic BOOLEAN DEFAULT FALSE NOT NULL;
        END IF;
    END IF;
END $$;

UPDATE campaign SET is_draft = TRUE WHERE status = 'DRAFT';

-- Métricas de email (V6 email)
ALTER TABLE campaign ADD COLUMN IF NOT EXISTS total_sent    INTEGER   DEFAULT 0;
ALTER TABLE campaign ADD COLUMN IF NOT EXISTS total_failed  INTEGER   DEFAULT 0;
ALTER TABLE campaign ADD COLUMN IF NOT EXISTS finished_at   TIMESTAMP NULL;

-- Costo estimado para ROI (V13)
ALTER TABLE campaign ADD COLUMN IF NOT EXISTS estimated_cost NUMERIC(10, 2);

-- Índices de campaign
CREATE INDEX IF NOT EXISTS idx_campaign_business_draft ON campaign(business_id, is_draft);
CREATE INDEX IF NOT EXISTS idx_campaign_metrics       ON campaign(total_sent, total_failed);

-- CHECK status: incluye SENDING (V9)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_class t ON c.conrelid = t.oid
        WHERE t.relname = 'campaign' AND c.conname = 'campaign_status_check'
    ) THEN
        ALTER TABLE campaign DROP CONSTRAINT campaign_status_check;
    END IF;

    ALTER TABLE campaign
      ADD CONSTRAINT campaign_status_check CHECK (status IN (
        'DRAFT','READY','SENDING','ACTIVE','INACTIVE','SCHEDULED'
      ));
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'No se pudo actualizar campaign_status_check: %', SQLERRM;
END $$;

-- CHECK promo_type: incluye NONE (V14)
ALTER TABLE campaign DROP CONSTRAINT IF EXISTS campaign_promo_type_check;
ALTER TABLE campaign
    ADD CONSTRAINT campaign_promo_type_check CHECK (promo_type IN (
        'NONE','DISCOUNT','AMOUNT','BOGO','FREE_ITEM','CUSTOM'
    ) OR promo_type IS NULL);

-- -----------------------------------------------------------------------
-- 2. Tabla promotion_reward (V3)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS promotion_reward (
    id               BIGSERIAL PRIMARY KEY,
    campaign_id      BIGINT        NOT NULL UNIQUE,
    reward_type      VARCHAR(50)   NOT NULL,
    numeric_value    DECIMAL(10,2),
    product_id       BIGINT,
    buy_quantity     INT,
    free_quantity    INT,
    custom_config    TEXT,
    description      VARCHAR(500),
    min_purchase_amount DECIMAL(10,2),
    usage_limit      INT,
    usage_count      INT DEFAULT 0,
    created_at       TIMESTAMP     NOT NULL,
    updated_at       TIMESTAMP     NOT NULL,
    CONSTRAINT fk_promotion_reward_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaign(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_promotion_reward_campaign ON promotion_reward(campaign_id);
CREATE INDEX IF NOT EXISTS idx_promotion_reward_type     ON promotion_reward(reward_type);

-- Migrar datos existentes de campaign a promotion_reward (V3)
INSERT INTO promotion_reward (campaign_id, reward_type, numeric_value, description, usage_count, created_at, updated_at)
SELECT
    c.id,
    CASE
        WHEN c.promo_type = 'DISCOUNT'  THEN 'PERCENT_DISCOUNT'
        WHEN c.promo_type = 'AMOUNT'    THEN 'FIXED_AMOUNT'
        WHEN c.promo_type = 'FREE_ITEM' THEN 'FREE_PRODUCT'
        WHEN c.promo_type = 'BOGO'      THEN 'BUY_X_GET_Y'
        ELSE 'CUSTOM'
    END,
    CASE
        WHEN c.promo_type IN ('DISCOUNT','AMOUNT') THEN
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

-- -----------------------------------------------------------------------
-- 3. campaign_result: agregar last_click_at (V3)
-- -----------------------------------------------------------------------
ALTER TABLE campaign_result ADD COLUMN IF NOT EXISTS last_click_at TIMESTAMP NULL;

-- -----------------------------------------------------------------------
-- 4. Tabla campaign_email (V7)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS campaign_email (
    id                      UUID PRIMARY KEY,
    campaign_id             BIGINT          NOT NULL,
    recipient_email         VARCHAR(255)    NOT NULL,
    recipient_name          VARCHAR(150),
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    attempt_count           INTEGER         NOT NULL DEFAULT 0,
    max_attempts            INTEGER,
    scheduled_at            TIMESTAMP,
    last_attempt_at         TIMESTAMP,
    next_attempt_at         TIMESTAMP,
    sent_at                 TIMESTAMP,
    opened_at               TIMESTAMP,
    bounced_at              TIMESTAMP,
    provider_name           VARCHAR(100),
    provider_message_id     VARCHAR(255),
    provider_error_code     VARCHAR(100),
    provider_error_message  TEXT,
    correlation_id          VARCHAR(100),
    coupon_id               BIGINT,
    created_at              TIMESTAMP       NOT NULL,
    updated_at              TIMESTAMP       NOT NULL,
    CONSTRAINT fk_campaign_email_campaign
        FOREIGN KEY (campaign_id) REFERENCES campaign(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_campaign_email_campaign       ON campaign_email(campaign_id);
CREATE INDEX IF NOT EXISTS idx_campaign_email_status         ON campaign_email(status);
CREATE INDEX IF NOT EXISTS idx_campaign_email_recipient      ON campaign_email(recipient_email);
CREATE INDEX IF NOT EXISTS idx_campaign_email_provider_msg   ON campaign_email(provider_message_id);
CREATE INDEX IF NOT EXISTS idx_campaign_email_next_attempt   ON campaign_email(next_attempt_at) WHERE status = 'PENDING';

-- -----------------------------------------------------------------------
-- 5. Tabla campaign_email_payload (V8)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS campaign_email_payload (
    id                  UUID PRIMARY KEY,
    campaign_email_id   UUID            NOT NULL,
    email_subject       VARCHAR(255),
    email_body          TEXT,
    email_body_html     TEXT,
    template_id         VARCHAR(100),
    template_data       TEXT,
    headers             TEXT,
    attempt_number      INTEGER,
    created_at          TIMESTAMP       NOT NULL,
    CONSTRAINT fk_campaign_email_payload_campaign_email
        FOREIGN KEY (campaign_email_id) REFERENCES campaign_email(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_campaign_email_payload_campaign_email ON campaign_email_payload(campaign_email_id);
CREATE INDEX IF NOT EXISTS idx_campaign_email_payload_created_at     ON campaign_email_payload(created_at);
