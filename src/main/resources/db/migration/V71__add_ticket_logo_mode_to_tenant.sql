-- Agrega la columna para la posición/modo de visualización del logo en el ticket (HEADER, WATERMARK, BOTH).
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS ticket_logo_mode VARCHAR(20) DEFAULT 'HEADER';
