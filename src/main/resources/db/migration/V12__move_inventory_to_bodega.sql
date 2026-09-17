-- =====================================================================
-- V12: Pasar el inventario distribuido actual a la Bodega.
-- Todo el stock (stock) se mueve a stock_bodega y cocina/barra/stock
-- quedan en 0. La distribución a cocina/barra se hará desde Bodega.
-- Requiere que V11 ya haya creado las columnas de ubicación.
-- =====================================================================

UPDATE insumo
SET stock_bodega = COALESCE(stock_bodega, 0) + COALESCE(stock, 0),
    stock        = 0,
    stock_cocina = 0,
    stock_barra  = 0;