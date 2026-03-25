-- =====================================================
-- V16: Soporte para ChatBot (Mesero Virtual)
-- Fecha: 2026-02-24
-- Descripción: Agrega campo source a client_order y tablas de sesiones de chatbot
-- =====================================================

-- 1. Agregar campo source a client_order
ALTER TABLE client_order 
ADD COLUMN source VARCHAR(20) DEFAULT 'MANUAL';

-- Crear índice para filtrar órdenes por origen
CREATE INDEX idx_client_order_source ON client_order(source);

-- Crear índice compuesto para dashboard (tenant + source)
CREATE INDEX idx_client_order_tenant_source ON client_order(tenant_id, source);

-- 2. Crear tabla de sesiones de ChatBot
CREATE TABLE chatbot_session (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL UNIQUE,
    tenant_id BIGINT NOT NULL,
    customer_id BIGINT,
    phone VARCHAR(20),
    email VARCHAR(150),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    context JSONB,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_interaction_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_chatbot_session_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_chatbot_session_customer FOREIGN KEY (customer_id) REFERENCES tenant_customer(id) ON DELETE SET NULL,
    CONSTRAINT chk_chatbot_session_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'ABANDONED', 'ERROR'))
);

-- Índices para chatbot_session
CREATE INDEX idx_chatbot_session_tenant ON chatbot_session(tenant_id);
CREATE INDEX idx_chatbot_session_customer ON chatbot_session(customer_id);
CREATE INDEX idx_chatbot_session_status ON chatbot_session(status);
CREATE INDEX idx_chatbot_session_started_at ON chatbot_session(started_at);
CREATE INDEX idx_chatbot_session_last_interaction ON chatbot_session(last_interaction_at);

-- 3. Crear tabla de mensajes de ChatBot
CREATE TABLE chatbot_message (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    sender VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_chatbot_message_session FOREIGN KEY (session_id) REFERENCES chatbot_session(id) ON DELETE CASCADE,
    CONSTRAINT chk_chatbot_message_type CHECK (message_type IN ('TEXT', 'PRODUCT_SUGGESTION', 'COUPON_VALIDATION', 'ORDER_CONFIRMATION', 'ERROR')),
    CONSTRAINT chk_chatbot_message_sender CHECK (sender IN ('USER', 'BOT', 'SYSTEM'))
);

-- Índices para chatbot_message
CREATE INDEX idx_chatbot_message_session ON chatbot_message(session_id);
CREATE INDEX idx_chatbot_message_timestamp ON chatbot_message(timestamp);
CREATE INDEX idx_chatbot_message_type ON chatbot_message(message_type);

-- 4. Comentarios en las tablas
COMMENT ON TABLE chatbot_session IS 'Sesiones de conversación del ChatBot (Mesero Virtual)';
COMMENT ON COLUMN chatbot_session.session_id IS 'Identificador único de sesión UUID';
COMMENT ON COLUMN chatbot_session.context IS 'Contexto de la conversación en formato JSON (productos en carrito, etc.)';
COMMENT ON COLUMN chatbot_session.status IS 'Estado de la sesión: ACTIVE, COMPLETED, ABANDONED, ERROR';

COMMENT ON TABLE chatbot_message IS 'Mensajes individuales de las conversaciones del ChatBot';
COMMENT ON COLUMN chatbot_message.message_type IS 'Tipo de mensaje: TEXT, PRODUCT_SUGGESTION, COUPON_VALIDATION, ORDER_CONFIRMATION, ERROR';
COMMENT ON COLUMN chatbot_message.sender IS 'Remitente: USER (cliente), BOT (chatbot), SYSTEM (automático)';
COMMENT ON COLUMN chatbot_message.metadata IS 'Metadatos adicionales del mensaje en formato JSON';

-- 5. Actualizar órdenes existentes sin source
UPDATE client_order 
SET source = 'MANUAL' 
WHERE source IS NULL;
