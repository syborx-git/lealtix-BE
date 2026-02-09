-- V009__allow_sending_in_campaign_status_check.sql
-- Añade el estado 'SENDING' a la restricción CHECK de la columna status en la tabla campaign.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_class t ON c.conrelid = t.oid
        WHERE t.relname = 'campaign' AND c.conname = 'campaign_status_check'
    ) THEN
        ALTER TABLE campaign DROP CONSTRAINT campaign_status_check;
        RAISE NOTICE 'Constraint campaign_status_check eliminada';
    END IF;

    ALTER TABLE campaign
      ADD CONSTRAINT campaign_status_check CHECK (status IN (
        'DRAFT','READY','SENDING','ACTIVE','INACTIVE','SCHEDULED'
    ));
    RAISE NOTICE 'Constraint campaign_status_check creada/actualizada con SENDING';
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'No se pudo actualizar campaign_status_check: %', SQLERRM;
END $$;
