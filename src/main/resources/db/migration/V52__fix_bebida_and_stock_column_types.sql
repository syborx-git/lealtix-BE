-- =====================================================
-- V52: Ajustar tipos de columna de inventario/bebidas
-- Corrige discrepancia entre NUMERIC(14,3) y Double en JPA
-- para validacion estricta de Hibernate 6
-- =====================================================

ALTER TABLE bebida 
    ALTER COLUMN stock_minimo TYPE DOUBLE PRECISION USING stock_minimo::DOUBLE PRECISION;

ALTER TABLE bebida_receta 
    ALTER COLUMN cantidad TYPE DOUBLE PRECISION USING cantidad::DOUBLE PRECISION;

ALTER TABLE stock_request 
    ALTER COLUMN cantidad TYPE DOUBLE PRECISION USING cantidad::DOUBLE PRECISION;

ALTER TABLE stock_transfer_history 
    ALTER COLUMN cantidad TYPE DOUBLE PRECISION USING cantidad::DOUBLE PRECISION;
