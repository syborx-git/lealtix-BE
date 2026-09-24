-- =====================================================================
-- V5: Módulo Usuarios, Roles y Permisos (estado final consolidado)
-- Consolida: V19, V20, V21, V24(tenant_config), V25(tenant),
--            V27, V28, V29, V30, V31, V36, V41(constraints)
-- =====================================================================

-- -----------------------------------------------------------------------
-- 1. Modificaciones a tenant (V25)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tenant (
    id                     BIGSERIAL PRIMARY KEY,
    nombre_negocio         VARCHAR(255),
    direccion              VARCHAR(255),
    telefono               VARCHAR(255),
    tipo_negocio           VARCHAR(255),
    slug                   VARCHAR(255) UNIQUE,
    uidtenant              VARCHAR(255),
    uid_tenant             VARCHAR(255),
    schedules              VARCHAR(255),
    user_id                BIGINT,
    logo_url               VARCHAR(255),
    slogan                 VARCHAR(255),
    kitchen_module_enabled BOOLEAN DEFAULT FALSE,
    kitchen_enabled_at     TIMESTAMP,
    is_active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE tenant
    ADD COLUMN IF NOT EXISTS kitchen_module_enabled BOOLEAN,
    ADD COLUMN IF NOT EXISTS kitchen_enabled_at     TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_tenant_kitchen_enabled    ON tenant(kitchen_module_enabled);
CREATE INDEX IF NOT EXISTS idx_tenant_kitchen_enabled_at ON tenant(kitchen_enabled_at);

-- -----------------------------------------------------------------------
-- 2. Modificaciones a tenant_config (V24)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tenant_config (
    id                     BIGSERIAL PRIMARY KEY,
    tenant_id              BIGINT,
    history                VARCHAR(500),
    vision                 VARCHAR(500),
    bussines_email         VARCHAR(150),
    twitter                VARCHAR(255),
    facebook               VARCHAR(255),
    linkedin               VARCHAR(255),
    instagram              VARCHAR(255),
    tiktok                 VARCHAR(255),
    schedules              TEXT,
    kitchen_module_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    kitchen_enabled_at     TIMESTAMP,
    created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE tenant_config
    ADD COLUMN IF NOT EXISTS history                  VARCHAR(500),
    ADD COLUMN IF NOT EXISTS vision                   VARCHAR(500),
    ADD COLUMN IF NOT EXISTS bussines_email           VARCHAR(150),
    ADD COLUMN IF NOT EXISTS twitter                  VARCHAR(255),
    ADD COLUMN IF NOT EXISTS facebook                 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS linkedin                 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS instagram                VARCHAR(255),
    ADD COLUMN IF NOT EXISTS tiktok                   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS schedules                TEXT,
    ADD COLUMN IF NOT EXISTS kitchen_module_enabled   BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS kitchen_enabled_at       TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at               TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_tenant_config_tenant              ON tenant_config(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_config_kitchen_enabled     ON tenant_config(kitchen_module_enabled);
CREATE INDEX IF NOT EXISTS idx_tenant_config_kitchen_enabled_at  ON tenant_config(kitchen_enabled_at);
CREATE INDEX IF NOT EXISTS idx_tenant_config_updated_at          ON tenant_config(updated_at);

-- -----------------------------------------------------------------------
-- 3. Tabla tenant_user (estado FINAL: V19 + V36 + V41)
--    CHECK rol incluye HOSTESS desde el inicio
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tenant_user (
    id             BIGSERIAL    PRIMARY KEY,
    tenant_id      BIGINT       NOT NULL,
    nombre         VARCHAR(100) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    rol            VARCHAR(50)  NOT NULL,
    activo         BOOLEAN      DEFAULT TRUE,
    sueldo_mensual DOUBLE PRECISION DEFAULT 100.0,
    created_by     VARCHAR(150),
    updated_by     VARCHAR(150),
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tenant_user_tenant     FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT uk_tenant_user_email      UNIQUE (tenant_id, email),
    CONSTRAINT chk_tenant_user_rol       CHECK (rol IN ('ADMIN','MESERO','COCINA','CAJA','MARKETING','HOSTESS')),
    CONSTRAINT chk_tenant_user_activo    CHECK (activo IN (TRUE, FALSE))
);

CREATE INDEX IF NOT EXISTS idx_tenant_user_tenant        ON tenant_user(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_user_email         ON tenant_user(email);
CREATE INDEX IF NOT EXISTS idx_tenant_user_activo        ON tenant_user(activo);
CREATE INDEX IF NOT EXISTS idx_tenant_user_rol           ON tenant_user(rol);
CREATE INDEX IF NOT EXISTS idx_tenant_user_tenant_activo ON tenant_user(tenant_id, activo);
CREATE INDEX IF NOT EXISTS idx_tenant_user_tenant_sueldo ON tenant_user(tenant_id, sueldo_mensual);

-- -----------------------------------------------------------------------
-- 4. Tabla user_permission (V19)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_permission (
    id             BIGSERIAL    PRIMARY KEY,
    tenant_user_id BIGINT       NOT NULL,
    permission     VARCHAR(100) NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_permission_tenant_user FOREIGN KEY (tenant_user_id) REFERENCES tenant_user(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_permission             UNIQUE (tenant_user_id, permission)
);

CREATE INDEX IF NOT EXISTS idx_user_permission_tenant_user ON user_permission(tenant_user_id);
CREATE INDEX IF NOT EXISTS idx_user_permission_permission  ON user_permission(permission);

-- -----------------------------------------------------------------------
-- 5. Tabla permission con catálogo COMPLETO (V20 + V27 + V29 + V41 + V43)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS permission (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    resource    VARCHAR(100),
    action      VARCHAR(50),
    category    VARCHAR(50),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_permission_code     ON permission(code);
CREATE INDEX IF NOT EXISTS idx_permission_resource ON permission(resource);
CREATE INDEX IF NOT EXISTS idx_permission_action   ON permission(action);
CREATE INDEX IF NOT EXISTS idx_permission_category ON permission(category);

-- Insertar TODOS los permisos (incluyendo los de V27, V29, V41, V43)
INSERT INTO permission (code, name, description, resource, action, category) VALUES
-- Usuarios (admin)
('view_users',              'Ver Usuarios',              'Listar y visualizar usuarios del tenant',                      'users',          'view',             'admin'),
('create_user',             'Crear Usuario',             'Crear nuevo usuario en el tenant',                             'users',          'create',           'admin'),
('edit_user',               'Editar Usuario',            'Modificar datos de un usuario',                                'users',          'edit',             'admin'),
('delete_user',             'Eliminar Usuario',          'Eliminar un usuario del tenant',                               'users',          'delete',           'admin'),
('manage_user_roles',       'Asignar Roles',             'Asignar y modificar roles de usuarios',                        'users',          'assign_roles',     'admin'),
-- Clientes
('view_customers',          'Ver Clientes',              'Consultar información de clientes',                            'customers',      'view',             'operations'),
('create_customer',         'Crear Cliente',             'Registrar nuevo cliente',                                      'customers',      'create',           'operations'),
('edit_customer',           'Editar Cliente',            'Modificar información de cliente',                             'customers',      'edit',             'operations'),
('delete_customer',         'Eliminar Cliente',          'Eliminar cliente del sistema',                                 'customers',      'delete',           'admin'),
-- Menú y Productos
('view_menu',               'Ver Menú',                  'Ver catálogo de productos del menú',                           'menu',           'view',             'operations'),
('view_products',           'Ver Productos',             'Ver lista completa de productos',                              'products',       'view',             'operations'),
('create_product',          'Crear Producto',            'Agregar nuevo producto al catálogo',                           'products',       'create',           'admin'),
('edit_product',            'Editar Producto',           'Modificar información de producto',                            'products',       'edit',             'admin'),
('delete_product',          'Eliminar Producto',         'Remover producto del catálogo',                                'products',       'delete',           'admin'),
('manage_categories',       'Gestionar Categorías',      'Crear, editar y eliminar categorías',                          'products',       'manage_categories','admin'),
-- POS / Comanda
('create_order',            'Crear Orden',               'Crear nuevas órdenes/comandas',                                'comanda',        'create',           'operations'),
('view_orders',             'Ver Órdenes',               'Visualizar órdenes del tenant',                                'comanda',        'view',             'operations'),
('edit_order',              'Editar Orden',              'Modificar órdenes no pagadas',                                 'comanda',        'edit',             'operations'),
('view_order_details',      'Ver Detalles Orden',        'Ver detalles completos de una orden',                          'comanda',        'view_details',     'operations'),
('process_payment',         'Procesar Pago',             'Procesar pagos de órdenes',                                    'comanda',        'process_payment',  'operations'),
('apply_discount',          'Aplicar Descuento',         'Aplicar descuentos y cupones a órdenes',                       'comanda',        'apply_discount',   'operations'),
-- Cocina
('view_pending_orders',     'Ver Órdenes Pendientes',    'Ver órdenes pendientes de preparación',                        'kitchen',        'view_pending',     'operations'),
('update_order_status',     'Actualizar Estado Orden',   'Cambiar estado de orden',                                      'kitchen',        'update_status',    'operations'),
('view_kitchen_orders',     'Ver Órdenes de Cocina',     'Acceso a vista de cocina de órdenes',                          'kitchen',        'view',             'operations'),
('dashboard_kitchen',       'Dashboard Cocina',          'Dashboard especializado para equipos de cocina',               'kitchen',        'dashboard',        'operations'),
-- Mesero
('dashboard_mesero',        'Dashboard Mesero',          'Dashboard con métricas de desempeño y clientes VIP',           'waiter',         'dashboard',        'sales'),
-- Redenciones
('view_redemptions',        'Ver Redenciones',           'Visualizar historial de redenciones',                          'redemptions',    'view',             'operations'),
('process_redemption',      'Procesar Redención',        'Procesar redención de cupones/promociones',                    'redemptions',    'process',          'operations'),
('query_coupons',           'Consultar Cupones',         'Buscar y validar códigos de cupones',                          'redemptions',    'query',            'operations'),
('view_coupon_status',      'Ver Estado Cupón',          'Verificar estado de un cupón',                                 'redemptions',    'view_status',      'operations'),
-- Dashboard y Reportes
('view_dashboard',          'Ver Dashboard',             'Acceso a KPIs y métricas del sistema',                         'dashboard',      'view',             'admin'),
('view_reports',            'Ver Reportes',              'Acceso a reportes analíticos',                                 'reports',        'view',             'admin'),
-- Configuración
('manage_settings',         'Gestionar Configuración',   'Modificar configuración general del tenant',                   'settings',       'manage',           'admin'),
('manage_admin_page',       'Editar Admin Page',         'Configurar página de administración',                          'settings',       'manage_admin',     'admin'),
('manage_landing_page',     'Editar Landing Page',       'Configurar página pública del tenant',                         'settings',       'manage_landing',   'admin'),
('manage_campaigns',        'Gestionar Campañas',        'Crear, editar y eliminar campañas de promoción',               'campaigns',      'manage',           'admin'),
('view_campaign_templates', 'Ver Plantillas Campañas',   'Acceder a plantillas de campañas',                             'campaigns',      'view_templates',   'admin'),
('manage_campaign_templates','Gestionar Plantillas',     'Crear y editar plantillas de campañas',                        'campaigns',      'manage_templates', 'admin'),
-- Impresión
('print_menu',              'Imprimir Menú',             'Generar e imprimir menú en formato clásico',                   'menu',           'print',            'operations'),
-- Permisos de administración
('manage_permissions',      'Gestionar Permisos',        'Asignar y revocar permisos a roles',                           'permissions',    'manage',           'admin'),
('view_permissions',        'Ver Permisos',              'Visualizar permisos y roles disponibles',                      'permissions',    'view',             'admin'),
-- Mesas y Reservaciones (V41)
('view_mesas',              'Ver Mesas',                 'Visualizar el mapeo de mesas del local',                       'mesas',          'view',             'operations'),
('manage_mesas',            'Gestionar Mesas',           'Crear, editar, eliminar y asignar mesas y meseros',            'mesas',          'manage',           'operations'),
('view_reservaciones',      'Ver Reservaciones',         'Visualizar las reservaciones del tenant',                      'reservaciones',  'view',             'operations'),
('manage_reservaciones',    'Gestionar Reservaciones',   'Crear, cancelar y modificar reservaciones',                    'reservaciones',  'manage',           'operations'),
-- Recetas y Mermas (V43)
('manage_recetas',          'Gestionar Recetas',         'Crear y editar recetas de platillos y sub-recetas',            'recipes',        'manage',           'admin'),
('manage_mermas',           'Gestionar Mermas',          'Registrar y consultar mermas (salidas no-venta)',               'mermas',         'manage',           'admin')
ON CONFLICT (code) DO NOTHING;

-- -----------------------------------------------------------------------
-- 6. Tabla role_permission (V21 + V28 + V30 + V31 + V41 + V43)
--    Estado FINAL: ADMIN sin dashboard_kitchen, COCINA con dashboard_kitchen
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role_permission (
    id            BIGSERIAL  PRIMARY KEY,
    role          VARCHAR(50) NOT NULL,
    permission_id BIGINT      NOT NULL,
    granted_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_role_permission_permission
        FOREIGN KEY (permission_id) REFERENCES permission(id) ON DELETE CASCADE,
    CONSTRAINT chk_role_permission_role
        CHECK (role IN ('ADMIN','MESERO','COCINA','CAJA','MARKETING','HOSTESS')),
    CONSTRAINT uk_role_permission
        UNIQUE (role, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_role_permission_role            ON role_permission(role);
CREATE INDEX IF NOT EXISTS idx_role_permission_permission      ON role_permission(permission_id);
CREATE INDEX IF NOT EXISTS idx_role_permission_role_permission ON role_permission(role, permission_id);

-- ADMIN: todo EXCEPTO dashboard_kitchen y dashboard_mesero
INSERT INTO role_permission (role, permission_id)
SELECT 'ADMIN', p.id FROM permission p
WHERE p.code NOT IN ('dashboard_kitchen', 'dashboard_mesero')
ON CONFLICT (role, permission_id) DO NOTHING;

-- MESERO
INSERT INTO role_permission (role, permission_id)
SELECT 'MESERO', p.id FROM permission p
WHERE p.code IN (
    'view_customers','create_customer','edit_customer',
    'view_menu','view_products',
    'create_order','view_orders','edit_order','view_order_details','process_payment','apply_discount',
    'view_redemptions','process_redemption','query_coupons','view_coupon_status',
    'print_menu',
    'dashboard_mesero'
)
ON CONFLICT (role, permission_id) DO NOTHING;

-- COCINA
INSERT INTO role_permission (role, permission_id)
SELECT 'COCINA', p.id FROM permission p
WHERE p.code IN (
    'view_products',
    'view_pending_orders','update_order_status','view_kitchen_orders',
    'view_orders','view_order_details',
    'dashboard_kitchen'
)
ON CONFLICT (role, permission_id) DO NOTHING;

-- CAJA
INSERT INTO role_permission (role, permission_id)
SELECT 'CAJA', p.id FROM permission p
WHERE p.code IN (
    'view_orders','process_payment','apply_discount',
    'view_redemptions','process_redemption','query_coupons'
)
ON CONFLICT (role, permission_id) DO NOTHING;

-- MARKETING
INSERT INTO role_permission (role, permission_id)
SELECT 'MARKETING', p.id FROM permission p
WHERE p.code IN (
    'view_dashboard','view_reports',
    'manage_campaigns','view_campaign_templates','manage_campaign_templates',
    'view_redemptions','process_redemption','query_coupons'
)
ON CONFLICT (role, permission_id) DO NOTHING;

-- HOSTESS (V41)
INSERT INTO role_permission (role, permission_id)
SELECT 'HOSTESS', p.id FROM permission p
WHERE p.code IN ('view_mesas','manage_mesas','view_reservaciones','manage_reservaciones')
ON CONFLICT (role, permission_id) DO NOTHING;
