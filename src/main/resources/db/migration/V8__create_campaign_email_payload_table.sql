-- Create campaign_email_payload table for email audit
-- V8__create_campaign_email_payload_table.sql

CREATE TABLE IF NOT EXISTS campaign_email_payload (
    id UUID PRIMARY KEY,
    campaign_email_id UUID NOT NULL,
    email_subject VARCHAR(255),
    email_body TEXT,
    email_body_html TEXT,
    template_id VARCHAR(100),
    template_data TEXT,
    headers TEXT,
    attempt_number INTEGER,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_campaign_email_payload_campaign_email FOREIGN KEY (campaign_email_id) REFERENCES campaign_email(id) ON DELETE CASCADE
);

-- Create indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_campaign_email_payload_campaign_email ON campaign_email_payload(campaign_email_id);
CREATE INDEX IF NOT EXISTS idx_campaign_email_payload_created_at ON campaign_email_payload(created_at);
