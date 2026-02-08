-- DEPRECATED: This development folder is no longer the canonical migrations location.
-- See src/main/resources/db/migration/V6__add_campaign_email_metrics.sql for the authoritative migration.
-- Do not edit files in this folder. Remove this folder or keep only backups.

-- Add campaign email metrics columns to campaign table
-- V6__add_campaign_email_metrics.sql

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'campaign' AND column_name = 'total_sent') THEN
        ALTER TABLE campaign ADD COLUMN total_sent INTEGER DEFAULT 0;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'campaign' AND column_name = 'total_failed') THEN
        ALTER TABLE campaign ADD COLUMN total_failed INTEGER DEFAULT 0;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'campaign' AND column_name = 'finished_at') THEN
        ALTER TABLE campaign ADD COLUMN finished_at TIMESTAMP NULL;
    END IF;
END
$$;

-- Create indexes for metrics queries
CREATE INDEX IF NOT EXISTS idx_campaign_metrics ON campaign(total_sent, total_failed);