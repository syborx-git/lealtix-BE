-- =====================================================
-- V38: Agregar campos de bebida a la tabla insumo
-- Fecha: 2026-09-03
-- Descripción: La bebida es un tipo de insumo. Se agrega el
--              flag es_bebida y el precio de venta (precio_venta)
--              para que pueda venderse en el POS Comandix.
-- =====================================================

ALTER TABLE insumo
    ADD COLUMN IF NOT EXISTS es_bebida BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE insumo
    ADD COLUMN IF NOT EXISTS precio_venta NUMERIC(10, 2);

-- Id del producto de menú (tenant_menu_product) enlazado, solo para bebidas vendibles en Comandix.
ALTER TABLE insumo
    ADD COLUMN IF NOT EXISTS producto_id BIGINT;
