-- =====================================================
-- V51: Plano interactivo de mesas (floor plan)
-- Fecha: 2026-09-21
-- Descripción: Soporta vista 2D con drag-and-drop y
--   unión de mesas en grupos temporales.
--
--   posicion_x / posicion_y  : coordenadas (px) del centro
--                              de la mesa sobre el plano.
--   forma                    : redonda | cuadrada | rectangular
--   id_grupo_temporal        : UUID que agrupa mesas unidas;
--                              NULL cuando la mesa opera sola.
-- =====================================================

ALTER TABLE mesa ADD COLUMN IF NOT EXISTS posicion_x DOUBLE PRECISION;
ALTER TABLE mesa ADD COLUMN IF NOT EXISTS posicion_y DOUBLE PRECISION;
ALTER TABLE mesa ADD COLUMN IF NOT EXISTS forma VARCHAR(20) NOT NULL DEFAULT 'cuadrada';
ALTER TABLE mesa ADD COLUMN IF NOT EXISTS id_grupo_temporal VARCHAR(36);

CREATE INDEX IF NOT EXISTS idx_mesa_grupo_temporal ON mesa(tenant_id, id_grupo_temporal);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_mesa_forma') THEN
        ALTER TABLE mesa ADD CONSTRAINT chk_mesa_forma CHECK (forma IN ('redonda', 'cuadrada', 'rectangular'));
    END IF;
END $$;