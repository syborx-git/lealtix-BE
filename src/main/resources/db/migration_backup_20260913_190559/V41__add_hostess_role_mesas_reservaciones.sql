-- =====================================================
-- V41: Rol HOSTESS + permisos de mesas y reservaciones
-- Fecha: 2026-09-08
-- Descripción: Agrega el rol HOSTESS, sus permisos y amplía
--   los CHECK constraints de rol para incluirlo.
-- =====================================================

-- 1. Ampliar CHECK constraints existentes
ALTER TABLE tenant_user DROP CONSTRAINT IF EXISTS chk_tenant_user_rol;
ALTER TABLE tenant_user ADD CONSTRAINT chk_tenant_user_rol CHECK (rol IN ('ADMIN', 'MESERO', 'COCINA', 'CAJA', 'MARKETING', 'HOSTESS'));

ALTER TABLE role_permission DROP CONSTRAINT IF EXISTS chk_role_permission_role;
ALTER TABLE role_permission ADD CONSTRAINT chk_role_permission_role CHECK (role IN ('ADMIN', 'MESERO', 'COCINA', 'CAJA', 'MARKETING', 'HOSTESS'));

-- 2. Permisos de mesas y reservaciones
INSERT INTO permission (code, name, description, resource, action, category) VALUES
('view_mesas', 'Ver Mesas', 'Visualizar el mapeo de mesas del local', 'mesas', 'view', 'operations'),
('manage_mesas', 'Gestionar Mesas', 'Crear, editar, eliminar y asignar mesas y meseros', 'mesas', 'manage', 'operations'),
('view_reservaciones', 'Ver Reservaciones', 'Visualizar las reservaciones del tenant', 'reservaciones', 'view', 'operations'),
('manage_reservaciones', 'Gestionar Reservaciones', 'Crear, cancelar y modificar reservaciones', 'reservaciones', 'manage', 'operations')
ON CONFLICT (code) DO NOTHING;

-- 3. Asignar permisos a HOSTESS
INSERT INTO role_permission (role, permission_id)
SELECT 'HOSTESS', p.id FROM permission p
WHERE p.code IN ('view_mesas', 'manage_mesas', 'view_reservaciones', 'manage_reservaciones')
ON CONFLICT (role, permission_id) DO NOTHING;

-- 4. Asignar los nuevos permisos también a ADMIN
INSERT INTO role_permission (role, permission_id)
SELECT 'ADMIN', p.id FROM permission p
WHERE p.code IN ('view_mesas', 'manage_mesas', 'view_reservaciones', 'manage_reservaciones')
ON CONFLICT (role, permission_id) DO NOTHING;