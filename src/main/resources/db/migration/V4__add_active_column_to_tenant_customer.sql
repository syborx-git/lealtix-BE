-- Add active column to tenant_customer table for soft delete functionality
ALTER TABLE tenant_customer ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

-- Create index for active customers queries
CREATE INDEX IF NOT EXISTS idx_tenant_customer_active ON tenant_customer(tenant_id, active);
