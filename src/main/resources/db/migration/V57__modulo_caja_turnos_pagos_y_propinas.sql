-- ============================================================================
-- V54: Módulo de Caja, Turnos, Pagos y Propinas (POS Restaurante)
-- Fecha: 2026-09-26
-- Descripción:
-- 1. Tabla turnos para apertura, arqueo y cierre de caja con fondo inicial.
-- 2. Tabla pagos para transacciones financieras reales vinculadas al turno y cajero.
-- 3. Tabla liquidaciones_propinas para auditoría de entrega de propinas a meseros.
-- 4. Modificaciones a client_order para ciclo financiero (ABIERTA, POR_COBRAR, PAGADA).
-- ============================================================================

-- 1. TABLA TURNOS
CREATE TABLE IF NOT EXISTS turnos (
    id_turno BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    id_cajero BIGINT NOT NULL,
    fecha_apertura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre TIMESTAMP,
    fondo_inicial NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (fondo_inicial >= 0),
    total_ingresos NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (total_ingresos >= 0),
    total_propinas NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (total_propinas >= 0),
    total_efectivo_declarado NUMERIC(10, 2) DEFAULT 0.00,
    diferencia_caja NUMERIC(10, 2) DEFAULT 0.00,
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO', 'CERRADO')),
    observaciones TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_turnos_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_turnos_cajero FOREIGN KEY (id_cajero) REFERENCES tenant_user(id) ON DELETE RESTRICT
);

-- Regla de integridad: Un cajero solo puede tener un turno ABIERTO a la vez por negocio
CREATE UNIQUE INDEX IF NOT EXISTS idx_cajero_turno_abierto_unique 
ON turnos (tenant_id, id_cajero) 
WHERE (estado = 'ABIERTO');

CREATE INDEX IF NOT EXISTS idx_turnos_tenant_fecha ON turnos(tenant_id, fecha_apertura DESC);

-- 2. MODIFICACIÓN A COMANDAS (client_order)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='client_order' AND column_name='id_turno') THEN
        ALTER TABLE client_order ADD COLUMN id_turno BIGINT;
        ALTER TABLE client_order ADD CONSTRAINT fk_client_order_turno
            FOREIGN KEY (id_turno) REFERENCES turnos(id_turno) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='client_order' AND column_name='fecha_impresion_ticket') THEN
        ALTER TABLE client_order ADD COLUMN fecha_impresion_ticket TIMESTAMP;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='client_order' AND column_name='propinas_liquidadas') THEN
        ALTER TABLE client_order ADD COLUMN propinas_liquidadas BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='client_order' AND column_name='fecha_liquidacion_propinas') THEN
        ALTER TABLE client_order ADD COLUMN fecha_liquidacion_propinas TIMESTAMP;
    END IF;
END $$;

-- Actualizar restricción de estados en client_order para incluir ABIERTA y POR_COBRAR
ALTER TABLE client_order DROP CONSTRAINT IF EXISTS chk_client_order_estado_financiero;
ALTER TABLE client_order DROP CONSTRAINT IF EXISTS client_order_estado_check;
ALTER TABLE client_order ADD CONSTRAINT chk_client_order_estado_financiero
CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'EN_PREPARACION', 'LISTO', 'PAGADA', 'CANCELADA', 'ABIERTA', 'POR_COBRAR', 'RECHAZADO'));

CREATE INDEX IF NOT EXISTS idx_client_order_turno ON client_order(id_turno);
CREATE INDEX IF NOT EXISTS idx_client_order_caja_estado ON client_order(tenant_id, estado);

-- 3. TABLA PAGOS
CREATE TABLE IF NOT EXISTS pagos (
    id_pago BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    id_comanda UUID NOT NULL,
    id_turno BIGINT NOT NULL,
    id_cajero BIGINT NOT NULL,
    metodo_pago VARCHAR(40) NOT NULL,
    monto_cuenta NUMERIC(10, 2) NOT NULL CHECK (monto_cuenta >= 0),
    monto_propina NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (monto_propina >= 0),
    monto_total NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    referencia VARCHAR(100),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'APLICADO' CHECK (estado IN ('APLICADO', 'ANULADO')),

    CONSTRAINT fk_pagos_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_pagos_comanda FOREIGN KEY (id_comanda) REFERENCES client_order(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pagos_turno FOREIGN KEY (id_turno) REFERENCES turnos(id_turno) ON DELETE RESTRICT,
    CONSTRAINT fk_pagos_cajero FOREIGN KEY (id_cajero) REFERENCES tenant_user(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_pagos_turno ON pagos(id_turno);
CREATE INDEX IF NOT EXISTS idx_pagos_comanda ON pagos(id_comanda);
CREATE INDEX IF NOT EXISTS idx_pagos_fecha ON pagos(tenant_id, fecha DESC);

-- 4. TABLA LIQUIDACIONES DE PROPINAS
CREATE TABLE IF NOT EXISTS liquidaciones_propinas (
    id_liquidacion BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    id_turno BIGINT NOT NULL,
    id_mesero BIGINT NOT NULL,
    id_cajero BIGINT NOT NULL,
    monto_bruto NUMERIC(10, 2) NOT NULL,
    porcentaje_retencion NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    monto_retencion NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    monto_neto_pagado NUMERIC(10, 2) NOT NULL,
    fecha_pago TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_liq_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_liq_turno FOREIGN KEY (id_turno) REFERENCES turnos(id_turno) ON DELETE RESTRICT,
    CONSTRAINT fk_liq_mesero FOREIGN KEY (id_mesero) REFERENCES tenant_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_liq_cajero FOREIGN KEY (id_cajero) REFERENCES tenant_user(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_liq_turno_mesero ON liquidaciones_propinas(id_turno, id_mesero);
