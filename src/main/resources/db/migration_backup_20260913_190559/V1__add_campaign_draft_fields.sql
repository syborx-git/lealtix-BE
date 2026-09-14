-- Add draft functionality to campaigns table
-- Add new columns only if they don't exist
DO $$
BEGIN
    -- Add is_draft column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'campaign' AND column_name = 'is_draft') THEN
        ALTER TABLE campaign ADD COLUMN is_draft BOOLEAN DEFAULT FALSE NOT NULL;
    END IF;

    -- Add published_at column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'campaign' AND column_name = 'published_at') THEN
        ALTER TABLE campaign ADD COLUMN published_at TIMESTAMP NULL;
    END IF;

    -- Add is_automatic column if it doesn't exist (might exist with different name)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'campaign' AND column_name = 'is_automatic') THEN
        -- Check if it exists as isAutomatic and rename it
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'campaign' AND column_name = 'isAutomatic') THEN
            ALTER TABLE campaign RENAME COLUMN "isAutomatic" TO is_automatic;
        ELSE
            ALTER TABLE campaign ADD COLUMN is_automatic BOOLEAN DEFAULT FALSE NOT NULL;
        END IF;
    END IF;
END
$$;

-- Update existing draft campaigns to have proper status
UPDATE campaign SET is_draft = TRUE WHERE status = 'DRAFT';

-- Create index for efficient draft queries if it doesn't exist
CREATE INDEX IF NOT EXISTS idx_campaign_business_draft ON campaign(business_id, is_draft);
