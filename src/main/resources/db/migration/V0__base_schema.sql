-- =====================================================================
-- V0: Esquema Base Fundacional (Pre-existente a migraciones incrementales)
-- Define las 19 tablas del modelo base de Lealtix
-- Permite que la base de datos se levante limpiamente desde cero en local
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- -----------------------------------------------------------------------
-- 1. Catálogo de Roles del Sistema
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) UNIQUE NOT NULL,
    description VARCHAR(255)
);

INSERT INTO role (name, description) VALUES
    ('ADMIN',      'Administrador con acceso total'),
    ('MESERO',     'Camarero / toma pedidos'),
    ('COCINA',     'Personal de cocina'),
    ('CAJA',       'Cajero / pagos'),
    ('MARKETING',  'Gestor de campañas y analytics'),
    ('HOSTESS',    'Recepción y gestión de mesas')
ON CONFLICT (name) DO NOTHING;

-- -----------------------------------------------------------------------
-- 2. Usuario Propietario / General (AppUser)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_user (
    id               BIGSERIAL PRIMARY KEY,
    full_name        VARCHAR(255),
    fecha_nacimiento DATE,
    telefono         VARCHAR(255),
    email            VARCHAR(255) UNIQUE NOT NULL,
    password_hash    VARCHAR(255),
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------
-- 3. Asignación de Roles a AppUser (UserRole)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role(id)     ON DELETE CASCADE
);

-- -----------------------------------------------------------------------
-- 4. Negocio / Inquilino (Tenant)
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
    updated_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tenant_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_tenant_slug ON tenant(slug);

-- -----------------------------------------------------------------------
-- 5. Configuración del Tenant (TenantConfig)
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
    updated_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tenant_config_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------
-- 6. Pagos y Suscripciones Stripe (TenantPayment)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tenant_payment (
    id                       BIGSERIAL PRIMARY KEY,
    tenant_id                BIGINT,
    stripe_customer_id       VARCHAR(255),
    stripe_subscription_id   VARCHAR(255),
    stripe_payment_method_id VARCHAR(255),
    plan                     VARCHAR(255),
    status                   VARCHAR(50),
    start_date               TIMESTAMP,
    end_date                 TIMESTAMP,
    description              VARCHAR(255),
    name                     VARCHAR(255),
    uidtenant                VARCHAR(255),
    uid_tenant               VARCHAR(255),
    amount                   BIGINT,
    stripe_payment_id        VARCHAR(255),
    stripe_mode              VARCHAR(50),
    user_email               VARCHAR(255),
    user_name                VARCHAR(255),
    receipt_url              VARCHAR(500),
    user_id                  BIGINT,
    created_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP,
    CONSTRAINT fk_tenant_payment_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)   ON DELETE SET NULL,
    CONSTRAINT fk_tenant_payment_user   FOREIGN KEY (user_id)   REFERENCES app_user(id) ON DELETE SET NULL
);

-- -----------------------------------------------------------------------
-- 7. Pre-registro de Landing Page (PreRegistro)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pre_registro (
    id             BIGSERIAL PRIMARY KEY,
    nombre         VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    status         VARCHAR(50)  NOT NULL,
    description    VARCHAR(255),
    fecha_registro TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------
-- 8. Invitaciones a la Plataforma (Invitations)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS invitations (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    token_hash    VARCHAR(128) NOT NULL UNIQUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at    TIMESTAMP    NOT NULL,
    used_at       TIMESTAMP,
    created_by_ip VARCHAR(45),
    notes         TEXT
);

-- -----------------------------------------------------------------------
-- 9. Auditoría de Correos Enviados (EmailLog)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS email_log (
    id                  BIGSERIAL PRIMARY KEY,
    entity_type         VARCHAR(50)  NOT NULL,
    entity_id           BIGINT       NOT NULL,
    email               VARCHAR(255) NOT NULL,
    template_name       VARCHAR(100) NOT NULL,
    sendgrid_message_id VARCHAR(255),
    status              VARCHAR(50)  NOT NULL DEFAULT 'pending',
    error_message       TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------
-- 10. Clientes del Tenant (TenantCustomer)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tenant_customer (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    name                VARCHAR(150) NOT NULL,
    email               VARCHAR(150) NOT NULL,
    gender              VARCHAR(10),
    birth_date          DATE,
    phone               VARCHAR(20),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_promotions BOOLEAN      NOT NULL DEFAULT TRUE,
    accepted_at         DATE,
    active              BOOLEAN      DEFAULT TRUE,
    CONSTRAINT fk_tenant_customer_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tenant_customer_tenant_created ON tenant_customer(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_tenant_customer_email_tenant   ON tenant_customer(email, tenant_id);

-- -----------------------------------------------------------------------
-- 11. Plantillas de Campañas (CampaignTemplate)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS campaign_template (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(150) NOT NULL,
    category            VARCHAR(100),
    default_title       VARCHAR(200),
    default_subtitle    VARCHAR(200),
    default_description VARCHAR(1000),
    default_image_url   VARCHAR(500),
    default_promo_type  VARCHAR(50),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE
);

-- -----------------------------------------------------------------------
-- 12. Campañas (Campaign)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS campaign (
    id             BIGSERIAL PRIMARY KEY,
    template_id    BIGINT,
    business_id    BIGINT NOT NULL,
    title          VARCHAR(200) NOT NULL,
    subtitle       VARCHAR(200),
    description    VARCHAR(2000),
    image_url      VARCHAR(500),
    promo_type     VARCHAR(50),
    promo_value    VARCHAR(255),
    start_date     DATE,
    end_date       DATE,
    status         VARCHAR(50),
    call_to_action VARCHAR(200),
    channels       VARCHAR(500),
    segmentation   TEXT,
    is_automatic   BOOLEAN NOT NULL DEFAULT FALSE,
    is_draft       BOOLEAN NOT NULL DEFAULT FALSE,
    published_at   TIMESTAMP,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_sent     INTEGER DEFAULT 0,
    total_failed   INTEGER DEFAULT 0,
    estimated_cost NUMERIC(10, 2),
    finished_at    TIMESTAMP,
    CONSTRAINT fk_campaign_template FOREIGN KEY (template_id) REFERENCES campaign_template(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_campaign_business    ON campaign(business_id);
CREATE INDEX IF NOT EXISTS idx_campaign_status      ON campaign(status);
CREATE INDEX IF NOT EXISTS idx_campaign_start_date  ON campaign(start_date);

-- -----------------------------------------------------------------------
-- 13. Métricas y Resultados de Campaña (CampaignResult)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS campaign_result (
    id                 BIGSERIAL PRIMARY KEY,
    campaign_id        BIGINT NOT NULL UNIQUE,
    views              INTEGER DEFAULT 0,
    clicks             INTEGER DEFAULT 0,
    redemptions        INTEGER DEFAULT 0,
    last_view_at       TIMESTAMP,
    last_click_at      TIMESTAMP,
    last_redemption_at TIMESTAMP,
    CONSTRAINT fk_campaign_result_campaign FOREIGN KEY (campaign_id) REFERENCES campaign(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------
-- 14. Registro de Redenciones de Cupones (CouponRedemption)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS coupon_redemption (
    id               VARCHAR(10) PRIMARY KEY,
    coupon_id        BIGINT NOT NULL,
    tenant_id        BIGINT NOT NULL,
    campaign_id      BIGINT NOT NULL,
    customer_email   VARCHAR(200),
    customer_name    VARCHAR(200),
    redeemed_by      VARCHAR(200) NOT NULL,
    channel          VARCHAR(50)  NOT NULL,
    ip_address       VARCHAR(45),
    user_agent       VARCHAR(500),
    location         VARCHAR(200),
    metadata         TEXT,
    original_amount  NUMERIC(10,2),
    purchase_amount  NUMERIC(10,2),
    discount_amount  NUMERIC(10,2),
    final_amount     NUMERIC(10,2),
    coupon_type      VARCHAR(50),
    coupon_value     NUMERIC(10,2),
    redeemed_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_redemption_coupon   ON coupon_redemption(coupon_id);
CREATE INDEX IF NOT EXISTS idx_redemption_tenant   ON coupon_redemption(tenant_id);
CREATE INDEX IF NOT EXISTS idx_redemption_campaign ON coupon_redemption(campaign_id);
CREATE INDEX IF NOT EXISTS idx_redemption_date     ON coupon_redemption(redeemed_at);
CREATE INDEX IF NOT EXISTS idx_redemption_channel  ON coupon_redemption(channel);

-- -----------------------------------------------------------------------
-- 15. Categorías de Menú del Tenant (TenantMenuCategory)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tenant_menu_category (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    nombre        VARCHAR(100) NOT NULL,
    descripcion   VARCHAR(500),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order INTEGER,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tmc_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------
-- 16. Productos del Menú (TenantMenuProduct)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tenant_menu_product (
    id                BIGSERIAL PRIMARY KEY,
    category_id       BIGINT        NOT NULL,
    precio            NUMERIC(10,2) NOT NULL,
    img_url           VARCHAR(255),
    nombre            VARCHAR(100)  NOT NULL,
    descripcion       VARCHAR(500),
    stock             DOUBLE PRECISION DEFAULT 0.0,
    stock_minimo      DOUBLE PRECISION DEFAULT 0.0,
    unidad            VARCHAR(20),
    venta_individual  BOOLEAN       DEFAULT FALSE,
    es_sub_receta     BOOLEAN       DEFAULT FALSE,
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
    auto_availability BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tmp_category FOREIGN KEY (category_id) REFERENCES tenant_menu_category(id) ON DELETE RESTRICT
);

-- -----------------------------------------------------------------------
-- 17. Insumos e Ingredientes (Insumo)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS insumo (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL,
    nombre       VARCHAR(100) NOT NULL,
    unidad       VARCHAR(20),
    es_bebida    BOOLEAN      NOT NULL DEFAULT FALSE,
    precio_venta NUMERIC(10,2),
    producto_id  BIGINT,
    stock        DOUBLE PRECISION DEFAULT 0.0,
    stock_minimo DOUBLE PRECISION DEFAULT 0.0,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_insumo_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------
-- 18. Recetas de Platillos (ProductRecipe)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_recipe (
    id              BIGSERIAL PRIMARY KEY,
    dish_product_id BIGINT        NOT NULL,
    insumo_id       BIGINT        NOT NULL,
    cantidad        NUMERIC(10,2) NOT NULL,
    modificable     BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_pr_dish   FOREIGN KEY (dish_product_id) REFERENCES tenant_menu_product(id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_insumo FOREIGN KEY (insumo_id)       REFERENCES insumo(id)              ON DELETE CASCADE
);

-- -----------------------------------------------------------------------
-- 19. Adicionales de Platillo (ProductAdditional)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_additional (
    id              BIGSERIAL PRIMARY KEY,
    dish_product_id BIGINT        NOT NULL,
    insumo_id       BIGINT        NOT NULL,
    cantidad        NUMERIC(10,2) NOT NULL,
    precio          NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_pa_dish   FOREIGN KEY (dish_product_id) REFERENCES tenant_menu_product(id) ON DELETE CASCADE,
    CONSTRAINT fk_pa_insumo FOREIGN KEY (insumo_id)       REFERENCES insumo(id)              ON DELETE CASCADE
);
