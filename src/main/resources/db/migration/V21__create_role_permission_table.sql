-- =====================================================
-- V21: Crear tabla role_permission
-- Fecha: 2026-03-21
-- Descripción: Asignación de permisos a roles del sistema
-- =====================================================

CREATE TABLE role_permission (
    id BIGSERIAL PRIMARY KEY,
    role VARCHAR(50) NOT NULL,
    permission_id BIGINT NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES permission(id) ON DELETE CASCADE,
    CONSTRAINT chk_role_permission_role CHECK (role IN ('ADMIN', 'MESERO', 'COCINA', 'CAJA', 'MARKETING')),
    CONSTRAINT uk_role_permission UNIQUE (role, permission_id)
);

-- Índices para role_permission
CREATE INDEX idx_role_permission_role ON role_permission(role);
CREATE INDEX idx_role_permission_permission ON role_permission(permission_id);
CREATE INDEX idx_role_permission_role_permission ON role_permission(role, permission_id);

-- =====================================================
-- ASIGNACIÓN DE PERMISOS A ROLES
-- =====================================================

-- ADMIN - Acceso total a todo
INSERT INTO role_permission (role, permission_id) 
SELECT 'ADMIN', p.id FROM permission p;

-- MESERO - Gestión de órdenes, clientes y redenciones
INSERT INTO role_permission (role, permission_id)
SELECT 'MESERO', p.id FROM permission p 
WHERE p.code IN (
    'view_customers', 'create_customer', 'edit_customer',
    'view_menu', 'view_products',
    'create_order', 'view_orders', 'edit_order', 'view_order_details', 'process_payment', 'apply_discount',
    'view_redemptions', 'process_redemption', 'query_coupons', 'view_coupon_status',
    'print_menu'
);

-- COCINA - Solo preparación de órdenes
INSERT INTO role_permission (role, permission_id)
SELECT 'COCINA', p.id FROM permission p 
WHERE p.code IN (
    'view_products',
    'view_pending_orders', 'update_order_status', 'view_kitchen_orders',
    'view_orders', 'view_order_details'
);

-- CAJA - Gestión de pagos y transacciones
INSERT INTO role_permission (role, permission_id)
SELECT 'CAJA', p.id FROM permission p 
WHERE p.code IN (
    'view_orders', 'process_payment', 'apply_discount',
    'view_redemptions', 'process_redemption', 'query_coupons'
);

-- MARKETING - Gestión de campañas y redenciones
INSERT INTO role_permission (role, permission_id)
SELECT 'MARKETING', p.id FROM permission p 
WHERE p.code IN (
    'view_dashboard', 'view_reports',
    'manage_campaigns', 'view_campaign_templates', 'manage_campaign_templates',
    'view_redemptions', 'process_redemption', 'query_coupons'
);
