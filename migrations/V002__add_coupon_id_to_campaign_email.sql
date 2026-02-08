-- V002__add_coupon_id_to_campaign_email.sql
-- Añade la columna coupon_id a campaign_email y la FK a coupon(id)
-- Compatible con PostgreSQL y H2 (DDL simple)

-- 1) Agregar columna (nullable - no rompe datos existentes)
ALTER TABLE campaign_email ADD COLUMN coupon_id BIGINT;

-- 2) Crear índice para búsquedas por coupon_id
CREATE INDEX IF NOT EXISTS idx_campaign_email_coupon_id ON campaign_email(coupon_id);

-- 3) Añadir constraint FK (si la tabla coupon existe)
-- En PostgreSQL: agregar FK con ON DELETE SET NULL
-- En H2: referencia igualmente funciona
ALTER TABLE campaign_email
    ADD CONSTRAINT fk_campaign_email_coupon
    FOREIGN KEY (coupon_id) REFERENCES coupon(id) ON DELETE SET NULL;
