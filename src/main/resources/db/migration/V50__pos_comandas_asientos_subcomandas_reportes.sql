-- ============================================================================
-- V50: Refactorización Comandas (Trazabilidad), Asientos y Sub-comandas (Caja)
-- Fecha: 2026-09-19
-- Descripción:
-- 1. Añade id_mesa, id_mesero, hora_apertura, hora_cierre a client_order.
-- 2. Permite id_cliente (customer_id) nulo para clientes generales/no registrados.
-- 3. Crea tabla comanda_asientos vinculada a client_order y asocia comanda_detalle (client_order_item).
-- 4. Crea estructura relacional de subcomandas (comanda_ticket_pago y comanda_ticket_asientos).
-- 5. Vista reporte_general_comandas_ventas para cruces y consolidados financieros.
-- ============================================================================

-- 1. REFACTORIZACIÓN TABLA PRINCIPAL COMANDAS (client_order)
DO $$
BEGIN
    -- Permitir que customer_id sea NULL para ventas generales / clientes no registrados
    ALTER TABLE client_order ALTER COLUMN customer_id DROP NOT NULL;

    -- id_mesa (FK a tabla mesa)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='client_order' AND column_name='id_mesa') THEN
        ALTER TABLE client_order ADD COLUMN id_mesa BIGINT;
        ALTER TABLE client_order ADD CONSTRAINT fk_client_order_mesa
            FOREIGN KEY (id_mesa) REFERENCES mesa(id) ON DELETE RESTRICT;
    END IF;

    -- id_mesero (FK a tabla tenant_user)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='client_order' AND column_name='id_mesero') THEN
        ALTER TABLE client_order ADD COLUMN id_mesero BIGINT;
        ALTER TABLE client_order ADD CONSTRAINT fk_client_order_mesero
            FOREIGN KEY (id_mesero) REFERENCES tenant_user(id) ON DELETE SET NULL;
    END IF;

    -- hora_apertura (Timestamp de inicio del servicio)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='client_order' AND column_name='hora_apertura') THEN
        ALTER TABLE client_order ADD COLUMN hora_apertura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
    END IF;

    -- hora_cierre (Timestamp de liquidación de cuenta)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='client_order' AND column_name='hora_cierre') THEN
        ALTER TABLE client_order ADD COLUMN hora_cierre TIMESTAMP;
    END IF;

    -- folio_comanda (Identificador legible de la comanda, ej. ORD-1001)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='client_order' AND column_name='folio_comanda') THEN
        ALTER TABLE client_order ADD COLUMN folio_comanda VARCHAR(50);
    END IF;
END $$;

-- Índices para optimizar reportes y búsquedas
CREATE INDEX IF NOT EXISTS idx_client_order_mesa ON client_order(id_mesa);
CREATE INDEX IF NOT EXISTS idx_client_order_mesero ON client_order(id_mesero);
CREATE INDEX IF NOT EXISTS idx_client_order_hora_apertura ON client_order(hora_apertura DESC);
CREATE INDEX IF NOT EXISTS idx_client_order_cierre ON client_order(hora_cierre);

-- 2. TABLA COMANDA_ASIENTOS (Seat Management por comensal)
CREATE TABLE IF NOT EXISTS comanda_asientos (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL,
    numero_asiento INTEGER NOT NULL,
    alias VARCHAR(100) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_comanda_asientos_order
        FOREIGN KEY (order_id) REFERENCES client_order(id) ON DELETE CASCADE,
    CONSTRAINT uq_order_asiento_num
        UNIQUE (order_id, numero_asiento),
    CONSTRAINT chk_asiento_estado
        CHECK (estado IN ('ACTIVO', 'PAGADO', 'CANCELADO'))
);

CREATE INDEX IF NOT EXISTS idx_comanda_asientos_order ON comanda_asientos(order_id);

-- 3. DETALLE DE PLATILLOS: ASOCIACIÓN CON ASIENTO
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='client_order_item' AND column_name='id_asiento') THEN
        ALTER TABLE client_order_item ADD COLUMN id_asiento BIGINT;
        ALTER TABLE client_order_item ADD CONSTRAINT fk_client_order_item_asiento
            FOREIGN KEY (id_asiento) REFERENCES comanda_asientos(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='client_order_item' AND column_name='asiento_alias') THEN
        ALTER TABLE client_order_item ADD COLUMN asiento_alias VARCHAR(100);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_client_order_item_asiento ON client_order_item(id_asiento);

-- 4. SUB-COMANDAS: FOLIOS DERIVADOS DE PAGO (División de Cuentas)
CREATE TABLE IF NOT EXISTS comanda_ticket_pago (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL,
    folio_pago VARCHAR(50) NOT NULL UNIQUE,      -- Ej: 12345-A, 12345-B
    subtotal NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    descuento NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    propina NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total_pagado NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    metodo_pago VARCHAR(30) NOT NULL,
    referencia VARCHAR(100),
    id_cajero BIGINT,
    fecha_pago TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ticket_pago_order
        FOREIGN KEY (order_id) REFERENCES client_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_pago_cajero
        FOREIGN KEY (id_cajero) REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_comanda_ticket_order ON comanda_ticket_pago(order_id);
CREATE INDEX IF NOT EXISTS idx_comanda_ticket_folio ON comanda_ticket_pago(folio_pago);

-- Tabla puente: relación de asientos liquidados por ticket
CREATE TABLE IF NOT EXISTS comanda_ticket_asientos (
    id BIGSERIAL PRIMARY KEY,
    id_ticket_pago BIGINT NOT NULL,
    id_asiento BIGINT NOT NULL,

    CONSTRAINT fk_ticket_asiento_ticket
        FOREIGN KEY (id_ticket_pago) REFERENCES comanda_ticket_pago(id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_asiento_asiento
        FOREIGN KEY (id_asiento) REFERENCES comanda_asientos(id) ON DELETE RESTRICT,
    CONSTRAINT uq_asiento_pagado_unico
        UNIQUE (id_asiento)
);

-- 5. VISTA DE CONSOLIDADO Y REPORTE GENERAL DE VENTAS
CREATE OR REPLACE VIEW vista_reporte_general_ventas AS
SELECT 
    co.id AS id_comanda,
    COALESCE(co.folio_comanda, 'ORD-' || SUBSTRING(co.id::text, 1, 8)) AS folio_comanda,
    co.hora_apertura,
    co.hora_cierre,
    CASE 
        WHEN co.hora_cierre IS NOT NULL THEN 
            TO_CHAR(co.hora_cierre - co.hora_apertura, 'HH24:MI:SS')
        ELSE 'EN SERVICIO'
    END AS duracion_servicio,
    m.id AS id_mesa,
    COALESCE(m.nombre, 'Sin mesa asignada') AS mesa_nombre,
    m.numero AS mesa_numero,
    u.id AS id_mesero,
    COALESCE(CONCAT(u.first_name, ' ', u.last_name), 'Mesero General') AS mesero_nombre,
    COALESCE(
        NULLIF(TRIM(CONCAT(tc.nombre, ' ', tc.apellido)), ''),
        'Cliente no registrado'
    ) AS cliente_nombre,
    tc.email AS cliente_email,
    COALESCE(SUM(tp.total_pagado), co.total) AS total_pagado,
    co.estado AS estado_comanda,
    COUNT(DISTINCT tp.id) AS subcomandas_emitidas
FROM client_order co
LEFT JOIN mesa m ON co.id_mesa = m.id
LEFT JOIN tenant_user u ON co.id_mesero = u.id
LEFT JOIN tenant_customer tc ON co.customer_id = tc.id
LEFT JOIN comanda_ticket_pago tp ON co.id = tp.order_id
GROUP BY 
    co.id, co.folio_comanda, co.hora_apertura, co.hora_cierre,
    m.id, m.nombre, m.numero, u.id, u.first_name, u.last_name,
    tc.nombre, tc.apellido, tc.email, co.total, co.estado;
