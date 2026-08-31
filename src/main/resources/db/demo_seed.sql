-- ==============================================================================
-- LEALTIX - SCRIPT DE DATOS DE DEMO / PRUEBA
-- ==============================================================================
-- Escenario: Restaurante "La Taquería Demo" listo para presentar la plataforma.
--
-- Tablas cubiertas:
--   role, app_user, tenant, tenant_config, tenant_user,
--   tenant_customer, tenant_menu_category, tenant_menu_product,
--   campaign, promotion_reward, campaign_result, coupon
--
-- Contraseña de TODOS los usuarios demo:  Demo2025!
--   BCrypt hash:  $2a$10$hIYO4NaVFOupU3G03sT1x.X5cB/pu6FX/z7JFVHy2Y1xzqJBH9W2O
--
-- Cómo ejecutar:
--   Opción A) Consola SQL de Neon → pegar y ejecutar.
--   Opción B) psql "tu_conexion_neon" -f demo_seed.sql
-- ==============================================================================

BEGIN;

-- [DEBUG] Descomentar para ver columnas reales de 'tenant' si hay error de columna:
-- SELECT column_name FROM information_schema.columns WHERE table_name = 'tenant' ORDER BY ordinal_position;

-- ==============================================================================
-- 1. ROLES DEL SISTEMA
-- ==============================================================================
INSERT INTO "role" (name, description)
VALUES
    ('ADMIN',      'Administrador con acceso total'),
    ('MESERO',     'Camarero / toma pedidos'),
    ('COCINA',     'Personal de cocina'),
    ('CAJA',       'Cajero / pagos'),
    ('MARKETING',  'Gestor de campañas y analytics')
ON CONFLICT (name) DO NOTHING;

-- ==============================================================================
-- 2. USUARIO PRINCIPAL (AppUser — dueño/propietario del negocio)
-- ==============================================================================
INSERT INTO app_user (
    full_name, fecha_nacimiento, telefono, email,
    password_hash, is_active, created_at, updated_at
)
VALUES (
    'Demo Admin Lealtix',
    '1990-05-15',
    '+52 55 1234 5678',
    'admin@lealtix-demo.com',
    '$2a$10$hIYO4NaVFOupU3G03sT1x.X5cB/pu6FX/z7JFVHy2Y1xzqJBH9W2O',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;

-- ==============================================================================
-- 3. TENANT (Negocio de demostración)
-- ==============================================================================
INSERT INTO tenant (
    nombre_negocio, direccion, telefono, tipo_negocio,
    slug, uid_tenant, schedules,
    logo_url, slogan,
    kitchen_module_enabled, kitchen_enabled_at,
    is_active, created_at, updated_at,
    user_id
)
VALUES (
    'La Taquería Demo',
    'Av. Insurgentes Sur 1234, Col. Del Valle, CDMX',
    '+52 55 9876 5432',
    'Restaurante',
    'la-taqueria-demo',
    'TENANT-DEMO-001',
    'Lun-Vie: 08:00-22:00 | Sab-Dom: 09:00-23:00',
    'https://res.cloudinary.com/demo/image/upload/v1/lealtix/logo_demo.png',
    'Los mejores tacos de la ciudad',
    true,
    NOW(),
    true,
    NOW(),
    NOW(),
    (SELECT id FROM app_user WHERE email = 'admin@lealtix-demo.com')
)
ON CONFLICT (slug) DO NOTHING;

-- Vincular el propietario (app_user) como tenant_user ADMIN del negocio
INSERT INTO tenant_user (
    tenant_id, nombre, email, password_hash,
    rol, activo, created_by, created_at, updated_at
)
SELECT
    t.id,
    'Demo Admin Lealtix',
    'admin@lealtix-demo.com',
    '$2a$10$hIYO4NaVFOupU3G03sT1x.X5cB/pu6FX/z7JFVHy2Y1xzqJBH9W2O',
    'ADMIN',
    true,
    'demo-seed',
    NOW(),
    NOW()
FROM tenant t
WHERE t.slug = 'la-taqueria-demo'
ON CONFLICT (tenant_id, email) DO NOTHING;

-- ==============================================================================
-- 4. CONFIGURACION DEL TENANT
-- ==============================================================================
INSERT INTO tenant_config (
    tenant_id, history, vision, bussines_email,
    twitter, facebook, instagram, tiktok, schedules,
    kitchen_module_enabled, created_at
)
SELECT
    t.id,
    'Fundada en 2010 por la familia Garcia, La Taqueria Demo nacio de la pasion por la cocina mexicana autentica.',
    'Ser el restaurante de referencia en experiencia de cliente y tecnologia de lealtad en Mexico.',
    'contacto@taqueria-demo.com',
    '@taqueria_demo',
    'facebook.com/taqueria.demo',
    '@taqueria.demo',
    '@taqueriademo_tiktok',
    'Lun-Vie: 08:00-22:00 | Sab-Dom: 09:00-23:00',
    true,
    NOW()
FROM tenant t
WHERE t.slug = 'la-taqueria-demo';

-- ==============================================================================
-- 5. USUARIOS DEL TENANT (5 empleados con distintos roles)
-- ==============================================================================
INSERT INTO tenant_user (
    tenant_id, nombre, email, password_hash,
    rol, activo, created_by, created_at, updated_at
)
SELECT
    t.id,
    u.nombre,
    u.email,
    '$2a$10$hIYO4NaVFOupU3G03sT1x.X5cB/pu6FX/z7JFVHy2Y1xzqJBH9W2O',
    u.rol,
    true,
    'demo-seed',
    NOW(),
    NOW()
FROM tenant t
CROSS JOIN (
    VALUES
      ('Admin Demo',      'admin.demo@taqueria.com',  'ADMIN'),
      ('Carlos Mesero',   'carlos@taqueria.com',       'MESERO'),
      ('Ana Cocina',      'ana.cocina@taqueria.com',   'COCINA'),
      ('Luis Caja',       'luis.caja@taqueria.com',    'CAJA'),
      ('Sofia Marketing', 'sofia.mkt@taqueria.com',    'MARKETING')
) AS u(nombre, email, rol)
WHERE t.slug = 'la-taqueria-demo'
ON CONFLICT (tenant_id, email) DO NOTHING;

-- ==============================================================================
-- 6. CLIENTES DEL TENANT (10 clientes con datos variados)
-- ==============================================================================
INSERT INTO tenant_customer (
    tenant_id, name, email, gender, birth_date,
    phone, created_at, updated_at,
    accepted_promotions, accepted_at, active
)
SELECT
    t.id,
    c.nombre,
    c.email,
    c.gender,
    c.birth_date::date,
    c.phone,
    NOW() - c.antiguedad::interval,
    NOW(),
    true,
    CURRENT_DATE,
    true
FROM tenant t
CROSS JOIN (
    VALUES
      ('Maria Gonzalez',  'maria.g@gmail.com',     'F', '1992-03-10', '+52 55 1111 2222', '90 days'),
      ('Juan Perez',      'juan.p@hotmail.com',     'M', '1985-07-22', '+52 55 3333 4444', '80 days'),
      ('Laura Martinez',  'laura.m@gmail.com',      'F', '1998-11-30', '+52 55 5555 6666', '70 days'),
      ('Roberto Silva',   'roberto.s@yahoo.com',    'M', '1979-01-15', '+52 55 7777 8888', '60 days'),
      ('Carmen Lopez',    'carmen.l@gmail.com',     'F', '2000-06-05', '+52 55 9999 0000', '50 days'),
      ('Diego Ramirez',   'diego.r@outlook.com',    'M', '1995-09-18', '+52 55 1212 3434', '40 days'),
      ('Valentina Cruz',  'vale.c@gmail.com',       'F', '1990-12-25', '+52 55 5656 7878', '30 days'),
      ('Arturo Mendoza',  'arturo.m@hotmail.com',   'M', '1988-04-02', '+52 55 9090 1212', '20 days'),
      ('Isabela Torres',  'isabela.t@gmail.com',    'F', '1997-08-14', '+52 55 3434 5656', '10 days'),
      ('Marcos Herrera',  'marcos.h@yahoo.com',     'M', '1983-02-28', '+52 55 7878 9090', '5 days')
) AS c(nombre, email, gender, birth_date, phone, antiguedad)
WHERE t.slug = 'la-taqueria-demo';

-- ==============================================================================
-- 7. CATEGORIAS DEL MENU
-- ==============================================================================
INSERT INTO tenant_menu_category (
    tenant_id, nombre, descripcion, is_active, display_order, created_at, updated_at
)
SELECT
    t.id,
    cat.nombre,
    cat.descripcion,
    true,
    cat.display_order,
    NOW(),
    NOW()
FROM tenant t
CROSS JOIN (
    VALUES
      ('Tacos',     'Nuestros tacos artesanales con tortilla hecha a mano',  1),
      ('Bebidas',   'Refrescos, aguas frescas y bebidas especiales',          2),
      ('Antojitos', 'Quesadillas, sopes, gorditas y mas',                     3),
      ('Postres',   'Nieve, churros y dulces tradicionales',                  4)
) AS cat(nombre, descripcion, display_order)
WHERE t.slug = 'la-taqueria-demo';

-- ==============================================================================
-- 8. PRODUCTOS DEL MENU
-- ==============================================================================
INSERT INTO tenant_menu_product (
    category_id, nombre, descripcion, precio, is_active, created_at, updated_at
)
SELECT
    mc.id,
    p.nombre,
    p.descripcion,
    p.precio::decimal(10,2),
    true,
    NOW(),
    NOW()
FROM tenant_menu_category mc
JOIN tenant t ON t.id = mc.tenant_id AND t.slug = 'la-taqueria-demo'
JOIN (
    VALUES
      ('Tacos',     'Taco de Pastor',        'Con pina, cilantro y cebolla. Tortilla de maiz artesanal',  35.00),
      ('Tacos',     'Taco de Suadero',       'Suadero dorado, salsa verde tatemada y guacamole fresco',   38.00),
      ('Tacos',     'Taco de Canasta',       'Taco tradicional estilo CDMX, de frijol o papas',           25.00),
      ('Bebidas',   'Agua de Jamaica',       'Agua fresca de jamaica sin azucar anadida, 500ml',          25.00),
      ('Bebidas',   'Refresco',              'Refresco embotellado 355ml surtido',                         22.00),
      ('Bebidas',   'Michelada',             'Cerveza con clamato, limon y salsas. La original',           65.00),
      ('Antojitos', 'Quesadilla de Hongos',  'Con hongos, epazote y queso Oaxaca en comal',               55.00),
      ('Antojitos', 'Sope de Chorizo',       'Sope grueso con chorizo, frijoles y crema',                 45.00),
      ('Postres',   'Nieve de Limon',        'Nieve artesanal de limon con ralladura, 2 bolas',            30.00),
      ('Postres',   'Churros con Chocolate', 'Tres churros crujientes con dip de chocolate caliente',      40.00)
) AS p(cat_nombre, nombre, descripcion, precio)
  ON mc.nombre = p.cat_nombre;

-- ==============================================================================
-- 9. CAMPANA ACTIVA DE DEMO
-- ==============================================================================
INSERT INTO campaign (
    business_id, title, subtitle, description,
    promo_type, start_date, end_date, status,
    call_to_action, channels, is_automatic, is_draft,
    published_at, created_at, updated_at,
    total_sent, total_failed, estimated_cost
)
SELECT
    t.id,
    'Tacolandia de Verano 2025',
    '20% de descuento en todo el menu',
    'Celebra el verano con nosotros. Presenta tu cupon digital y obtente 20% de descuento en cualquier orden minima de $150 pesos.',
    'DISCOUNT',
    CURRENT_DATE - INTERVAL '5 days',
    CURRENT_DATE + INTERVAL '25 days',
    'ACTIVE',
    'Canjear cupon ahora',
    'email,whatsapp',
    false,
    false,
    NOW() - INTERVAL '5 days',
    NOW() - INTERVAL '5 days',
    NOW(),
    8,
    0,
    500.00
FROM tenant t
WHERE t.slug = 'la-taqueria-demo';

-- ==============================================================================
-- 10. PROMOTION REWARD (beneficio de la campana)
-- ==============================================================================
INSERT INTO promotion_reward (
    campaign_id, reward_type, numeric_value,
    description, min_purchase_amount,
    usage_limit, usage_count, created_at, updated_at
)
SELECT
    c.id,
    'PERCENT_DISCOUNT',
    20.00,
    '20% de descuento en tu orden (compra minima $150)',
    150.00,
    100,
    8,
    NOW(),
    NOW()
FROM campaign c
JOIN tenant t ON t.id = c.business_id AND t.slug = 'la-taqueria-demo'
WHERE c.title = 'Tacolandia de Verano 2025';

-- ==============================================================================
-- 11. CAMPAIGN RESULT (metricas de demo)
-- ==============================================================================
INSERT INTO campaign_result (campaign_id, views, clicks, redemptions)
SELECT c.id, 145, 52, 8
FROM campaign c
JOIN tenant t ON t.id = c.business_id AND t.slug = 'la-taqueria-demo'
WHERE c.title = 'Tacolandia de Verano 2025';

-- ==============================================================================
-- 12. CUPONES
-- ==============================================================================
-- Cupon ACTIVO para Maria Gonzalez
INSERT INTO coupon (
    code, campaign_id, customer_id, status,
    expires_at, created_at, qr_token, qr_url
)
SELECT
    'DEMO-SUMMER-01',
    c.id,
    tc.id,
    'ACTIVE',
    NOW() + INTERVAL '25 days',
    NOW(),
    'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2',
    'http://5.161.82.24:8082/api/coupons/redeem/a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2'
FROM campaign c
JOIN tenant t ON t.id = c.business_id AND t.slug = 'la-taqueria-demo'
JOIN tenant_customer tc ON tc.tenant_id = t.id AND tc.email = 'maria.g@gmail.com'
WHERE c.title = 'Tacolandia de Verano 2025';

-- Cupon REDIMIDO para Juan Perez
INSERT INTO coupon (
    code, campaign_id, customer_id, status,
    expires_at, created_at, redeemed_at, redeemed_by, qr_token, qr_url
)
SELECT
    'DEMO-SUMMER-02',
    c.id,
    tc.id,
    'REDEEMED',
    NOW() + INTERVAL '25 days',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '1 day',
    'carlos@taqueria.com',
    'f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5',
    'http://5.161.82.24:8082/api/coupons/redeem/f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5'
FROM campaign c
JOIN tenant t ON t.id = c.business_id AND t.slug = 'la-taqueria-demo'
JOIN tenant_customer tc ON tc.tenant_id = t.id AND tc.email = 'juan.p@hotmail.com'
WHERE c.title = 'Tacolandia de Verano 2025';

COMMIT;

-- ==============================================================================
-- RESUMEN DE DATOS CREADOS
-- ==============================================================================
--  Tabla                  | Registros
--  -----------------------|----------------------------------------------------
--  role                   | 5  (ADMIN, MESERO, COCINA, CAJA, MARKETING)
--  app_user               | 1  admin@lealtix-demo.com
--  tenant                 | 1  "La Taqueria Demo"  slug: la-taqueria-demo
--  tenant_config          | 1  con redes sociales y horarios
--  tenant_user            | 5  empleados con distintos roles
--  tenant_customer        | 10 clientes con perfiles variados
--  tenant_menu_category   | 4  (Tacos, Bebidas, Antojitos, Postres)
--  tenant_menu_product    | 10 productos con precios reales
--  campaign               | 1  "Tacolandia de Verano 2025" STATUS=ACTIVE
--  promotion_reward       | 1  20% descuento, minimo $150
--  campaign_result        | 1  145 views | 52 clicks | 8 redenciones
--  coupon                 | 2  DEMO-SUMMER-01 (ACTIVE) + DEMO-SUMMER-02 (REDEEMED)

-- ==============================================================================
-- CREDENCIALES DEMO  (contrasena: Demo2025!)
-- ==============================================================================
--  ROL        | EMAIL                       | Panel
--  -----------|-----------------------------|----------------------------
--  ADMIN      | admin.demo@taqueria.com     | Dashboard completo
--  MESERO     | carlos@taqueria.com         | Vista comanda
--  COCINA     | ana.cocina@taqueria.com     | Vista cocina
--  CAJA       | luis.caja@taqueria.com      | Vista caja
--  MARKETING  | sofia.mkt@taqueria.com      | Vista campanas

-- ==============================================================================
-- LIMPIEZA — ejecutar solo para borrar datos de demo
-- ==============================================================================
-- BEGIN;
-- DELETE FROM coupon         WHERE code LIKE 'DEMO-%';
-- DELETE FROM campaign_result
--   WHERE campaign_id IN (SELECT id FROM campaign WHERE title LIKE '%Tacolandia%');
-- DELETE FROM promotion_reward
--   WHERE campaign_id IN (SELECT id FROM campaign WHERE title LIKE '%Tacolandia%');
-- DELETE FROM campaign       WHERE title LIKE '%Tacolandia%';
-- DELETE FROM tenant_menu_product
--   WHERE category_id IN (
--     SELECT mc.id FROM tenant_menu_category mc
--     JOIN tenant t ON t.id = mc.tenant_id WHERE t.slug = 'la-taqueria-demo');
-- DELETE FROM tenant_menu_category
--   WHERE tenant_id = (SELECT id FROM tenant WHERE slug = 'la-taqueria-demo');
-- DELETE FROM tenant_customer
--   WHERE tenant_id = (SELECT id FROM tenant WHERE slug = 'la-taqueria-demo');
-- DELETE FROM tenant_user
--   WHERE tenant_id = (SELECT id FROM tenant WHERE slug = 'la-taqueria-demo');
-- DELETE FROM tenant_config
--   WHERE tenant_id = (SELECT id FROM tenant WHERE slug = 'la-taqueria-demo');
-- DELETE FROM tenant        WHERE slug = 'la-taqueria-demo';
-- DELETE FROM app_user      WHERE email = 'admin@lealtix-demo.com';
-- COMMIT;
