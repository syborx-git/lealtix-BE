-- Create campaign_email table for email tracking
-- V7__create_campaign_email_table.sql

CREATE TABLE IF NOT EXISTS campaign_email (
    id UUID PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(150),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER,
    scheduled_at TIMESTAMP,
    last_attempt_at TIMESTAMP,
    next_attempt_at TIMESTAMP,
    sent_at TIMESTAMP,
    opened_at TIMESTAMP,
    bounced_at TIMESTAMP,
    provider_name VARCHAR(100),
    provider_message_id VARCHAR(255),
    provider_error_code VARCHAR(100),
    provider_error_message TEXT,
    correlation_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_campaign_email_campaign FOREIGN KEY (campaign_id) REFERENCES campaign(id) ON DELETE CASCADE
);

-- Create indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_campaign_email_campaign ON campaign_email(campaign_id);
CREATE INDEX IF NOT EXISTS idx_campaign_email_status ON campaign_email(status);
CREATE INDEX IF NOT EXISTS idx_campaign_email_recipient ON campaign_email(recipient_email);
CREATE INDEX IF NOT EXISTS idx_campaign_email_provider_msg ON campaign_email(provider_message_id);
CREATE INDEX IF NOT EXISTS idx_campaign_email_next_attempt ON campaign_email(next_attempt_at) WHERE status = 'PENDING';
