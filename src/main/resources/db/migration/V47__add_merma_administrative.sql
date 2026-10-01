-- =====================================================
-- V47: Mermas Administrativas (salidas directas de almacén)
-- Agrega trazabilidad (usuario), motivo, origen y categoría
-- de registro: COMANDADA (por comanda) | ADMINISTRATIVA (directa)
-- =====================================================

-- Categoría del registro de merma: COMANDADA (por comanda) o ADMINISTRATIVA (almacén)
ALTER TABLE merma ADD COLUMN IF NOT EXISTS categoria_merma VARCHAR(20) DEFAULT 'COMANDADA';

-- Almacén del que se descuenta el stock en mermas administrativas: BODEGA | COCINA | BARRA
ALTER TABLE merma ADD COLUMN IF NOT EXISTS origen VARCHAR(20);

-- Motivo de la merma (texto libre, ej. caducidad, accidente)
ALTER TABLE merma ADD COLUMN IF NOT EXISTS motivo VARCHAR(255);

-- Usuario que registró la merma (trazabilidad)
ALTER TABLE merma ADD COLUMN IF NOT EXISTS usuario_id BIGINT;
ALTER TABLE merma ADD COLUMN IF NOT EXISTS usuario_nombre VARCHAR(120);

-- Las mermas administrativas no pertenecen a una comanda/ticket
ALTER TABLE merma ALTER COLUMN ticket DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_merma_categoria ON merma(tenant_id, categoria_merma, fecha);