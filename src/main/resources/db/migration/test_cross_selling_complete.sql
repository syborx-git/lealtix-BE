-- ============================================================================
-- Script de Testing Completo para Cross-Selling
-- ============================================================================
-- Este script asume que ya existen productos en tenant_menu_product
-- Ajusta los IDs según tu base de datos

-- ============================================================================
-- PASO 1: VERIFICAR PRODUCTOS EXISTENTES
-- ============================================================================
SELECT 
    p.id,
    p.nombre,
    p.precio,
    c.nombre as categoria,
    c.tenant_id
FROM tenant_menu_product p
JOIN tenant_menu_category c ON p.category_id = c.id
WHERE c.tenant_id = 1  -- Cambia este ID por tu tenant
ORDER BY c.nombre, p.nombre;

-- ============================================================================
-- PASO 2: CREAR CONFIGURACIONES DE CROSS-SELLING
-- ============================================================================

-- Ejemplo 1: Hamburguesa → Sugerencias
-- Asume: Hamburguesa ID=1, Papas ID=2, Coca Cola ID=3, Ensalada ID=4

INSERT INTO product_cross_selling 
(product_id, suggested_product_id, tenant_id, display_order, is_active)
VALUES 
(1, 2, 1, 1, true),  -- Hamburguesa → Papas Fritas (prioridad 1)
(1, 3, 1, 2, true),  -- Hamburguesa → Coca Cola (prioridad 2)
(1, 4, 1, 3, true)   -- Hamburguesa → Ensalada (prioridad 3)
ON CONFLICT (product_id, suggested_product_id, tenant_id) DO NOTHING;

-- Ejemplo 2: Pizza → Sugerencias
-- Asume: Pizza ID=5, Alitas ID=6

INSERT INTO product_cross_selling 
(product_id, suggested_product_id, tenant_id, display_order, is_active)
VALUES 
(5, 3, 1, 1, true),  -- Pizza → Coca Cola
(5, 6, 1, 2, true)   -- Pizza → Alitas
ON CONFLICT (product_id, suggested_product_id, tenant_id) DO NOTHING;

-- Ejemplo 3: Café → Sugerencias
-- Asume: Café ID=7, Croissant ID=8, Muffin ID=9

INSERT INTO product_cross_selling 
(product_id, suggested_product_id, tenant_id, display_order, is_active)
VALUES 
(7, 8, 1, 1, true),  -- Café → Croissant
(7, 9, 1, 2, true)   -- Café → Muffin
ON CONFLICT (product_id, suggested_product_id, tenant_id) DO NOTHING;

-- Ejemplo 4: Ensalada → Sugerencias
-- Asume: Ensalada ID=4, Jugo ID=10

INSERT INTO product_cross_selling 
(product_id, suggested_product_id, tenant_id, display_order, is_active)
VALUES 
(4, 10, 1, 1, true)  -- Ensalada → Jugo Natural
ON CONFLICT (product_id, suggested_product_id, tenant_id) DO NOTHING;

-- ============================================================================
-- PASO 3: VERIFICAR CONFIGURACIONES CREADAS
-- ============================================================================

-- Vista general de todas las configuraciones
SELECT 
    pcs.id,
    p.nombre as producto_principal,
    sp.nombre as producto_sugerido,
    pcs.display_order,
    pcs.is_active,
    pcs.created_at
FROM product_cross_selling pcs
JOIN tenant_menu_product p ON pcs.product_id = p.id
JOIN tenant_menu_product sp ON pcs.suggested_product_id = sp.id
WHERE pcs.tenant_id = 1
ORDER BY p.nombre, pcs.display_order;

-- ============================================================================
-- PASO 4: SIMULAR CONSULTAS DEL FRONTEND
-- ============================================================================

-- Consulta 1: ¿Qué sugerir cuando se selecciona Hamburguesa?
SELECT 
    sp.id,
    sp.nombre,
    sp.precio,
    sp.img_url,
    sp.descripcion,
    c.nombre as categoria
FROM product_cross_selling pcs
JOIN tenant_menu_product sp ON pcs.suggested_product_id = sp.id
JOIN tenant_menu_category c ON sp.category_id = c.id
WHERE pcs.product_id = 1  -- Hamburguesa
  AND pcs.tenant_id = 1
  AND pcs.is_active = true
  AND sp.is_active = true
ORDER BY pcs.display_order;

-- Consulta 2: ¿Qué sugerir cuando se selecciona Pizza?
SELECT 
    sp.id,
    sp.nombre,
    sp.precio,
    sp.img_url
FROM product_cross_selling pcs
JOIN tenant_menu_product sp ON pcs.suggested_product_id = sp.id
WHERE pcs.product_id = 5  -- Pizza
  AND pcs.tenant_id = 1
  AND pcs.is_active = true
  AND sp.is_active = true
ORDER BY pcs.display_order;

-- ============================================================================
-- PASO 5: PROBAR ACTUALIZACIÓN
-- ============================================================================

-- Cambiar orden de prioridad: Coca Cola ahora es primera sugerencia
UPDATE product_cross_selling
SET display_order = 1, updated_at = CURRENT_TIMESTAMP
WHERE product_id = 1 
  AND suggested_product_id = 3
  AND tenant_id = 1;

-- Papas Fritas pasa a segunda
UPDATE product_cross_selling
SET display_order = 2, updated_at = CURRENT_TIMESTAMP
WHERE product_id = 1 
  AND suggested_product_id = 2
  AND tenant_id = 1;

-- Verificar cambio
SELECT 
    p.nombre as producto,
    sp.nombre as sugerencia,
    pcs.display_order
FROM product_cross_selling pcs
JOIN tenant_menu_product p ON pcs.product_id = p.id
JOIN tenant_menu_product sp ON pcs.suggested_product_id = sp.id
WHERE pcs.product_id = 1
  AND pcs.tenant_id = 1
ORDER BY pcs.display_order;

-- ============================================================================
-- PASO 6: PROBAR ACTIVAR/DESACTIVAR
-- ============================================================================

-- Desactivar Ensalada temporalmente (producto agotado)
UPDATE product_cross_selling
SET is_active = false, updated_at = CURRENT_TIMESTAMP
WHERE product_id = 1 
  AND suggested_product_id = 4
  AND tenant_id = 1;

-- Verificar que ya no aparece en sugerencias activas
SELECT 
    sp.nombre as sugerencia,
    pcs.is_active
FROM product_cross_selling pcs
JOIN tenant_menu_product sp ON pcs.suggested_product_id = sp.id
WHERE pcs.product_id = 1
  AND pcs.tenant_id = 1
  AND pcs.is_active = true  -- Solo activas
ORDER BY pcs.display_order;

-- Reactivar Ensalada
UPDATE product_cross_selling
SET is_active = true, updated_at = CURRENT_TIMESTAMP
WHERE product_id = 1 
  AND suggested_product_id = 4
  AND tenant_id = 1;

-- ============================================================================
-- PASO 7: PROBAR ELIMINACIÓN
-- ============================================================================

-- Eliminar una sugerencia (ejemplo: Ensalada ya no se sugiere con Hamburguesa)
DELETE FROM product_cross_selling
WHERE product_id = 1 
  AND suggested_product_id = 4
  AND tenant_id = 1;

-- Verificar eliminación
SELECT 
    p.nombre as producto,
    sp.nombre as sugerencia
FROM product_cross_selling pcs
JOIN tenant_menu_product p ON pcs.product_id = p.id
JOIN tenant_menu_product sp ON pcs.suggested_product_id = sp.id
WHERE pcs.product_id = 1
  AND pcs.tenant_id = 1;

-- ============================================================================
-- PASO 8: ESTADÍSTICAS Y REPORTES
-- ============================================================================

-- Contar configuraciones por producto
SELECT 
    p.nombre as producto,
    COUNT(pcs.id) as total_sugerencias,
    SUM(CASE WHEN pcs.is_active THEN 1 ELSE 0 END) as sugerencias_activas
FROM tenant_menu_product p
LEFT JOIN product_cross_selling pcs ON p.id = pcs.product_id AND pcs.tenant_id = 1
WHERE p.category_id IN (SELECT id FROM tenant_menu_category WHERE tenant_id = 1)
GROUP BY p.id, p.nombre
HAVING COUNT(pcs.id) > 0
ORDER BY total_sugerencias DESC;

-- Productos más sugeridos (populares como complementos)
SELECT 
    sp.nombre as producto_sugerido,
    COUNT(pcs.id) as veces_sugerido,
    AVG(pcs.display_order) as prioridad_promedio
FROM product_cross_selling pcs
JOIN tenant_menu_product sp ON pcs.suggested_product_id = sp.id
WHERE pcs.tenant_id = 1
  AND pcs.is_active = true
GROUP BY sp.id, sp.nombre
ORDER BY veces_sugerido DESC;

-- Productos sin configuración de cross-selling
SELECT 
    p.id,
    p.nombre,
    c.nombre as categoria
FROM tenant_menu_product p
JOIN tenant_menu_category c ON p.category_id = c.id
LEFT JOIN product_cross_selling pcs ON p.id = pcs.product_id AND pcs.tenant_id = 1
WHERE c.tenant_id = 1
  AND p.is_active = true
  AND pcs.id IS NULL
ORDER BY c.nombre, p.nombre;

-- ============================================================================
-- PASO 9: VALIDACIONES DE INTEGRIDAD
-- ============================================================================

-- Verificar que no hay auto-referencias (producto sugiriéndose a sí mismo)
SELECT 
    pcs.id,
    p.nombre
FROM product_cross_selling pcs
JOIN tenant_menu_product p ON pcs.product_id = p.id
WHERE pcs.product_id = pcs.suggested_product_id;
-- Debe retornar 0 filas

-- Verificar que no hay duplicados
SELECT 
    product_id,
    suggested_product_id,
    tenant_id,
    COUNT(*) as duplicados
FROM product_cross_selling
GROUP BY product_id, suggested_product_id, tenant_id
HAVING COUNT(*) > 1;
-- Debe retornar 0 filas

-- Verificar que todos los productos pertenecen al mismo tenant
SELECT 
    pcs.id,
    pcs.tenant_id as config_tenant,
    pc.tenant_id as product_tenant,
    sc.tenant_id as suggested_tenant
FROM product_cross_selling pcs
JOIN tenant_menu_product p ON pcs.product_id = p.id
JOIN tenant_menu_category pc ON p.category_id = pc.id
JOIN tenant_menu_product sp ON pcs.suggested_product_id = sp.id
JOIN tenant_menu_category sc ON sp.category_id = sc.id
WHERE pcs.tenant_id != pc.tenant_id 
   OR pcs.tenant_id != sc.tenant_id;
-- Debe retornar 0 filas

-- ============================================================================
-- PASO 10: LIMPIAR DATOS DE PRUEBA (OPCIONAL)
-- ============================================================================

-- ⚠️ CUIDADO: Esto eliminará TODAS las configuraciones del tenant 1
-- Descomenta solo si quieres limpiar:

-- DELETE FROM product_cross_selling WHERE tenant_id = 1;

-- ============================================================================
-- QUERIES ÚTILES PARA EL DÍA A DÍA
-- ============================================================================

-- Ver configuración de un producto específico
SELECT 
    pcs.id,
    p.nombre as producto,
    sp.nombre as sugerencia,
    sp.precio,
    pcs.display_order,
    CASE WHEN pcs.is_active THEN '✅ Activo' ELSE '❌ Inactivo' END as estado
FROM product_cross_selling pcs
JOIN tenant_menu_product p ON pcs.product_id = p.id
JOIN tenant_menu_product sp ON pcs.suggested_product_id = sp.id
WHERE pcs.product_id = 1  -- Cambia el ID
  AND pcs.tenant_id = 1
ORDER BY pcs.display_order;

-- Buscar configuración por nombre de producto
SELECT 
    pcs.id,
    p.nombre as producto,
    sp.nombre as sugerencia,
    pcs.display_order,
    pcs.is_active
FROM product_cross_selling pcs
JOIN tenant_menu_product p ON pcs.product_id = p.id
JOIN tenant_menu_product sp ON pcs.suggested_product_id = sp.id
WHERE LOWER(p.nombre) LIKE LOWER('%hamburguesa%')  -- Buscar por nombre
  AND pcs.tenant_id = 1;

-- Activar todas las configuraciones de un producto
UPDATE product_cross_selling
SET is_active = true, updated_at = CURRENT_TIMESTAMP
WHERE product_id = 1
  AND tenant_id = 1;

-- Desactivar todas las configuraciones de un producto
UPDATE product_cross_selling
SET is_active = false, updated_at = CURRENT_TIMESTAMP
WHERE product_id = 1
  AND tenant_id = 1;

-- ============================================================================
-- FIN DEL SCRIPT
-- ============================================================================

-- Para usar este script:
-- 1. Ajusta los IDs de productos según tu base de datos
-- 2. Cambia el tenant_id si es necesario
-- 3. Ejecuta paso por paso, verificando resultados
-- 4. Usa las queries útiles para gestión diaria

COMMIT;
