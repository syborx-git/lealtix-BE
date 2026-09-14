-- =====================================================
-- V42: Tablas mesa y reserva
-- Fecha: 2026-09-08
-- Descripción: Módulo de hostess: mapeo de mesas y reservaciones
-- =====================================================

CREATE TABLE IF NOT EXISTS mesa (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    numero INTEGER,
    capacidad INTEGER DEFAULT 4,
    estado VARCHAR(20) NOT NULL DEFAULT 'LIBRE',
    mesero_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mesa_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_mesa_mesero FOREIGN KEY (mesero_user_id) REFERENCES tenant_user(id) ON DELETE SET NULL,
    CONSTRAINT chk_mesa_estado CHECK (estado IN ('LIBRE', 'OCUPADA', 'RESERVADA'))
);

CREATE INDEX IF NOT EXISTS idx_mesa_tenant ON mesa(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mesa_tenant_estado ON mesa(tenant_id, estado);

CREATE TABLE IF NOT EXISTS reserva (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    cliente_nombre VARCHAR(150) NOT NULL,
    telefono VARCHAR(50),
    fecha TIMESTAMP NOT NULL,
    numero_personas INTEGER DEFAULT 1,
    mesa_id BIGINT,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    notas VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reserva_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_reserva_mesa FOREIGN KEY (mesa_id) REFERENCES mesa(id) ON DELETE SET NULL,
    CONSTRAINT chk_reserva_estado CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'CUMPLIDA'))
);

CREATE INDEX IF NOT EXISTS idx_reserva_tenant ON reserva(tenant_id);
CREATE INDEX IF NOT EXISTS idx_reserva_tenant_fecha ON reserva(tenant_id, fecha);
CREATE INDEX IF NOT EXISTS idx_reserva_tenant_estado ON reserva(tenant_id, estado);