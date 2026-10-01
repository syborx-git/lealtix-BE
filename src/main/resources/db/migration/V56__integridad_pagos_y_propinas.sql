-- ============================================================================
-- V53: Integridad de los datos de pago y propinas
--
-- 1) Propinas: no existia ningun campo donde guardarlas. La unica columna
--    "propina" estaba en comanda_ticket_pago, tabla que creo la migracion V50 y
--    que NINGUN codigo lee ni escribe: el detalle real de pagos vive en
--    comanda_pago. Sin esto el reporte de corte de caja no podia informar
--    propinas.
--    Se agregan en los dos lugares donde ahora se registran pagos:
--      - client_order.propina  -> pago directo, sin division de cuenta
--      - comanda_pago.propina  -> division de cuenta (pago parcial por asiento)
--
-- 2) comanda_pago.estado es un String libre. El reporte 1.2 filtra por
--    estado = 'PAGADA', asi que cualquier valor mal escrito (incluido NULL) se
--    convierte en dinero que no aparece en el corte. Se agrega un CHECK con los
--    estados reales que escribe ComandaAsientoServiceImpl, que es el unico
--    productor de la tabla.
--
-- 3) Se elimina comanda_ticket_pago: nada en el codigo la lee ni la escribe.
-- ============================================================================

-- ---------- 1) Propinas ----------

ALTER TABLE client_order
    ADD COLUMN IF NOT EXISTS propina NUMERIC(10, 2);

ALTER TABLE comanda_pago
    ADD COLUMN IF NOT EXISTS propina NUMERIC(10, 2);

-- Backfill: los registros previos no tienen propina, y las sumas con NULL
-- arruinarian el reporte. Se normalizan a 0.
UPDATE client_order SET propina = 0 WHERE propina IS NULL;
UPDATE comanda_pago  SET propina = 0 WHERE propina IS NULL;

ALTER TABLE client_order
    ALTER COLUMN propina SET DEFAULT 0,
    ALTER COLUMN propina SET NOT NULL;

ALTER TABLE comanda_pago
    ALTER COLUMN propina SET DEFAULT 0,
    ALTER COLUMN propina SET NOT NULL;

-- La propina nunca puede ser negativa ni tirar el total.
ALTER TABLE client_order
    DROP CONSTRAINT IF EXISTS chk_client_order_propina;
ALTER TABLE client_order
    ADD CONSTRAINT chk_client_order_propina CHECK (propina >= 0);

ALTER TABLE comanda_pago
    DROP CONSTRAINT IF EXISTS chk_comanda_pago_propina;
ALTER TABLE comanda_pago
    ADD CONSTRAINT chk_comanda_pago_propina CHECK (propina >= 0);

-- ---------- 2) Estado de comanda_pago tipado ----------

-- Valores que realmente escribe ComandaAsientoServiceImpl.
-- Si aparece alguno distinto, el CHECK lo rechaza en vez de dejar que el dinero
-- desaparezca del reporte de corte.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_comanda_pago_estado') THEN
        ALTER TABLE comanda_pago
            ADD CONSTRAINT chk_comanda_pago_estado
            CHECK (estado IN ('PAGADA', 'PENDIENTE', 'CANCELADA', 'ANULADA', 'REEMBOLSADA'));
    END IF;
END $$;

-- ---------- 3) Limpieza de la tabla muerta ----------

-- comanda_ticket_pago (V50) no tiene entidad JPA, repositorio ni consulta que
-- la lea. Conservarla solo genera confusion sobre cual es la fuente de verdad
-- de los pagos. El detalle real esta en comanda_pago.
DROP TABLE IF EXISTS comanda_ticket_pago;

COMMENT ON COLUMN client_order.propina IS
    'Propina del pago directo. No es ingreso: se excluye del reporte de ventas y el corte de caja la reporta aparte';
COMMENT ON COLUMN comanda_pago.propina IS
    'Propina del pago parcial por asiento. No es ingreso: se excluye del reporte de ventas y el corte de caja la reporta aparte';
