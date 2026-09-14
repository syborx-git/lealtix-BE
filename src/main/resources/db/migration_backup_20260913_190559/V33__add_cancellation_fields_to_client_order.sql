-- Agregar campos de cancelación a client_order
-- Almacena: quién canceló, cuándo, y por qué

ALTER TABLE client_order
ADD COLUMN cancelled_by VARCHAR(255);
-- cancelled_by: Email del usuario que canceló (para auditoría)

ALTER TABLE client_order
ADD COLUMN cancelled_at TIMESTAMP;
-- cancelled_at: Timestamp de cuándo se realizó la cancelación

ALTER TABLE client_order
ADD COLUMN cancellation_reason VARCHAR(500);
-- cancellation_reason: Razón textual de por qué se canceló

-- Crear índices para queries de auditoría
CREATE INDEX idx_client_order_cancelled_by ON client_order(cancelled_by);
CREATE INDEX idx_client_order_cancelled_at ON client_order(cancelled_at);
CREATE INDEX idx_client_order_cancellation ON client_order(estado, cancelled_at) WHERE estado = 'CANCELADA';
