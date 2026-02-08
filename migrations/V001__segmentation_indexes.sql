-- ============================================================================
-- MIGRATION: Agregar Índices para Optimización de Segmentación
-- ============================================================================
-- Ejecutar esta migración después de desplegar el código de segmentación
-- para optimizar las queries de filtrado de clientes.
--
-- Sintaxis: psql -U postgres -d lealtix_prod -f migrations/segmentation_indexes.sql
-- ============================================================================

-- Verificar que los índices no existan antes de crear
-- Esto previene errores si la migración se ejecuta múltiples veces

DO $$
BEGIN
    -- Índice para búsquedas por tenant + gender
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE schemaname = 'public' 
        AND tablename = 'tenant_customer' 
        AND indexname = 'idx_tenant_customer_tenant_gender'
    ) THEN
        CREATE INDEX idx_tenant_customer_tenant_gender 
        ON tenant_customer(tenant_id, gender);
        RAISE NOTICE 'Índice idx_tenant_customer_tenant_gender creado';
    ELSE
        RAISE NOTICE 'Índice idx_tenant_customer_tenant_gender ya existe';
    END IF;

    -- Índice para búsquedas por tenant + birth_date (cumpleaños)
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE schemaname = 'public' 
        AND tablename = 'tenant_customer' 
        AND indexname = 'idx_tenant_customer_tenant_birth'
    ) THEN
        CREATE INDEX idx_tenant_customer_tenant_birth 
        ON tenant_customer(tenant_id, birth_date);
        RAISE NOTICE 'Índice idx_tenant_customer_tenant_birth creado';
    ELSE
        RAISE NOTICE 'Índice idx_tenant_customer_tenant_birth ya existe';
    END IF;

    -- Índice para búsquedas por tenant + accepted_promotions
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE schemaname = 'public' 
        AND tablename = 'tenant_customer' 
        AND indexname = 'idx_tenant_customer_tenant_accepted'
    ) THEN
        CREATE INDEX idx_tenant_customer_tenant_accepted 
        ON tenant_customer(tenant_id, accepted_promotions);
        RAISE NOTICE 'Índice idx_tenant_customer_tenant_accepted creado';
    ELSE
        RAISE NOTICE 'Índice idx_tenant_customer_tenant_accepted ya existe';
    END IF;

    -- Índice para búsquedas por email + tenant (para búsquedas por email)
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE schemaname = 'public' 
        AND tablename = 'tenant_customer' 
        AND indexname = 'idx_tenant_customer_email_tenant'
    ) THEN
        CREATE INDEX idx_tenant_customer_email_tenant 
        ON tenant_customer(email, tenant_id);
        RAISE NOTICE 'Índice idx_tenant_customer_email_tenant creado';
    ELSE
        RAISE NOTICE 'Índice idx_tenant_customer_email_tenant ya existe';
    END IF;

    -- Índice para búsquedas por tenant + updated_at (actividad)
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE schemaname = 'public' 
        AND tablename = 'tenant_customer' 
        AND indexname = 'idx_tenant_customer_updated'
    ) THEN
        CREATE INDEX idx_tenant_customer_updated 
        ON tenant_customer(tenant_id, updated_at);
        RAISE NOTICE 'Índice idx_tenant_customer_updated creado';
    ELSE
        RAISE NOTICE 'Índice idx_tenant_customer_updated ya existe';
    END IF;

END $$;

-- Ejecutar VACUUM ANALYZE para actualizar estadísticas
-- Esto mejora el query planner después de crear índices
VACUUM ANALYZE tenant_customer;

-- Mostrar resumen de índices creados
\echo '=========================================='
\echo 'Índices de Segmentación Creados:'
\echo '=========================================='
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'tenant_customer' 
AND indexname LIKE 'idx_tenant_customer_%'
ORDER BY indexname;

-- ============================================================================
-- CAMPOS FUTUROS: DDL para campos que se necesitarán más adelante
-- ============================================================================
-- Descomenta estas líneas cuando sea el momento de agregar los campos

-- -- Campo LTV (Lifetime Value) para segmentación HIGH_LTV
-- ALTER TABLE tenant_customer ADD COLUMN ltv NUMERIC(12, 2) DEFAULT 0.00 NOT NULL;
-- CREATE INDEX idx_tenant_customer_ltv ON tenant_customer(tenant_id, ltv);

-- -- Campo lastPurchaseAt para segmentación NO_PURCHASE_60D
-- ALTER TABLE tenant_customer ADD COLUMN last_purchase_at TIMESTAMP;
-- CREATE INDEX idx_tenant_customer_last_purchase ON tenant_customer(tenant_id, last_purchase_at);

-- -- Campo lastActiveAt para mejorar segmentación ACTIVE_30D
-- ALTER TABLE tenant_customer ADD COLUMN last_active_at TIMESTAMP;
-- CREATE INDEX idx_tenant_customer_last_active ON tenant_customer(tenant_id, last_active_at);

-- -- Campo isVip para segmentación VIP
-- ALTER TABLE tenant_customer ADD COLUMN is_vip BOOLEAN DEFAULT false NOT NULL;
-- CREATE INDEX idx_tenant_customer_vip ON tenant_customer(tenant_id, is_vip);

-- ============================================================================
-- VERIFICACIÓN POST-MIGRACIÓN
-- ============================================================================
-- Ejecutar estas queries para verificar que todo funcionó correctamente

-- Contar índices creados
SELECT COUNT(*) as total_indexes
FROM pg_indexes 
WHERE tablename = 'tenant_customer' 
AND indexname LIKE 'idx_tenant_customer_%';

-- Verificar tamaño de índices
\echo '=========================================='
\echo 'Tamaño de Índices:'
\echo '=========================================='
SELECT 
  indexname,
  pg_size_pretty(pg_relation_size(indexrelid)) as size
FROM pg_indexes
WHERE tablename = 'tenant_customer'
  AND indexname LIKE 'idx_tenant_customer_%'
ORDER BY pg_relation_size(indexrelid) DESC;

-- ============================================================================
-- ROLLBACK (en caso de necesitar deshacer)
-- ============================================================================
-- Si necesitas deshacer esta migración, ejecuta:
-- 
-- DROP INDEX IF EXISTS idx_tenant_customer_tenant_gender;
-- DROP INDEX IF EXISTS idx_tenant_customer_tenant_birth;
-- DROP INDEX IF EXISTS idx_tenant_customer_tenant_accepted;
-- DROP INDEX IF EXISTS idx_tenant_customer_email_tenant;
-- DROP INDEX IF EXISTS idx_tenant_customer_updated;
-- VACUUM ANALYZE tenant_customer;

-- ============================================================================
-- NOTAS DE RENDIMIENTO
-- ============================================================================
-- 
-- Después de esta migración:
-- 1. Las queries de segmentación serán ~10-100x más rápidas
-- 2. El uso de memoria puede aumentar ~50MB por cada millón de registros
-- 3. El tiempo de INSERT/UPDATE puede aumentar ligeramente (~5-10%)
-- 4. Se recomienda ejecutar VACUUM ANALYZE frecuentemente durante pruebas
--
-- Monitoreo:
-- SELECT * FROM pg_stat_user_indexes WHERE relname = 'tenant_customer'
--   ORDER BY idx_scan DESC;
-- 
-- Para ver qué índices se usan más.

-- ============================================================================
