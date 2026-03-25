-- Script de ejemplo para insertar datos de cross-selling
-- Este script es para propósitos de demostración y testing

-- Ejemplo 1: Sugerencias para una Hamburguesa (asumiendo product_id = 1, tenant_id = 1)
-- Sugerencia 1: Papas Fritas (product_id = 2)
INSERT INTO product_cross_selling (product_id, suggested_product_id, tenant_id, display_order, is_active)
VALUES (1, 2, 1, 1, true)
ON CONFLICT (product_id, suggested_product_id, tenant_id) DO NOTHING;

-- Sugerencia 2: Coca Cola (product_id = 3)
INSERT INTO product_cross_selling (product_id, suggested_product_id, tenant_id, display_order, is_active)
VALUES (1, 3, 1, 2, true)
ON CONFLICT (product_id, suggested_product_id, tenant_id) DO NOTHING;

-- Sugerencia 3: Ensalada (product_id = 4)
INSERT INTO product_cross_selling (product_id, suggested_product_id, tenant_id, display_order, is_active)
VALUES (1, 4, 1, 3, true)
ON CONFLICT (product_id, suggested_product_id, tenant_id) DO NOTHING;

-- Ejemplo 2: Sugerencias para Pizza (asumiendo product_id = 5)
-- Sugerencia 1: Coca Cola
INSERT INTO product_cross_selling (product_id, suggested_product_id, tenant_id, display_order, is_active)
VALUES (5, 3, 1, 1, true)
ON CONFLICT (product_id, suggested_product_id, tenant_id) DO NOTHING;

-- Sugerencia 2: Alitas (product_id = 6)
INSERT INTO product_cross_selling (product_id, suggested_product_id, tenant_id, display_order, is_active)
VALUES (5, 6, 1, 2, true)
ON CONFLICT (product_id, suggested_product_id, tenant_id) DO NOTHING;

-- Ejemplo 3: Sugerencias para Café (asumiendo product_id = 7)
-- Sugerencia 1: Croissant (product_id = 8)
INSERT INTO product_cross_selling (product_id, suggested_product_id, tenant_id, display_order, is_active)
VALUES (7, 8, 1, 1, true)
ON CONFLICT (product_id, suggested_product_id, tenant_id) DO NOTHING;

-- Sugerencia 2: Muffin (product_id = 9)
INSERT INTO product_cross_selling (product_id, suggested_product_id, tenant_id, display_order, is_active)
VALUES (7, 9, 1, 2, true)
ON CONFLICT (product_id, suggested_product_id, tenant_id) DO NOTHING;

-- Verificar las inserciones
SELECT 
    pcs.id,
    p.nombre as producto_principal,
    sp.nombre as producto_sugerido,
    pcs.display_order,
    pcs.is_active
FROM product_cross_selling pcs
JOIN tenant_menu_product p ON pcs.product_id = p.id
JOIN tenant_menu_product sp ON pcs.suggested_product_id = sp.id
WHERE pcs.tenant_id = 1
ORDER BY pcs.product_id, pcs.display_order;
