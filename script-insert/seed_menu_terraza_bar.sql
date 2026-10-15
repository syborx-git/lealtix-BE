-- ============================================================================
-- Script: seed_menu_terraza_bar.sql
-- Descripción: Limpia categorías y productos obsoletos de bebidas para Tenant 1
--              e inicializa el catálogo completo de "TERRAZA BAR / mixology bar"
--              con soporte para variantes en cards tipo subcomanda.
-- ============================================================================

DO $$
DECLARE
    v_tenant_id BIGINT := 1;
    v_cat_snacks BIGINT;
    v_cat_cerveza BIGINT;
    v_cat_cocteleria BIGINT;
    v_cat_bebidas BIGINT;
    v_cat_botellas BIGINT;

    -- IDs de productos
    v_prod_id BIGINT;
    v_insumo_id BIGINT;
BEGIN
    -- 1. LIMPIEZA DE PRODUCTOS Y CATEGORÍAS DE BEBIDAS PREVIAS (38, 39, 40, 41)
    -- Limpieza en orden de llaves foráneas:
    DELETE FROM comanda_asiento_item 
    WHERE item_id IN (
        SELECT id FROM client_order_item 
        WHERE product_id IN (SELECT id FROM tenant_menu_product WHERE category_id IN (38, 39, 40, 41))
    );

    DELETE FROM client_order_item 
    WHERE product_id IN (SELECT id FROM tenant_menu_product WHERE category_id IN (38, 39, 40, 41));

    DELETE FROM product_cross_selling 
    WHERE product_id IN (SELECT id FROM tenant_menu_product WHERE category_id IN (38, 39, 40, 41))
       OR suggested_product_id IN (SELECT id FROM tenant_menu_product WHERE category_id IN (38, 39, 40, 41));

    DELETE FROM product_additional 
    WHERE dish_product_id IN (SELECT id FROM tenant_menu_product WHERE category_id IN (38, 39, 40, 41));

    DELETE FROM product_recipe 
    WHERE dish_product_id IN (SELECT id FROM tenant_menu_product WHERE category_id IN (38, 39, 40, 41));

    DELETE FROM product_sub_receta 
    WHERE dish_product_id IN (SELECT id FROM tenant_menu_product WHERE category_id IN (38, 39, 40, 41))
       OR sub_receta_id IN (SELECT id FROM tenant_menu_product WHERE category_id IN (38, 39, 40, 41));

    DELETE FROM tenant_menu_product_category 
    WHERE product_id IN (SELECT id FROM tenant_menu_product WHERE category_id IN (38, 39, 40, 41))
       OR category_id IN (38, 39, 40, 41);

    DELETE FROM insumo_category WHERE category_id IN (38, 39, 40, 41);

    UPDATE insumo SET producto_id = NULL 
    WHERE producto_id IN (SELECT id FROM tenant_menu_product WHERE category_id IN (38, 39, 40, 41));

    DELETE FROM tenant_menu_product WHERE category_id IN (38, 39, 40, 41);
    DELETE FROM tenant_menu_category WHERE id IN (38, 39, 40, 41);

    -- Limpiar si ya existían de una corrida previa categorías con estos nombres
    DELETE FROM tenant_menu_product WHERE category_id IN (
        SELECT id FROM tenant_menu_category 
        WHERE tenant_id = v_tenant_id AND nombre IN ('SNACKS', 'CERVEZA', 'COCTELERÍA CLÁSICA', 'BEBIDAS', 'DESTILADOS & BOTELLAS')
    );
    DELETE FROM tenant_menu_category 
    WHERE tenant_id = v_tenant_id AND nombre IN ('SNACKS', 'CERVEZA', 'COCTELERÍA CLÁSICA', 'BEBIDAS', 'DESTILADOS & BOTELLAS');

    -- 2. CREAR NUEVAS CATEGORÍAS
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, is_active, display_order, created_at, updated_at)
    VALUES (v_tenant_id, 'SNACKS', 'Snacks y botanas de la terraza', true, 20, NOW(), NOW())
    RETURNING id INTO v_cat_snacks;

    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, is_active, display_order, created_at, updated_at)
    VALUES (v_tenant_id, 'CERVEZA', 'Cervezas nacionales e importadas', true, 21, NOW(), NOW())
    RETURNING id INTO v_cat_cerveza;

    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, is_active, display_order, created_at, updated_at)
    VALUES (v_tenant_id, 'COCTELERÍA CLÁSICA', 'Mixología y coctelería clásica de autor', true, 22, NOW(), NOW())
    RETURNING id INTO v_cat_cocteleria;

    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, is_active, display_order, created_at, updated_at)
    VALUES (v_tenant_id, 'BEBIDAS', 'Refrescos, aguas y aguas minerales', true, 23, NOW(), NOW())
    RETURNING id INTO v_cat_bebidas;

    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, is_active, display_order, created_at, updated_at)
    VALUES (v_tenant_id, 'DESTILADOS & BOTELLAS', 'Servicio de botellas por categoría con variantes y mezcladores', true, 24, NOW(), NOW())
    RETURNING id INTO v_cat_botellas;

    -- 3. INSERTAR SNACKS
    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_snacks, 'Nachos pork', 'Totopos con queso manchego gratinado y bañados de una salsa amarillito, aguacate, cebolla encurtida, cilantro y cubos de pork belly', 295.00, true, 50, 5, 'orden', false, NOW(), NOW());

    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_snacks, 'Trío de mini hamburguesas', '150gr de sirloin marinado, acompañado de gouda, lechuga, jitomate, cebollas y papas gajo', 230.00, true, 50, 5, 'orden', false, NOW(), NOW());

    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_snacks, 'Papas gajo', '200 gr de papa gajo fritas', 110.00, true, 80, 10, 'orden', false, NOW(), NOW());

    -- 4. INSERTAR CERVEZA
    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_cerveza, 'Amstel ultra 355ml', 'Cerveza Amstel Ultra 355ml bien fría', 79.00, true, 100, 12, 'pieza', false, NOW(), NOW());

    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_cerveza, 'Bohemia cristal 355ml', 'Cerveza Bohemia Cristal 355ml', 79.00, true, 100, 12, 'pieza', false, NOW(), NOW());

    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_cerveza, 'Heineken 355ml', 'Cerveza Heineken 355ml', 79.00, true, 100, 12, 'pieza', false, NOW(), NOW());

    -- 5. INSERTAR COCTELERÍA CLÁSICA
    -- Mojito de sabores (con variantes de sabor: Limón, Mango, Frutos Rojos)
    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_cocteleria, 'Mojito de sabores (230 ml)', 'Bacardí, hierbabuena y soda. Sabores a elegir: limón, mango, frutos rojos', 145.00, true, 80, 10, 'pieza', false, NOW(), NOW())
    RETURNING id INTO v_prod_id;

    -- Insumos para variantes de sabor de Mojito
    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Sabor Limón Clásico', 'pieza', 100, 5, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 145.00, 'Sabor', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Sabor Mango', 'pieza', 100, 5, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 145.00, 'Sabor', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Sabor Frutos Rojos', 'pieza', 100, 5, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 145.00, 'Sabor', true);

    -- Otros cócteles
    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_cocteleria, 'Piña colada (220ml)', 'Bacardí, crema de coco, leche evaporada y jugo de piña', 140.00, true, 80, 10, 'pieza', false, NOW(), NOW());

    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_cocteleria, 'Azulito (220 ml)', 'Vodka, blue curaçao, jugo de piña, soda de limón', 160.00, true, 80, 10, 'pieza', false, NOW(), NOW());

    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_cocteleria, 'Shot ruso negro (60 ml)', 'Licor de café, vodka', 125.00, true, 100, 10, 'pieza', false, NOW(), NOW());

    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_cocteleria, 'Shot alma negra (60 ml)', 'Granadina, vodka, curaçao', 120.00, true, 100, 10, 'pieza', false, NOW(), NOW());

    -- 6. INSERTAR BEBIDAS
    -- Refrescos 355ml (con variantes: Coca cola, Coca light, Coca 0, Sprite, Manzana)
    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_bebidas, 'Refrescos 355ml', 'Coca Cola, Coca Light, Coca 0, Sprite, Manzana', 40.00, true, 150, 15, 'pieza', false, NOW(), NOW())
    RETURNING id INTO v_prod_id;

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Coca Cola 355ml', 'pieza', 80, 10, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 40.00, 'Refresco', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Coca Light 355ml', 'pieza', 50, 10, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 40.00, 'Refresco', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Coca 0 (Zero) 355ml', 'pieza', 50, 10, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 40.00, 'Refresco', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Sprite 355ml', 'pieza', 50, 10, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 40.00, 'Refresco', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Manzana Lift 355ml', 'pieza', 50, 10, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 40.00, 'Refresco', true);

    -- Aguas
    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_bebidas, 'Agua embotellada 355ml', 'Agua natural purificada 355ml', 35.00, true, 100, 10, 'pieza', false, NOW(), NOW());

    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_bebidas, 'Agua mineral ciel 355ml', 'Agua mineral Ciel 355ml', 40.00, true, 100, 10, 'pieza', false, NOW(), NOW());

    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_bebidas, 'St. pellegrino 250ml', 'Agua mineral italiana San Pellegrino 250ml', 50.00, true, 50, 5, 'pieza', false, NOW(), NOW());

    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_bebidas, 'Perrier 330ml', 'Agua mineral francesa Perrier 330ml', 60.00, true, 50, 5, 'pieza', false, NOW(), NOW());

    -- 7. INSUMOS COMUNES DE MEZCLADORES PARA SERVICIO DE BOTELLAS
    -- (*el servicio por botella incluye 5 mezcladores de 355ml)
    -- =========================================================================

    -- ==================== TEQUILA ====================
    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_botellas, 'Tequila', 'Servicio de botella incluye 5 mezcladores de 355ml. Selecciona tu presentación en la subcomanda.', 1500.00, true, 50, 5, 'botella', false, NOW(), NOW())
    RETURNING id INTO v_prod_id;

    -- Variantes de Tequila
    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Tradicional plata 695ml', 'botella', 20, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 1500.00, 'Tequila', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Tradicional reposado 695ml', 'botella', 20, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 1500.00, 'Tequila', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, '1800 cristalino 700ml', 'botella', 15, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 2400.00, 'Tequila', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Maestro dob diamante 700ml', 'botella', 15, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 2600.00, 'Tequila', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Don julio 70 700ml', 'botella', 15, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 3300.00, 'Tequila', true);

    -- Mezcladores incluidos en Tequila
    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, '5 Mezcladores: Coca-Cola y Agua Mineral', 'servicio', 100, 5, true, false, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 0.00, 'Mezcladores Incluidos', false);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, '5 Mezcladores: Squirt Toronja', 'servicio', 100, 5, true, false, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 0.00, 'Mezcladores Incluidos', false);


    -- ==================== MEZCAL ====================
    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_botellas, 'Mezcal', 'Servicio de botella incluye 5 mezcladores de 355ml. Selecciona tu presentación en la subcomanda.', 1800.00, true, 50, 5, 'botella', false, NOW(), NOW())
    RETURNING id INTO v_prod_id;

    -- Variantes de Mezcal
    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, '400 conejos espadín 750ml', 'botella', 20, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 1800.00, 'Mezcal', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Montelobos espadín 750ml', 'botella', 15, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 2600.00, 'Mezcal', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Ojo de tigre joven 750ml', 'botella', 15, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 2650.00, 'Mezcal', true);

    -- Mezcladores incluidos en Mezcal
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 0.00, 'Mezcladores Incluidos', false);


    -- ==================== VODKA ====================
    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_botellas, 'Vodka', 'Servicio de botella incluye 5 mezcladores de 355ml. Selecciona tu presentación en la subcomanda.', 1350.00, true, 50, 5, 'botella', false, NOW(), NOW())
    RETURNING id INTO v_prod_id;

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Smirnoff 21 750ml', 'botella', 20, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 1350.00, 'Vodka', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Smirnoff tamarindo 750ml', 'botella', 20, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 1450.00, 'Vodka', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Absolut azul 750ml', 'botella', 20, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 1600.00, 'Vodka', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Absolut pears 750ml', 'botella', 15, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 1600.00, 'Vodka', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Absolut raspberry 750ml', 'botella', 15, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 1600.00, 'Vodka', true);


    -- ==================== WHISKY ====================
    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_botellas, 'Whisky', 'Servicio de botella incluye 5 mezcladores de 355ml. Selecciona tu presentación en la subcomanda.', 1850.00, true, 50, 5, 'botella', false, NOW(), NOW())
    RETURNING id INTO v_prod_id;

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'JW red label 700ml', 'botella', 20, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 1850.00, 'Whisky', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Jack daniels 700ml', 'botella', 20, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 1850.00, 'Whisky', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Buchanans 12 750ml', 'botella', 20, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 2950.00, 'Whisky', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Jack daniels honey 750ml', 'botella', 15, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 1850.00, 'Whisky', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'JW black label 700ml', 'botella', 15, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 3200.00, 'Whisky', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Macallan 12 750ml', 'botella', 10, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 4500.00, 'Whisky', true);


    -- ==================== RON ====================
    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_botellas, 'Ron', 'Servicio de botella incluye 5 mezcladores de 355ml. Selecciona tu presentación en la subcomanda.', 1400.00, true, 50, 5, 'botella', false, NOW(), NOW())
    RETURNING id INTO v_prod_id;

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Bacardi blanco 750ml', 'botella', 20, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 1400.00, 'Ron', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Bacardi raspberry 750ml', 'botella', 15, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 1400.00, 'Ron', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Bacardi spicy mango 750ml', 'botella', 15, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 1400.00, 'Ron', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Matusalem clásico 750ml', 'botella', 15, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 1400.00, 'Ron', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Zacapa 23 750ml', 'botella', 10, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 3400.00, 'Ron', true);


    -- ==================== GINEBRA ====================
    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_botellas, 'Ginebra', 'Servicio de botella incluye 5 mezcladores de 355ml. Selecciona tu presentación en la subcomanda.', 2100.00, true, 30, 3, 'botella', false, NOW(), NOW())
    RETURNING id INTO v_prod_id;

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Bombai 750ml', 'botella', 15, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 2100.00, 'Ginebra', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Hendricks 750ml', 'botella', 15, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 2750.00, 'Ginebra', true);


    -- ==================== BRANDY ====================
    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_botellas, 'Brandy', 'Servicio de botella incluye 5 mezcladores de 355ml. Selecciona tu presentación en la subcomanda.', 1600.00, true, 30, 3, 'botella', false, NOW(), NOW())
    RETURNING id INTO v_prod_id;

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Torres 10 700ml', 'botella', 20, 2, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 1600.00, 'Brandy', true);


    -- ==================== ESPECIALES ====================
    INSERT INTO tenant_menu_product (category_id, nombre, descripcion, precio, is_active, stock, stock_minimo, unidad, auto_availability, created_at, updated_at)
    VALUES (v_cat_botellas, 'Especiales', 'Ediciones especiales, botellas de gran formato y barra de shots. Selecciona en la subcomanda.', 380.00, true, 30, 3, 'pieza', false, NOW(), NOW())
    RETURNING id INTO v_prod_id;

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Maestro dobel diamante 1750ml', 'botella', 10, 1, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 4800.00, 'Especiales', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Moët chandon brut imperial 750ml', 'botella', 10, 1, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 3900.00, 'Especiales', true);

    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, is_active, es_bebida, created_at, updated_at)
    VALUES (v_tenant_id, 'Barra shots (8pza) 60ml c/u', 'ronda', 50, 5, true, true, NOW(), NOW()) RETURNING id INTO v_insumo_id;
    INSERT INTO product_additional (dish_product_id, insumo_id, cantidad, precio, grupo, is_variant)
    VALUES (v_prod_id, v_insumo_id, 1, 380.00, 'Especiales', true);

END $$;
