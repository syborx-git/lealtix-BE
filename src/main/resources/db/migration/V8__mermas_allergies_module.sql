-- =====================================================================
-- V8: Módulo Mermas y Alergias (V43 + V45)
-- =====================================================================

-- -----------------------------------------------------------------------
-- 1. Tabla merma (V43)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS merma (
    id              BIGSERIAL          PRIMARY KEY,
    tenant_id       BIGINT             NOT NULL,
    ticket          VARCHAR(20)        NOT NULL,
    order_id        UUID,
    registro_id     UUID               NOT NULL,
    tipo_merma      VARCHAR(30)        DEFAULT 'OPERATIVA',
    insumo_id       BIGINT,
    insumo_nombre   VARCHAR(120),
    producto_id     BIGINT,
    producto_nombre VARCHAR(120),
    cantidad        DOUBLE PRECISION   NOT NULL,
    unidad          VARCHAR(20),
    costo_unitario  DOUBLE PRECISION   DEFAULT 0,
    costo_total     DOUBLE PRECISION   DEFAULT 0,
    fecha           TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_merma_tenant_fecha ON merma(tenant_id, fecha);
CREATE INDEX IF NOT EXISTS idx_merma_order_id     ON merma(order_id);
CREATE INDEX IF NOT EXISTS idx_merma_registro_id  ON merma(registro_id);

-- -----------------------------------------------------------------------
-- 2. Tabla allergy (V45)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS allergy (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(150) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_allergy_name ON allergy(name);

-- -----------------------------------------------------------------------
-- 3. Tabla tenant_customer_allergy (V45)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tenant_customer_allergy (
    customer_id BIGINT    NOT NULL,
    allergy_id  BIGINT    NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (customer_id, allergy_id),
    CONSTRAINT fk_tca_customer FOREIGN KEY (customer_id) REFERENCES tenant_customer(id) ON DELETE CASCADE,
    CONSTRAINT fk_tca_allergy  FOREIGN KEY (allergy_id)  REFERENCES allergy(id)         ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tca_customer ON tenant_customer_allergy(customer_id);
CREATE INDEX IF NOT EXISTS idx_tca_allergy  ON tenant_customer_allergy(allergy_id);
