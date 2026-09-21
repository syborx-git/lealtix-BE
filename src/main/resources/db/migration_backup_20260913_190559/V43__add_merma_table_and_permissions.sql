-- =====================================================
-- V43: Módulo de Mermas (salidas No-Venta) + permisos de Recetas/Mermas
-- Fecha: 2026-09-09
-- =====================================================

CREATE TABLE IF NOT EXISTS merma (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    ticket VARCHAR(20) NOT NULL,
    order_id UUID,
    registro_id UUID NOT NULL,
    tipo_merma VARCHAR(30) DEFAULT 'OPERATIVA',
    insumo_id BIGINT,
    insumo_nombre VARCHAR(120),
    producto_id BIGINT,
    producto_nombre VARCHAR(120),
    cantidad DOUBLE PRECISION NOT NULL,
    unidad VARCHAR(20),
    costo_unitario DOUBLE PRECISION DEFAULT 0,
    costo_total DOUBLE PRECISION DEFAULT 0,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_merma_tenant_fecha ON merma(tenant_id, fecha);
CREATE INDEX IF NOT EXISTS idx_merma_order_id ON merma(order_id);
CREATE INDEX IF NOT EXISTS idx_merma_registro_id ON merma(registro_id);

-- Permisos nuevos
INSERT INTO permission (code, name, description, resource, action, category) VALUES
('manage_recetas', 'Gestionar Recetas', 'Crear y editar recetas de platillos y sub-recetas', 'recipes', 'manage', 'admin'),
('manage_mermas', 'Gestionar Mermas', 'Registrar y consultar mermas (salidas no-venta)', 'mermas', 'manage', 'admin')
ON CONFLICT (code) DO NOTHING;

-- Admin conserva acceso total (los permisos nuevos se le otorgan explícitamente)
INSERT INTO role_permission (role, permission_id)
SELECT 'ADMIN', p.id FROM permission p WHERE p.code IN ('manage_recetas', 'manage_mermas')
ON CONFLICT (role, permission_id) DO NOTHING;