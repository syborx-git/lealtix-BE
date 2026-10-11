-- =====================================================
-- V64: Soporte de dimensiones personalizadas (ancho/alto)
--      e identificador único para mesas del local
-- =====================================================

ALTER TABLE mesa ADD COLUMN IF NOT EXISTS ancho DOUBLE PRECISION DEFAULT 80.0;
ALTER TABLE mesa ADD COLUMN IF NOT EXISTS alto DOUBLE PRECISION DEFAULT 80.0;
ALTER TABLE mesa ADD COLUMN IF NOT EXISTS identificador VARCHAR(50);

-- Inicializar dimensiones de mesas existentes según su forma
UPDATE mesa 
SET ancho = 80.0, alto = 80.0 
WHERE forma = 'redonda' AND (ancho IS NULL OR ancho <= 0);

UPDATE mesa 
SET ancho = 110.0, alto = 70.0, forma = 'cuadrada' 
WHERE forma = 'rectangular';

UPDATE mesa 
SET ancho = 80.0, alto = 80.0 
WHERE (forma = 'cuadrada' OR forma IS NULL) AND (ancho IS NULL OR ancho <= 0);

-- Si no tienen identificador, asignar número o ID
UPDATE mesa 
SET identificador = COALESCE(numero::text, id::text) 
WHERE identificador IS NULL OR TRIM(identificador) = '';

-- Crear índice para búsquedas por identificador dentro del tenant
CREATE INDEX IF NOT EXISTS idx_mesa_tenant_identificador ON mesa (tenant_id, identificador);
