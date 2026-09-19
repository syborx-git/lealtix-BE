-- =====================================================================
-- V17: Módulo Seat Management - Trazabilidad de comandas de mesa
-- Refactor de client_order para soportar mesa, mesero, hora de apertura
-- y cliente de mesa; crea la tabla comanda_asiento (asientos/personas).
-- =====================================================================

-- -----------------------------------------------------------------------
-- 1. Nuevas columnas de trazabilidad en client_order
-- -----------------------------------------------------------------------
ALTER TABLE client_order ADD COLUMN IF NOT EXISTS mesa BIGINT;
ALTER TABLE client_order ADD COLUMN IF NOT EXISTS mesero_id BIGINT;
ALTER TABLE client_order ADD COLUMN IF NOT EXISTS hora_apertura TIMESTAMP;
ALTER TABLE client_order ADD COLUMN IF NOT EXISTS cliente_mesa_id BIGINT;

COMMENT ON COLUMN client_order.mesa IS 'ID de la mesa (mesa.id) donde se atiende la comanda';
COMMENT ON COLUMN client_order.mesero_id IS 'ID del mesero (app_user.id) responsable de la comanda';
COMMENT ON COLUMN client_order.hora_apertura IS 'Hora de apertura de la mesa (cuando se abre la comanda)';
COMMENT ON COLUMN client_order.cliente_mesa_id IS 'ID del cliente del asiento/comensal de mesa (tenant_customer.id), distinto de customer_id';

-- Backfill: las comandas existentes abren a la hora en que fueron registradas
UPDATE client_order SET hora_apertura = fecha WHERE hora_apertura IS NULL;

-- Restricciones de las nuevas columnas (convención del repo: fk_...)
ALTER TABLE client_order ADD CONSTRAINT fk_client_order_mesa
    FOREIGN KEY (mesa) REFERENCES mesa(id) ON DELETE SET NULL;
ALTER TABLE client_order ADD CONSTRAINT fk_client_order_mesero
    FOREIGN KEY (mesero_id) REFERENCES app_user(id) ON DELETE SET NULL;
ALTER TABLE client_order ADD CONSTRAINT fk_client_order_cliente_mesa
    FOREIGN KEY (cliente_mesa_id) REFERENCES tenant_customer(id) ON DELETE SET NULL;

-- Índices de las nuevas columnas (convención del repo: idx_...)
CREATE INDEX IF NOT EXISTS idx_client_order_mesa          ON client_order(mesa);
CREATE INDEX IF NOT EXISTS idx_client_order_mesero        ON client_order(mesero_id);
CREATE INDEX IF NOT EXISTS idx_client_order_hora_apertura ON client_order(hora_apertura);
CREATE INDEX IF NOT EXISTS idx_client_order_cliente_mesa  ON client_order(cliente_mesa_id);

-- -----------------------------------------------------------------------
-- 2. Tabla comanda_asiento (asiento/persona de una comanda)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS comanda_asiento (
    id         UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id   UUID           NOT NULL,
    tenant_id  BIGINT         NOT NULL,
    numero     INTEGER        NOT NULL DEFAULT 1,
    alias      VARCHAR(80),
    estado     VARCHAR(20)    NOT NULL DEFAULT 'ABIERTA',
    total      NUMERIC(10,2)  NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_comanda_asiento_order
        FOREIGN KEY (order_id)  REFERENCES client_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_comanda_asiento_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant(id)       ON DELETE CASCADE,
    CONSTRAINT chk_comanda_asiento_estado
        CHECK (estado IN ('ABIERTA','PAGADA','CANCELADA')),
    CONSTRAINT chk_comanda_asiento_total
        CHECK (total >= 0)
);

COMMENT ON TABLE  comanda_asiento               IS 'Asiento/persona de una comanda: permite dividir la cuenta por comensal';
COMMENT ON COLUMN comanda_asiento.numero        IS 'Número de asiento dentro de la comanda (1..N)';
COMMENT ON COLUMN comanda_asiento.alias         IS 'Alias/nombre opcional de la persona (comensal)';
COMMENT ON COLUMN comanda_asiento.estado        IS 'Estado del asiento: ABIERTA, PAGADA o CANCELADA';
COMMENT ON COLUMN comanda_asiento.total         IS 'Monto acumulado de los ítems asignados al asiento';

CREATE INDEX IF NOT EXISTS idx_comanda_asiento_order_id  ON comanda_asiento(order_id);
CREATE INDEX IF NOT EXISTS idx_comanda_asiento_tenant_id ON comanda_asiento(tenant_id);