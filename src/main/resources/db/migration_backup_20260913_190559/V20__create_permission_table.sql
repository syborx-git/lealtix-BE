-- =====================================================
-- V20: Crear tabla permission
-- Fecha: 2026-03-21
-- Descripción: Catálogo maestro de permisos del sistema
-- =====================================================

CREATE TABLE IF NOT EXISTS permission (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    resource VARCHAR(100),           
    action VARCHAR(50),               
    category VARCHAR(50),             
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para permission
CREATE INDEX IF NOT EXISTS idx_permission_code ON permission(code);
CREATE INDEX IF NOT EXISTS idx_permission_resource ON permission(resource);
CREATE INDEX IF NOT EXISTS idx_permission_action ON permission(action);
CREATE INDEX IF NOT EXISTS idx_permission_category ON permission(category);

-- Insertar permisos base según especificación FE
-- Gestión de Usuarios (ADMIN)
INSERT INTO permission (code, name, description, resource, action, category) VALUES
('view_users', 'Ver Usuarios', 'Listar y visualizar usuarios del tenant', 'users', 'view', 'admin'),
('create_user', 'Crear Usuario', 'Crear nuevo usuario en el tenant', 'users', 'create', 'admin'),
('edit_user', 'Editar Usuario', 'Modificar datos de un usuario', 'users', 'edit', 'admin'),
('delete_user', 'Eliminar Usuario', 'Eliminar un usuario del tenant', 'users', 'delete', 'admin'),
('manage_user_roles', 'Asignar Roles', 'Asignar y modificar roles de usuarios', 'users', 'assign_roles', 'admin')
ON CONFLICT (code) DO NOTHING;

-- Gestión de Clientes
INSERT INTO permission (code, name, description, resource, action, category) VALUES
('view_customers', 'Ver Clientes', 'Consultar información de clientes', 'customers', 'view', 'operations'),
('create_customer', 'Crear Cliente', 'Registrar nuevo cliente', 'customers', 'create', 'operations'),
('edit_customer', 'Editar Cliente', 'Modificar información de cliente', 'customers', 'edit', 'operations'),
('delete_customer', 'Eliminar Cliente', 'Eliminar cliente del sistema', 'customers', 'delete', 'admin')
ON CONFLICT (code) DO NOTHING;

-- Menú y Productos
INSERT INTO permission (code, name, description, resource, action, category) VALUES
('view_menu', 'Ver Menú', 'Ver catálogo de productos del menú', 'menu', 'view', 'operations'),
('view_products', 'Ver Productos', 'Ver lista completa de productos', 'products', 'view', 'operations'),
('create_product', 'Crear Producto', 'Agregar nuevo producto al catálogo', 'products', 'create', 'admin'),
('edit_product', 'Editar Producto', 'Modificar información de producto', 'products', 'edit', 'admin'),
('delete_product', 'Eliminar Producto', 'Remover producto del catálogo', 'products', 'delete', 'admin'),
('manage_categories', 'Gestionar Categorías', 'Crear, editar y eliminar categorías', 'products', 'manage_categories', 'admin')
ON CONFLICT (code) DO NOTHING;

-- POS/Comanda
INSERT INTO permission (code, name, description, resource, action, category) VALUES
('create_order', 'Crear Orden', 'Crear nuevas órdenes/comandas', 'comanda', 'create', 'operations'),
('view_orders', 'Ver Órdenes', 'Visualizar órdenes del tenant', 'comanda', 'view', 'operations'),
('edit_order', 'Editar Orden', 'Modificar órdenes no pagadas', 'comanda', 'edit', 'operations'),
('view_order_details', 'Ver Detalles Orden', 'Ver detalles completos de una orden', 'comanda', 'view_details', 'operations'),
('process_payment', 'Procesar Pago', 'Procesar pagos de órdenes', 'comanda', 'process_payment', 'operations'),
('apply_discount', 'Aplicar Descuento', 'Aplicar descuentos y cupones a órdenes', 'comanda', 'apply_discount', 'operations')
ON CONFLICT (code) DO NOTHING;

-- Cocina (Kitchen)
INSERT INTO permission (code, name, description, resource, action, category) VALUES
('view_pending_orders', 'Ver Órdenes Pendientes', 'Ver órdenes pendientes de preparación', 'kitchen', 'view_pending', 'operations'),
('update_order_status', 'Actualizar Estado Orden', 'Cambiar estado de orden (en prep, listo, etc)', 'kitchen', 'update_status', 'operations'),
('view_kitchen_orders', 'Ver Órdenes de Cocina', 'Acceso a vista de cocina de órdenes', 'kitchen', 'view', 'operations')
ON CONFLICT (code) DO NOTHING;

-- Redenciones
INSERT INTO permission (code, name, description, resource, action, category) VALUES
('view_redemptions', 'Ver Redenciones', 'Visualizar historial de redenciones', 'redemptions', 'view', 'operations'),
('process_redemption', 'Procesar Redención', 'Procesar redención de cupones/promociones', 'redemptions', 'process', 'operations'),
('query_coupons', 'Consultar Cupones', 'Buscar y validar códigos de cupones', 'redemptions', 'query', 'operations'),
('view_coupon_status', 'Ver Estado Cupón', 'Verificar estado de un cupón', 'redemptions', 'view_status', 'operations')
ON CONFLICT (code) DO NOTHING;

-- Dashboard y Reportes
INSERT INTO permission (code, name, description, resource, action, category) VALUES
('view_dashboard', 'Ver Dashboard', 'Acceso a KPIs y métricas del sistema', 'dashboard', 'view', 'admin'),
('view_reports', 'Ver Reportes', 'Acceso a reportes analíticos', 'reports', 'view', 'admin')
ON CONFLICT (code) DO NOTHING;

-- Configuración (Admin)
INSERT INTO permission (code, name, description, resource, action, category) VALUES
('manage_settings', 'Gestionar Configuración', 'Modificar configuración general del tenant', 'settings', 'manage', 'admin'),
('manage_admin_page', 'Editar Admin Page', 'Configurar página de administración', 'settings', 'manage_admin', 'admin'),
('manage_landing_page', 'Editar Landing Page', 'Configurar página pública del tenant', 'settings', 'manage_landing', 'admin'),
('manage_campaigns', 'Gestionar Campañas', 'Crear, editar y eliminar campañas de promoción', 'campaigns', 'manage', 'admin'),
('view_campaign_templates', 'Ver Plantillas Campañas', 'Acceder a plantillas de campañas', 'campaigns', 'view_templates', 'admin'),
('manage_campaign_templates', 'Gestionar Plantillas', 'Crear y editar plantillas de campañas', 'campaigns', 'manage_templates', 'admin')
ON CONFLICT (code) DO NOTHING;

-- Impresión
INSERT INTO permission (code, name, description, resource, action, category) VALUES
('print_menu', 'Imprimir Menú', 'Generar e imprimir menú en formato clásico', 'menu', 'print', 'operations')
ON CONFLICT (code) DO NOTHING;

-- Permisos especiales de administración
INSERT INTO permission (code, name, description, resource, action, category) VALUES
('manage_permissions', 'Gestionar Permisos', 'Asignar y revocar permisos a roles', 'permissions', 'manage', 'admin'),
('view_permissions', 'Ver Permisos', 'Visualizar permisos y roles disponibles', 'permissions', 'view', 'admin')
ON CONFLICT (code) DO NOTHING;
