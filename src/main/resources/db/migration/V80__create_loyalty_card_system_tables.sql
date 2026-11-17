-- ============================================================================
-- V80: Creación del Sistema de Tarjetas de Fidelización y Sellos para Lealtix
-- Soporta:
--   1. Configuración de personalización por restaurante (colores, sellos, recompensas)
--   2. Tarjetas digitales individuales de clientes con sellos actuales y canjes
--   3. Historial auditable de movimientos de sellos y redenciones
-- ============================================================================

-- 1. Tabla de Configuración de la Tarjeta de Fidelización por Restaurante
CREATE TABLE IF NOT EXISTS tenant_loyalty_card_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL UNIQUE REFERENCES tenant(id) ON DELETE CASCADE,
    card_title VARCHAR(150) NOT NULL DEFAULT 'Tarjeta de Fidelización',
    card_subtitle VARCHAR(255) DEFAULT 'Programa de Lealtad y Recompensas',
    bg_color_hex VARCHAR(20) NOT NULL DEFAULT '#4338CA',
    text_color_hex VARCHAR(20) NOT NULL DEFAULT '#FFFFFF',
    accent_color_hex VARCHAR(20) NOT NULL DEFAULT '#F59E0B',
    stamp_icon VARCHAR(50) NOT NULL DEFAULT 'HAMBURGER',
    total_stamps INT NOT NULL DEFAULT 8 CHECK (total_stamps > 0 AND total_stamps <= 20),
    reward_mode VARCHAR(50) NOT NULL DEFAULT 'FULL_CARD', -- FULL_CARD | MILESTONE_PER_STAMP
    reward_type VARCHAR(50) NOT NULL DEFAULT 'FREE_PRODUCT', -- FREE_PRODUCT | PERCENT_DISCOUNT | FIXED_AMOUNT | TWO_FOR_ONE | CUSTOM
    reward_value NUMERIC(10, 2) DEFAULT NULL, -- Porcentaje o Monto Fijo si aplica
    reward_product_id BIGINT REFERENCES tenant_menu_product(id) ON DELETE SET NULL, -- Platillo del menú de cortesía
    reward_title VARCHAR(200) NOT NULL DEFAULT 'Recompensa de Cortesía',
    reward_description TEXT DEFAULT NULL,
    milestone_config TEXT DEFAULT NULL, -- Configuración JSON opcional para hitos intermedios
    terms_and_conditions TEXT DEFAULT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_loyalty_config_tenant ON tenant_loyalty_card_config(tenant_id);

-- 2. Tabla de Tarjetas de Lealtad por Cliente
CREATE TABLE IF NOT EXISTS customer_loyalty_card (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES tenant_customer(id) ON DELETE CASCADE,
    current_stamps INT NOT NULL DEFAULT 0 CHECK (current_stamps >= 0),
    completed_cards_count INT NOT NULL DEFAULT 0 CHECK (completed_cards_count >= 0),
    total_stamps_accumulated INT NOT NULL DEFAULT 0 CHECK (total_stamps_accumulated >= 0),
    has_pending_reward BOOLEAN NOT NULL DEFAULT FALSE,
    pending_reward_description VARCHAR(255) DEFAULT NULL,
    google_wallet_object_id VARCHAR(255) DEFAULT NULL,
    google_wallet_save_url TEXT DEFAULT NULL,
    last_stamp_at TIMESTAMP DEFAULT NULL,
    last_redemption_at TIMESTAMP DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customer_loyalty_card UNIQUE (tenant_id, customer_id)
);

CREATE INDEX IF NOT EXISTS idx_customer_loyalty_card_tenant_customer ON customer_loyalty_card(tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_loyalty_card_stamps ON customer_loyalty_card(tenant_id, current_stamps);
CREATE INDEX IF NOT EXISTS idx_customer_loyalty_card_pending_reward ON customer_loyalty_card(tenant_id, has_pending_reward);

-- 3. Tabla de Historial de Movimientos de Sellos (Ledger de Sellos)
CREATE TABLE IF NOT EXISTS loyalty_stamp_history (
    id BIGSERIAL PRIMARY KEY,
    card_id BIGINT NOT NULL REFERENCES customer_loyalty_card(id) ON DELETE CASCADE,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES tenant_customer(id) ON DELETE CASCADE,
    order_id UUID REFERENCES client_order(id) ON DELETE SET NULL,
    registered_by_user_id BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
    action_type VARCHAR(50) NOT NULL, -- ADD_STAMP | REDEEM_REWARD | RESET_CYCLE | MANUAL_ADJUST
    stamps_change INT NOT NULL DEFAULT 0,
    stamps_before INT NOT NULL,
    stamps_after INT NOT NULL,
    reward_redeemed VARCHAR(255) DEFAULT NULL,
    notes TEXT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_loyalty_stamp_history_card ON loyalty_stamp_history(card_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_stamp_history_tenant_created ON loyalty_stamp_history(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_loyalty_stamp_history_customer ON loyalty_stamp_history(customer_id);

-- 4. Semilla inicial de Configuración para Tenants Existentes
-- Para Tenant 1 (Restaurante Petra / Demo)
INSERT INTO tenant_loyalty_card_config (
    tenant_id, card_title, card_subtitle, bg_color_hex, text_color_hex, accent_color_hex,
    stamp_icon, total_stamps, reward_mode, reward_type, reward_value, reward_title,
    reward_description, terms_and_conditions, active
) VALUES (
    1,
    'Petra Club Gourmet',
    'Tarjeta de Sellos y Beneficios Exclusivos',
    '#4338CA',
    '#FFFFFF',
    '#F59E0B',
    'HAMBURGER',
    8,
    'FULL_CARD',
    'FREE_PRODUCT',
    NULL,
    'Enchiladas Petra de Cortesía',
    'Acumula 8 sellos en tus visitas y recibe una orden de Enchiladas Petra o 2x1 en platillos fuertes.',
    'Válido en consumo en restaurante. Un sello por cuenta mínima de $150 MXN. No acumulable con otras promociones.',
    TRUE
) ON CONFLICT (tenant_id) DO NOTHING;

-- Para Tenant 3 (VectorInside) si existe
INSERT INTO tenant_loyalty_card_config (
    tenant_id, card_title, card_subtitle, bg_color_hex, text_color_hex, accent_color_hex,
    stamp_icon, total_stamps, reward_mode, reward_type, reward_value, reward_title,
    reward_description, terms_and_conditions, active
)
SELECT 
    3,
    'VectorInside Rewards',
    'Programa de Fidelización Digital',
    '#0F172A',
    '#FFFFFF',
    '#10B981',
    'STAR',
    10,
    'FULL_CARD',
    'PERCENT_DISCOUNT',
    20.00,
    '20% de Descuento en tu Cuenta',
    'Completa 10 visitas y obtén un 20% de descuento directo en tu próximo consumo.',
    'Aplica en consumo en sucursal. Válido durante 30 días posteriores al canje.',
    TRUE
WHERE EXISTS (SELECT 1 FROM tenant WHERE id = 3)
ON CONFLICT (tenant_id) DO NOTHING;

-- 5. Inicializar Tarjetas de Lealtad para Clientes Existentes en Tenant 1
INSERT INTO customer_loyalty_card (
    tenant_id, customer_id, current_stamps, completed_cards_count, total_stamps_accumulated,
    has_pending_reward, pending_reward_description, last_stamp_at
)
SELECT 
    c.tenant_id,
    c.id,
    CASE 
        WHEN c.id = 1 THEN 6 -- Jesus Ramirez: 6 sellos
        WHEN c.id = 2 THEN 8 -- Jorge: 8 sellos (¡Premio listo para canjear!)
        ELSE 2               -- Otros: 2 sellos
    END,
    CASE 
        WHEN c.id = 1 THEN 2 -- Ha canjeado 2 veces antes
        WHEN c.id = 2 THEN 1 -- Ha canjeado 1 vez antes
        ELSE 0 
    END,
    CASE 
        WHEN c.id = 1 THEN 22 -- 6 actuales + 16 de 2 tarjetas completadas
        WHEN c.id = 2 THEN 16 -- 8 actuales + 8 de 1 tarjeta completada
        ELSE 2
    END,
    CASE 
        WHEN c.id = 2 THEN TRUE -- Jorge tiene recompensa pendiente
        ELSE FALSE 
    END,
    CASE 
        WHEN c.id = 2 THEN 'Enchiladas Petra de Cortesía'
        ELSE NULL 
    END,
    CURRENT_TIMESTAMP - INTERVAL '2 days'
FROM tenant_customer c
WHERE c.tenant_id = 1
ON CONFLICT (tenant_id, customer_id) DO NOTHING;

