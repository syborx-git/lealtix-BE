-- =====================================================================
-- V18: Módulo Seat Management - Asignación de ítems a asientos
-- Tabla intermedia comanda_asiento_item: vincula ítems de la comanda
-- (client_order_item) con el asiento/persona (comanda_asiento).
-- =====================================================================

CREATE TABLE IF NOT EXISTS comanda_asiento_item (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    seat_id    UUID      NOT NULL,
    item_id    UUID      NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_comanda_asiento_item_seat
        FOREIGN KEY (seat_id) REFERENCES comanda_asiento(id)      ON DELETE CASCADE,
    CONSTRAINT fk_comanda_asiento_item_item
        FOREIGN KEY (item_id) REFERENCES client_order_item(id)    ON DELETE CASCADE
);

COMMENT ON TABLE  comanda_asiento_item IS 'Vincula ítems de una comanda con el asiento/persona que los consume (para dividir cuenta)';
COMMENT ON COLUMN comanda_asiento_item.item_id IS 'ID del ítem de la comanda (único: un ítem se asigna a un solo asiento)';

CREATE INDEX IF NOT EXISTS idx_comanda_asiento_item_seat  ON comanda_asiento_item(seat_id);
CREATE INDEX IF NOT EXISTS idx_comanda_asiento_item_item  ON comanda_asiento_item(item_id);