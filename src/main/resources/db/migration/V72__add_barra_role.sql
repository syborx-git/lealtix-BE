-- V72: Agregar rol BARRA a restricciones CHECK de tenant_user y role_permission

-- 1. Actualizar check constraint en tenant_user
ALTER TABLE tenant_user DROP CONSTRAINT IF EXISTS chk_tenant_user_rol;
ALTER TABLE tenant_user ADD CONSTRAINT chk_tenant_user_rol 
    CHECK (rol IN ('ADMIN', 'MESERO', 'COCINA', 'CAJA', 'MARKETING', 'HOSTESS', 'BARRA'));

-- 2. Actualizar check constraint en role_permission
ALTER TABLE role_permission DROP CONSTRAINT IF EXISTS chk_role_permission_role;
ALTER TABLE role_permission ADD CONSTRAINT chk_role_permission_role 
    CHECK (role IN ('ADMIN', 'MESERO', 'COCINA', 'CAJA', 'MARKETING', 'HOSTESS', 'BARRA'));

-- 3. Asignar permisos por defecto al rol BARRA
INSERT INTO role_permission (role, permission_id)
SELECT 'BARRA', p.id FROM permission p
WHERE p.code IN (
    'view_products',
    'view_pending_orders',
    'update_order_status',
    'view_kitchen_orders',
    'view_orders',
    'view_order_details'
)
ON CONFLICT (role, permission_id) DO NOTHING;
