-- =====================================================
-- V45: Alergias de clientes
-- Fecha: 2026-09-12
-- Descripción: Catálogo de alergias (allergy) y tabla
--              intermedia con tenant_customer
--              (tenant_customer_allergy). El nombre se
--              guarda normalizado (minúsculas, sin
--              acentos, singular).
-- =====================================================

CREATE TABLE IF NOT EXISTS allergy (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_allergy_name ON allergy (name);

CREATE TABLE IF NOT EXISTS tenant_customer_allergy (
    customer_id BIGINT NOT NULL,
    allergy_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (customer_id, allergy_id),
    CONSTRAINT fk_tca_customer FOREIGN KEY (customer_id) REFERENCES tenant_customer (id) ON DELETE CASCADE,
    CONSTRAINT fk_tca_allergy  FOREIGN KEY (allergy_id)  REFERENCES allergy (id)          ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tca_customer ON tenant_customer_allergy (customer_id);
CREATE INDEX IF NOT EXISTS idx_tca_allergy  ON tenant_customer_allergy (allergy_id);