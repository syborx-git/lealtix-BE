-- =====================================================================
-- V4: Módulo ChatBot + Venta Cruzada (consolidado)
-- Consolida: V15, V16 (tablas chatbot)
-- =====================================================================

-- -----------------------------------------------------------------------
-- 1. Tabla product_cross_selling (V15)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_cross_selling (
    id                   BIGSERIAL PRIMARY KEY,
    product_id           BIGINT    NOT NULL,
    suggested_product_id BIGINT    NOT NULL,
    tenant_id            BIGINT    NOT NULL,
    display_order        INTEGER   DEFAULT 1,
    is_active            BOOLEAN   DEFAULT TRUE,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_cross_selling_product
        FOREIGN KEY (product_id)           REFERENCES tenant_menu_product(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_cross_selling_suggested
        FOREIGN KEY (suggested_product_id) REFERENCES tenant_menu_product(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_cross_selling_tenant
        FOREIGN KEY (tenant_id)            REFERENCES tenant(id)              ON DELETE CASCADE,
    CONSTRAINT chk_product_cross_selling_different_products
        CHECK (product_id != suggested_product_id),
    CONSTRAINT chk_product_cross_selling_display_order
        CHECK (display_order > 0)
);

CREATE INDEX IF NOT EXISTS idx_product_cross_selling_product_id
    ON product_cross_selling(product_id);
CREATE INDEX IF NOT EXISTS idx_product_cross_selling_tenant_id
    ON product_cross_selling(tenant_id);
CREATE INDEX IF NOT EXISTS idx_product_cross_selling_active
    ON product_cross_selling(product_id, tenant_id, is_active);
CREATE INDEX IF NOT EXISTS idx_product_cross_selling_order
    ON product_cross_selling(product_id, display_order);
CREATE UNIQUE INDEX IF NOT EXISTS idx_product_cross_selling_unique
    ON product_cross_selling(product_id, suggested_product_id, tenant_id);

COMMENT ON TABLE product_cross_selling IS 'Gestiona las sugerencias de productos complementarios para venta cruzada';

-- -----------------------------------------------------------------------
-- 2. Tabla chatbot_session (V16)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chatbot_session (
    id                   BIGSERIAL    PRIMARY KEY,
    session_id           VARCHAR(100) NOT NULL UNIQUE,
    tenant_id            BIGINT       NOT NULL,
    customer_id          BIGINT,
    phone                VARCHAR(20),
    email                VARCHAR(150),
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    context              JSONB,
    started_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_interaction_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at             TIMESTAMP,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_chatbot_session_tenant
        FOREIGN KEY (tenant_id)    REFERENCES tenant(id)          ON DELETE CASCADE,
    CONSTRAINT fk_chatbot_session_customer
        FOREIGN KEY (customer_id)  REFERENCES tenant_customer(id) ON DELETE SET NULL,
    CONSTRAINT chk_chatbot_session_status
        CHECK (status IN ('ACTIVE','COMPLETED','ABANDONED','ERROR'))
);

CREATE INDEX IF NOT EXISTS idx_chatbot_session_tenant           ON chatbot_session(tenant_id);
CREATE INDEX IF NOT EXISTS idx_chatbot_session_customer         ON chatbot_session(customer_id);
CREATE INDEX IF NOT EXISTS idx_chatbot_session_status           ON chatbot_session(status);
CREATE INDEX IF NOT EXISTS idx_chatbot_session_started_at       ON chatbot_session(started_at);
CREATE INDEX IF NOT EXISTS idx_chatbot_session_last_interaction ON chatbot_session(last_interaction_at);

COMMENT ON TABLE chatbot_session IS 'Sesiones de conversación del ChatBot (Mesero Virtual)';
COMMENT ON COLUMN chatbot_session.context IS 'Contexto de la conversación en JSON (productos en carrito, etc.)';

-- -----------------------------------------------------------------------
-- 3. Tabla chatbot_message (V16)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chatbot_message (
    id           BIGSERIAL   PRIMARY KEY,
    session_id   BIGINT      NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    sender       VARCHAR(20) NOT NULL,
    content      TEXT        NOT NULL,
    metadata     JSONB,
    timestamp    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_chatbot_message_session
        FOREIGN KEY (session_id) REFERENCES chatbot_session(id) ON DELETE CASCADE,
    CONSTRAINT chk_chatbot_message_type
        CHECK (message_type IN ('TEXT','PRODUCT_SUGGESTION','COUPON_VALIDATION','ORDER_CONFIRMATION','ERROR')),
    CONSTRAINT chk_chatbot_message_sender
        CHECK (sender IN ('USER','BOT','SYSTEM'))
);

CREATE INDEX IF NOT EXISTS idx_chatbot_message_session   ON chatbot_message(session_id);
CREATE INDEX IF NOT EXISTS idx_chatbot_message_timestamp ON chatbot_message(timestamp);
CREATE INDEX IF NOT EXISTS idx_chatbot_message_type      ON chatbot_message(message_type);

COMMENT ON TABLE chatbot_message IS 'Mensajes individuales de las conversaciones del ChatBot';
