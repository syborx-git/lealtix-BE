-- Migración V26: Actualizar restricción CHECK en client_order
-- Propósito: Agregar nuevos estados (EN_PREPARACION, LISTO) a la restricción CHECK
-- Fecha: 2026-03-28
-- Autor: Copilot

-- Eliminar restricción CHECK antigua
ALTER TABLE client_order 
DROP CONSTRAINT chk_client_order_estado;

-- Crear nueva restricción CHECK con los nuevos estados
ALTER TABLE client_order 
ADD CONSTRAINT chk_client_order_estado 
CHECK (estado IN ('PENDIENTE', 'CONFIRMADA','PAGADA', 'CANCELADA', 'EN_PREPARACION', 'LISTO'));

-- Comentario en la tabla para documentar el cambio
COMMENT ON CONSTRAINT chk_client_order_estado ON client_order 
IS 'Valida que el estado de la orden sea uno de los valores permitidos: PENDIENTE, CONFIRMADA, PAGADA, CANCELADA, EN_PREPARACION, LISTO';
