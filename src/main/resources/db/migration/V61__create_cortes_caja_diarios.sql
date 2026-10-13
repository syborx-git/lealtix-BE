-- ============================================================================
-- V61: Módulo de Cortes de Caja Diarios y Auditoría de Arqueo Físico
-- Fecha: 2026-10-08
-- Descripción:
-- 1. Tabla cortes_caja_diarios para registrar el arqueo y cierre del día.
-- 2. Restricción UNIQUE(tenant_id, fecha_corte) para asegurar exactamente UN solo corte por día.
-- 3. Registro inmutable con desglose por método de pago (sistema vs real vs diferencia),
--    comentarios de justificación y trazabilidad del cajero responsable.
-- ============================================================================

CREATE TABLE IF NOT EXISTS cortes_caja_diarios (
    id_corte_diario BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    id_cajero BIGINT NOT NULL,
    cajero_nombre VARCHAR(150),
    cajero_email VARCHAR(150),
    fecha_corte DATE NOT NULL,
    fecha_hora_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Montos calculados por el sistema (Teórico)
    sistema_efectivo NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    sistema_tarjeta NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    sistema_transferencia NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    sistema_otros NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    sistema_total NUMERIC(12, 2) NOT NULL DEFAULT 0.00,

    -- Montos reales contados/declarados en caja (Físico)
    real_efectivo NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    real_tarjeta NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    real_transferencia NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    real_otros NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    real_total NUMERIC(12, 2) NOT NULL DEFAULT 0.00,

    -- Diferencias (Real - Sistema)
    diferencia_efectivo NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    diferencia_tarjeta NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    diferencia_transferencia NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    diferencia_otros NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    diferencia_total NUMERIC(12, 2) NOT NULL DEFAULT 0.00,

    estado_diferencia VARCHAR(20) NOT NULL DEFAULT 'CUADRADO',
    comentarios TEXT,
    total_comandas BIGINT DEFAULT 0,
    total_articulos BIGINT DEFAULT 0,
    total_propinas NUMERIC(12, 2) DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_corte_diario_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_corte_diario_cajero FOREIGN KEY (id_cajero) REFERENCES tenant_user(id) ON DELETE RESTRICT,
    CONSTRAINT uk_corte_tenant_fecha UNIQUE (tenant_id, fecha_corte)
);

CREATE INDEX IF NOT EXISTS idx_cortes_diarios_tenant_fecha ON cortes_caja_diarios(tenant_id, fecha_corte DESC);
