-- =====================================================
-- V29: Agregar permiso dashboard_mesero
-- Fecha: 2026-03-30
-- Descripción: Crear permiso para acceso al dashboard del mesero con métricas de venta y clientes VIP
-- =====================================================

INSERT INTO permission (code, name, description, resource, action, category)
VALUES (
    'dashboard_mesero',
    'Dashboard Mesero',
    'Acceso al dashboard especializado con métricas de desempeño, clientes VIP y recomendaciones de venta cruzada para equipos de meseros',
    'waiter',
    'dashboard',
    'sales'
)
ON CONFLICT (code) DO NOTHING;
