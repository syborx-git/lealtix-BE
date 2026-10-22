-- =====================================================
-- V69: Soporte de áreas múltiples en mesas (Principal, Terraza, Balcón, etc.)
-- =====================================================

ALTER TABLE mesa ADD COLUMN IF NOT EXISTS area VARCHAR(100) DEFAULT 'Principal';

-- Inicializar mesas existentes que no tengan área con 'Principal'
UPDATE mesa 
SET area = 'Principal' 
WHERE area IS NULL OR TRIM(area) = '';

-- Índice para búsquedas rápidas por tenant y área
CREATE INDEX IF NOT EXISTS idx_mesa_tenant_area ON mesa (tenant_id, area);
