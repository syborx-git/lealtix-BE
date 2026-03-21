-- =====================================================
-- V18: Revertir V17 - Restaurar business_id en Campaign
-- Fecha: 24 de febrero de 2026
-- Descripción: Revertir el cambio de tenant_id a business_id
-- =====================================================

-- 1. Crear columna business_id nuevamente
ALTER TABLE campaign 
ADD COLUMN business_id BIGINT;

-- 2. Copiar datos de tenant_id a business_id
UPDATE campaign 
SET business_id = tenant_id;

-- 3. Hacer business_id NOT NULL
ALTER TABLE campaign 
ALTER COLUMN business_id SET NOT NULL;

-- 4. Eliminar índices viejos (tenant_id)
DROP INDEX IF EXISTS idx_campaign_tenant;
DROP INDEX IF EXISTS idx_campaign_tenant_draft;

-- 5. Crear índices originales (business_id)
CREATE INDEX idx_campaign_business ON campaign(business_id);
CREATE INDEX idx_campaign_business_draft ON campaign(business_id, is_draft);

-- 6. Eliminar columna tenant_id
ALTER TABLE campaign 
DROP COLUMN IF EXISTS tenant_id;

-- 7. Verificación final
-- SELECT COUNT(*) FROM campaign WHERE business_id IS NULL;  -- Debería ser 0
