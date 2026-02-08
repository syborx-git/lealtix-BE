-- DEPRECATED: This development folder is no longer the canonical migrations location.
-- See src/main/resources/db/migration/V2__add_accepted_promotions_to_tenant_customer.sql for the authoritative migration.
-- Do not edit files in this folder. Remove this folder or keep only backups.

-- Add accepted_promotions and accepted_at columns to tenant_customer table
DO $$
BEGIN
    -- Add accepted_promotions column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'tenant_customer'
                   AND column_name = 'accepted_promotions') THEN
        ALTER TABLE tenant_customer
        ADD COLUMN accepted_promotions BOOLEAN NOT NULL DEFAULT true;
    END IF;

    -- Add accepted_at column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'tenant_customer'
                   AND column_name = 'accepted_at') THEN
        ALTER TABLE tenant_customer
        ADD COLUMN accepted_at DATE NULL;
    END IF;
END
$$;

-- Create index for efficient queries on promotion acceptance
CREATE INDEX IF NOT EXISTS idx_tenant_customer_accepted_promotions
ON tenant_customer(tenant_id, accepted_promotions);