-- =====================================================================
-- V9: Seed de datos DEMO para validación local
-- Crea: 1 tenant demo + usuarios por rol con contraseñas conocidas
--
-- ⚠️  SOLO para ambiente local/dev — NO ejecutar en producción
--
-- Passwords (cifrado Base64, util EncrypUtils):
--   admin123    → YWRtaW4xMjM=
--   mesero123   → bWVzZXJvMTIz
--   cocina123   → Y29jaW5hMTIz
--   caja123     → Y2FqYTEyMw==
--   marketing123→ bWFya2V0aW5nMTIz
--   hostess123  → aG9zdGVzczEyMw==
-- =====================================================================

-- -----------------------------------------------------------------------
-- -----------------------------------------------------------------------
-- 0. Usuario propietario y Tenant demo
-- -----------------------------------------------------------------------
INSERT INTO app_user (
    full_name, fecha_nacimiento, telefono, email,
    password_hash, is_active, created_at, updated_at
)
VALUES (
    'Demo Admin Lealtix',
    '1990-05-15',
    '+52 55 1234 5678',
    'admin@demo.com',
    'YWRtaW4xMjM=',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO tenant (
    nombre_negocio, direccion, telefono, tipo_negocio,
    slug, uid_tenant, schedules,
    logo_url, slogan,
    kitchen_module_enabled, kitchen_enabled_at,
    is_active, created_at, updated_at,
    user_id
)
SELECT
    'Restaurante Demo',
    'Av. Insurgentes Sur 1234, Col. Del Valle, CDMX',
    '+52 55 1234 5678',
    'Restaurante',
    'demo',
    'UID-DEMO',
    'Lun-Vie: 08:00-22:00 | Sab-Dom: 09:00-23:00',
    'https://res.cloudinary.com/demo/image/upload/v1/lealtix/logo_demo.png',
    'Los mejores platillos de la ciudad',
    TRUE,
    NOW(),
    TRUE,
    NOW(),
    NOW(),
    (SELECT id FROM app_user WHERE email = 'admin@demo.com' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM tenant WHERE slug = 'demo');

-- -----------------------------------------------------------------------
-- 1. Usuarios demo (tenant_user) — contraseñas en Base64
-- -----------------------------------------------------------------------
DO $$
DECLARE
    v_tenant_id BIGINT;
BEGIN
    SELECT id INTO v_tenant_id FROM tenant WHERE slug = 'demo' LIMIT 1;

    IF v_tenant_id IS NULL THEN
        RAISE EXCEPTION 'No se encontró el tenant demo. Verifica que la tabla tenant tenga la columna slug.';
    END IF;

    -- ADMIN
    INSERT INTO tenant_user (tenant_id, nombre, email, password_hash, rol, activo, sueldo_mensual, created_at, updated_at)
    VALUES (v_tenant_id, 'Admin Demo', 'admin@demo.com', 'YWRtaW4xMjM=', 'ADMIN', TRUE, 15000.00, NOW(), NOW())
    ON CONFLICT (tenant_id, email) DO NOTHING;

    -- MESERO
    INSERT INTO tenant_user (tenant_id, nombre, email, password_hash, rol, activo, sueldo_mensual, created_at, updated_at)
    VALUES (v_tenant_id, 'Carlos Mesero', 'mesero@demo.com', 'bWVzZXJvMTIz', 'MESERO', TRUE, 8000.00, NOW(), NOW())
    ON CONFLICT (tenant_id, email) DO NOTHING;

    -- COCINA
    INSERT INTO tenant_user (tenant_id, nombre, email, password_hash, rol, activo, sueldo_mensual, created_at, updated_at)
    VALUES (v_tenant_id, 'Ana Cocina', 'cocina@demo.com', 'Y29jaW5hMTIz', 'COCINA', TRUE, 9000.00, NOW(), NOW())
    ON CONFLICT (tenant_id, email) DO NOTHING;

    -- CAJA
    INSERT INTO tenant_user (tenant_id, nombre, email, password_hash, rol, activo, sueldo_mensual, created_at, updated_at)
    VALUES (v_tenant_id, 'Luis Caja', 'caja@demo.com', 'Y2FqYTEyMw==', 'CAJA', TRUE, 7500.00, NOW(), NOW())
    ON CONFLICT (tenant_id, email) DO NOTHING;

    -- MARKETING
    INSERT INTO tenant_user (tenant_id, nombre, email, password_hash, rol, activo, sueldo_mensual, created_at, updated_at)
    VALUES (v_tenant_id, 'Sofia Marketing', 'marketing@demo.com', 'bWFya2V0aW5nMTIz', 'MARKETING', TRUE, 10000.00, NOW(), NOW())
    ON CONFLICT (tenant_id, email) DO NOTHING;

    -- HOSTESS
    INSERT INTO tenant_user (tenant_id, nombre, email, password_hash, rol, activo, sueldo_mensual, created_at, updated_at)
    VALUES (v_tenant_id, 'Maria Hostess', 'hostess@demo.com', 'aG9zdGVzczEyMw==', 'HOSTESS', TRUE, 7000.00, NOW(), NOW())
    ON CONFLICT (tenant_id, email) DO NOTHING;

    RAISE NOTICE 'Usuarios demo creados para tenant_id: %', v_tenant_id;
END $$;

-- -----------------------------------------------------------------------
-- 2. Tenant config demo
-- -----------------------------------------------------------------------
DO $$
DECLARE
    v_tenant_id BIGINT;
BEGIN
    SELECT id INTO v_tenant_id FROM tenant WHERE slug = 'demo' LIMIT 1;

    INSERT INTO tenant_config (
        tenant_id,
        history, vision,
        bussines_email,
        instagram, facebook,
        kitchen_module_enabled,
        updated_at
    )
    SELECT
        v_tenant_id,
        'Restaurante fundado en 2020, con pasión por la gastronomía local.',
        'Ser el restaurante más querido de la ciudad.',
        'demo@lealtix.com',
        '@restaurantedemo',
        'Restaurante Demo',
        TRUE,
        NOW()
    WHERE NOT EXISTS (
        SELECT 1 FROM tenant_config WHERE tenant_id = v_tenant_id
    );
END $$;

-- -----------------------------------------------------------------------
-- 3. Mesas demo
-- -----------------------------------------------------------------------
DO $$
DECLARE
    v_tenant_id BIGINT;
BEGIN
    SELECT id INTO v_tenant_id FROM tenant WHERE slug = 'demo' LIMIT 1;

    INSERT INTO mesa (tenant_id, nombre, numero, capacidad, estado, created_at, updated_at)
    VALUES
        (v_tenant_id, 'Mesa 1', 1, 4, 'LIBRE',   NOW(), NOW()),
        (v_tenant_id, 'Mesa 2', 2, 2, 'LIBRE',   NOW(), NOW()),
        (v_tenant_id, 'Mesa 3', 3, 6, 'LIBRE',   NOW(), NOW()),
        (v_tenant_id, 'Mesa 4', 4, 4, 'LIBRE',   NOW(), NOW()),
        (v_tenant_id, 'Barra',  5, 8, 'LIBRE',   NOW(), NOW())
    ON CONFLICT DO NOTHING;
END $$;
