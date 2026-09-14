-- =====================================================
-- V17: Renombrar business_id a tenant_id en Campaign
-- Fecha: 24 de febrero de 2026
-- Descripción: Cambiar todas las referencias de business_id a tenant_id para consistencia
-- =====================================================

-- 1. Verificar si la columna existe con el nombre antiguo
-- SELECT EXISTS (
--     SELECT FROM information_schema.columns 
--     WHERE table_name='campaign' AND column_name='business_id'
-- );

-- 2. Crear columna temporal con datos
ALTER TABLE campaign 
ADD COLUMN tenant_id BIGINT;

-- 3. Copiar datos de business_id a tenant_id
UPDATE campaign 
SET tenant_id = business_id;

-- 4. Hacer tenant_id NOT NULL si business_id era NOT NULL
ALTER TABLE campaign 
ALTER COLUMN tenant_id SET NOT NULL;

-- 5. Actualizar índices - Primero eliminar los antiguos
DROP INDEX IF EXISTS idx_campaign_business;
DROP INDEX IF EXISTS idx_campaign_business_draft;

-- 6. Crear nuevos índices con tenant_id
CREATE INDEX idx_campaign_tenant ON campaign(tenant_id);
CREATE INDEX idx_campaign_tenant_draft ON campaign(tenant_id, is_draft);

-- 7. Eliminar la columna antigua
ALTER TABLE campaign 
DROP COLUMN IF EXISTS business_id;

-- 8. Verificación final
-- SELECT COUNT(*) FROM campaign WHERE tenant_id IS NULL;  -- Debería ser 0
