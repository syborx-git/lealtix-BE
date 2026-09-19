-- =====================================================================
-- V19: Módulo Seat Management - División de cuenta con folio derivado
-- Crea comanda_pago (sub-comandas de pago por asiento): registra cada
-- cobro parcial con un folio derivado del folio de la comanda original
-- (padre-hijo). La comanda original NO se duplica en client_order.
-- =====================================================================

CREATE TABLE IF NOT EXISTS comanda_pago (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id          UUID           NOT NULL,
    seat_id           UUID,
    folio             VARCHAR(50)    NOT NULL,
    folio_original    VARCHAR(50),
    total             NUMERIC(10,2)  NOT NULL DEFAULT 0.00,
    estado            VARCHAR(20)    NOT NULL DEFAULT 'PAGADA',
    paid_method       VARCHAR(20),
    payment_reference VARCHAR(255),
    paid_by           BIGINT,
    paid_at           TIMESTAMP,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_comanda_pago_order
        FOREIGN KEY (order_id)   REFERENCES client_order(id)  ON DELETE CASCADE,
    CONSTRAINT fk_comanda_pago_seat
        FOREIGN KEY (seat_id)    REFERENCES comanda_asiento(id) ON DELETE SET NULL,
    CONSTRAINT fk_comanda_pago_paid_by
        FOREIGN KEY (paid_by)    REFERENCES app_user(id)      ON DELETE SET NULL,
    CONSTRAINT chk_comanda_pago_total
        CHECK (total >= 0)
);

COMMENT ON TABLE  comanda_pago IS 'Sub-comanda de pago por asiento (división de cuenta): registro hijo con folio derivado';
COMMENT ON COLUMN comanda_pago.order_id IS 'Comanda original (padre). La comanda original NO se duplica';
COMMENT ON COLUMN comanda_pago.seat_id IS 'Asiento/persona al que corresponde esta sub-comanda';
COMMENT ON COLUMN comanda_pago.folio IS 'Folio derivado de la sub-comanda: <folio_original>-<letraAsiento> (ej: 12345-A)';
COMMENT ON COLUMN comanda_pago.folio_original IS 'Folio de la comanda original a la que pertenece (pagada_original_folio)';
COMMENT ON COLUMN comanda_pago.total IS 'Monto cobrado en esta sub-comanda';

CREATE INDEX IF NOT EXISTS idx_comanda_pago_order_id ON comanda_pago(order_id);
CREATE INDEX IF NOT EXISTS idx_comanda_pago_seat_id  ON comanda_pago(seat_id);
CREATE INDEX IF NOT EXISTS idx_comanda_pago_folio    ON comanda_pago(folio);