-- =====================================================
-- V28: Asignar permiso dashboard_kitchen al rol COCINA
-- Fecha: 2026-03-29
-- Descripción: Asignar permiso de dashboard de cocina al rol COCINA
-- =====================================================

INSERT INTO role_permission (role, permission_id)
SELECT 'COCINA', p.id FROM permission p
WHERE p.code = 'dashboard_kitchen'
ON CONFLICT (role, permission_id) DO NOTHING;

-- También asignar al rol ADMIN (tiene acceso a todo)
INSERT INTO role_permission (role, permission_id)
SELECT 'ADMIN', p.id FROM permission p
WHERE p.code = 'dashboard_kitchen'
ON CONFLICT (role, permission_id) DO NOTHING;
