-- V003__allow_sending_in_campaign_status_check.sql
-- Añade el estado 'SENDING' a la restricción CHECK de la columna status en la tabla campaign.
-- Compatible con PostgreSQL.

DO $$
BEGIN
    -- Si existe la constraint, la eliminamos antes de crear la nueva con SENDING
    IF EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_class t ON c.conrelid = t.oid
        WHERE t.relname = 'campaign' AND c.conname = 'campaign_status_check'
    ) THEN
        ALTER TABLE campaign DROP CONSTRAINT campaign_status_check;
        RAISE NOTICE 'Constraint campaign_status_check eliminada';
    END IF;

    -- Añadir la constraint incluyendo SENDING
    ALTER TABLE campaign
      ADD CONSTRAINT campaign_status_check CHECK (status IN (
        'DRAFT','READY','SENDING','ACTIVE','INACTIVE','SCHEDULED'
    ));
    RAISE NOTICE 'Constraint campaign_status_check creada/actualizada con SENDING';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'No se pudo actualizar campaign_status_check: %', SQLERRM;
END $$;
