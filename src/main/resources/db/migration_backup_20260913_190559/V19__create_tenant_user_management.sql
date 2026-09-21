-- =====================================================
-- V19: Crear tabla tenant_user y user_permission
-- Fecha: 2026-03-21
-- Descripción: Agrega soporte para usuarios por tenant con gestión de roles y permisos
-- =====================================================

-- 1. Crear tabla tenant_user
CREATE TABLE IF NOT EXISTS tenant_user (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(150),
    updated_by VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_tenant_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT uk_tenant_user_email UNIQUE (tenant_id, email),
    CONSTRAINT chk_tenant_user_rol CHECK (rol IN ('ADMIN', 'MESERO', 'COCINA', 'CAJA', 'MARKETING')),
    CONSTRAINT chk_tenant_user_activo CHECK (activo IN (TRUE, FALSE))
);

-- Índices para tenant_user
CREATE INDEX IF NOT EXISTS idx_tenant_user_tenant ON tenant_user(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_user_email ON tenant_user(email);
CREATE INDEX IF NOT EXISTS idx_tenant_user_activo ON tenant_user(activo);
CREATE INDEX IF NOT EXISTS idx_tenant_user_rol ON tenant_user(rol);
CREATE INDEX IF NOT EXISTS idx_tenant_user_tenant_activo ON tenant_user(tenant_id, activo);

-- 2. Crear tabla user_permission
CREATE TABLE IF NOT EXISTS user_permission (
    id BIGSERIAL PRIMARY KEY,
    tenant_user_id BIGINT NOT NULL,
    permission VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_user_permission_tenant_user FOREIGN KEY (tenant_user_id) REFERENCES tenant_user(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_permission UNIQUE (tenant_user_id, permission)
);

-- Índices para user_permission
CREATE INDEX IF NOT EXISTS idx_user_permission_tenant_user ON user_permission(tenant_user_id);
CREATE INDEX IF NOT EXISTS idx_user_permission_permission ON user_permission(permission);
