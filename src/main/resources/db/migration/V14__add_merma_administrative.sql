-- =====================================================
-- V14: Ampliar tabla merma para soportar mermas administrativas
-- Fecha: 2026-09-16
-- Descripcion: Mermas administrativas = salidas directas de
--              almacen (bodega/cocina/barra) con motivo y usuario.
--              categoria_merma distingue 'COMANDADA' (por comanda,
--              no descuenta inventario) de 'ADMINISTRATIVA' (descuenta).
-- =====================================================

ALTER TABLE merma ADD COLUMN IF NOT EXISTS categoria_merma VARCHAR(20) NOT NULL DEFAULT 'COMANDADA';
ALTER TABLE merma ADD COLUMN IF NOT EXISTS origen VARCHAR(20);
ALTER TABLE merma ADD COLUMN IF NOT EXISTS motivo VARCHAR(255);
ALTER TABLE merma ADD COLUMN IF NOT EXISTS usuario_id BIGINT;
ALTER TABLE merma ADD COLUMN IF NOT EXISTS usuario_nombre VARCHAR(120);

-- Una merma administrativa no requiere ticket de comanda
ALTER TABLE merma ALTER COLUMN ticket DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_merma_categoria ON merma(categoria_merma);
CREATE INDEX IF NOT EXISTS idx_merma_tenant_fecha ON merma(tenant_id, fecha);