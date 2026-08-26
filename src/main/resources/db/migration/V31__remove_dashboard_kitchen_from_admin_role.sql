-- =====================================================
-- V31: Remover permiso dashboard_kitchen de rol ADMIN
-- Fecha: 2026-03-30
-- Descripción: Eliminar la asignación del permiso dashboard_kitchen del rol ADMIN (mantener en COCINA)
-- =====================================================

DELETE FROM role_permission 
WHERE role = 'ADMIN' 
AND permission_id = (SELECT id FROM permission WHERE code = 'dashboard_kitchen');
