-- =====================================================
-- V30: Asignar permiso dashboard_mesero al rol MESERO
-- Fecha: 2026-03-30
-- Descripción: Asignar permiso de dashboard mesero únicamente al rol MESERO (NO al ADMIN)
-- =====================================================

INSERT INTO role_permission (role, permission_id)
SELECT 'MESERO', p.id FROM permission p
WHERE p.code = 'dashboard_mesero'
ON CONFLICT (role, permission_id) DO NOTHING;
