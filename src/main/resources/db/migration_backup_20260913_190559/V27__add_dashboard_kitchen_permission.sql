-- =====================================================
-- V27: Agregar permiso dashboard_kitchen
-- Fecha: 2026-03-29
-- Descripción: Crear permiso para acceso al dashboard de cocina
-- =====================================================

INSERT INTO permission (code, name, description, resource, action, category)
VALUES (
    'dashboard_kitchen',
    'Dashboard Cocina',
    'Acceso al dashboard especializado con métricas operativas y motivacionales para equipos de cocina',
    'kitchen',
    'dashboard',
    'operations'
)
ON CONFLICT (code) DO NOTHING;
