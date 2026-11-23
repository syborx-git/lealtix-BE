-- ============================================================================
-- V82: Actualización Oficial del Menú de Restaurante Petra
-- Sustituye el menú anterior del servidor admin@restaurante-petra.com por el
-- nuevo menú oficial con 17 categorías, 110 platillos y bebidas, recetas base
-- e insumos con stock masivo (999,999) para pruebas operativas continuas.
-- ============================================================================

DO $$
DECLARE
    v_tenant_id BIGINT;
    v_cat_id BIGINT;
    v_prod_id BIGINT;
    v_insumo_id BIGINT;
BEGIN
    -- 1. IDENTIFICACIÓN DINÁMICA DEL TENANT DE PETRA
    SELECT tenant_id INTO v_tenant_id FROM tenant_user WHERE LOWER(email) = 'admin@restaurante-petra.com' LIMIT 1;
    IF v_tenant_id IS NULL THEN
        SELECT t.id INTO v_tenant_id FROM tenant t JOIN app_user u ON t.user_id = u.id WHERE LOWER(u.email) = 'admin@restaurante-petra.com' LIMIT 1;
    END IF;
    IF v_tenant_id IS NULL THEN
        SELECT id INTO v_tenant_id FROM tenant WHERE LOWER(slug) IN ('restaurante-petra', 'demo') OR LOWER(nombre_negocio) LIKE '%petra%' ORDER BY id ASC LIMIT 1;
    END IF;
    IF v_tenant_id IS NULL THEN
        SELECT id INTO v_tenant_id FROM tenant ORDER BY id ASC LIMIT 1;
    END IF;

    IF v_tenant_id IS NULL THEN
        RAISE NOTICE 'No se encontró ningún tenant para Petra. Abortando migración V82.';
        RETURN;
    END IF;

    RAISE NOTICE 'Actualizando Menú Oficial de Petra para el Tenant ID: %', v_tenant_id;

    -- 2. ASEGURAR USUARIO ADMIN PETRA
    INSERT INTO tenant_user (tenant_id, nombre, email, password_hash, rol, activo, sueldo_mensual, created_at, updated_at)
    VALUES (v_tenant_id, 'Admin Petra', 'admin@restaurante-petra.com', 'YWRtaW4xMjM=', 'ADMIN', TRUE, 15000.00, NOW(), NOW())
    ON CONFLICT (tenant_id, email) DO UPDATE SET password_hash = 'YWRtaW4xMjM=', activo = TRUE, rol = 'ADMIN';

    -- 3. LIMPIEZA TOTAL DEL MENÚ ANTERIOR DEL TENANT (Sin violar FKs de órdenes)
    DELETE FROM comanda_asiento_item WHERE item_id IN (
        SELECT id FROM client_order_item WHERE product_id IN (
            SELECT p.id FROM tenant_menu_product p
            JOIN tenant_menu_category c ON p.category_id = c.id
            WHERE c.tenant_id = v_tenant_id
        )
    );

    DELETE FROM client_order_item WHERE product_id IN (
        SELECT p.id FROM tenant_menu_product p
        JOIN tenant_menu_category c ON p.category_id = c.id
        WHERE c.tenant_id = v_tenant_id
    );

    DELETE FROM product_cross_selling WHERE tenant_id = v_tenant_id;

    DELETE FROM product_sub_receta WHERE dish_product_id IN (
        SELECT p.id FROM tenant_menu_product p
        JOIN tenant_menu_category c ON p.category_id = c.id
        WHERE c.tenant_id = v_tenant_id
    ) OR sub_receta_id IN (
        SELECT p.id FROM tenant_menu_product p
        JOIN tenant_menu_category c ON p.category_id = c.id
        WHERE c.tenant_id = v_tenant_id
    );

    DELETE FROM product_recipe WHERE dish_product_id IN (
        SELECT p.id FROM tenant_menu_product p
        JOIN tenant_menu_category c ON p.category_id = c.id
        WHERE c.tenant_id = v_tenant_id
    );

    DELETE FROM product_additional WHERE dish_product_id IN (
        SELECT p.id FROM tenant_menu_product p
        JOIN tenant_menu_category c ON p.category_id = c.id
        WHERE c.tenant_id = v_tenant_id
    );

    DELETE FROM product_variant_dish WHERE dish_product_id IN (
        SELECT p.id FROM tenant_menu_product p
        JOIN tenant_menu_category c ON p.category_id = c.id
        WHERE c.tenant_id = v_tenant_id
    ) OR variant_dish_id IN (
        SELECT p.id FROM tenant_menu_product p
        JOIN tenant_menu_category c ON p.category_id = c.id
        WHERE c.tenant_id = v_tenant_id
    );

    UPDATE tenant_loyalty_card_config SET reward_product_id = NULL WHERE tenant_id = v_tenant_id;

    DELETE FROM tenant_menu_product_category WHERE category_id IN (
        SELECT id FROM tenant_menu_category WHERE tenant_id = v_tenant_id
    );

    DELETE FROM tenant_menu_product WHERE category_id IN (
        SELECT id FROM tenant_menu_category WHERE tenant_id = v_tenant_id
    );

    DELETE FROM insumo_category WHERE category_id IN (
        SELECT id FROM tenant_menu_category WHERE tenant_id = v_tenant_id
    );

    DELETE FROM insumo WHERE tenant_id = v_tenant_id;

    DELETE FROM tenant_menu_category WHERE tenant_id = v_tenant_id;

    -- 4. INSERTAR INSUMOS BASE CON STOCK MASIVO (999,999)
    INSERT INTO insumo (tenant_id, nombre, unidad, stock, stock_minimo, stock_bodega, stock_cocina, stock_barra, es_bebida, precio_venta, is_active, created_at, updated_at)
    VALUES
        (v_tenant_id, 'Insumo Base Alimento Petra', 'pieza', 999999.0, 10.0, 499999.0, 500000.0, 0.0, false, NULL, TRUE, NOW(), NOW()),
        (v_tenant_id, 'Proteína Cocina Petra', 'gramos', 999999.0, 10.0, 499999.0, 500000.0, 0.0, false, NULL, TRUE, NOW(), NOW()),
        (v_tenant_id, 'Guarnición y Vegetales Petra', 'gramos', 999999.0, 10.0, 499999.0, 500000.0, 0.0, false, NULL, TRUE, NOW(), NOW()),
        (v_tenant_id, 'Lácteos y Quesos Cocina Petra', 'gramos', 999999.0, 10.0, 499999.0, 500000.0, 0.0, false, NULL, TRUE, NOW(), NOW()),
        (v_tenant_id, 'Panadería y Masas Petra', 'pieza', 999999.0, 10.0, 499999.0, 500000.0, 0.0, false, NULL, TRUE, NOW(), NOW()),
        (v_tenant_id, 'Café de Grano Petra', 'gramos', 999999.0, 10.0, 499999.0, 0.0, 500000.0, true, NULL, TRUE, NOW(), NOW()),
        (v_tenant_id, 'Leche y Bases Líquidas Petra', 'mililitros', 999999.0, 10.0, 499999.0, 0.0, 500000.0, true, NULL, TRUE, NOW(), NOW()),
        (v_tenant_id, 'Jarabes y Sabores Barra Petra', 'mililitros', 999999.0, 10.0, 499999.0, 0.0, 500000.0, true, NULL, TRUE, NOW(), NOW()),
        (v_tenant_id, 'Tés y Polvos Base (Matcha/Chai/Taro)', 'gramos', 999999.0, 10.0, 499999.0, 0.0, 500000.0, true, NULL, TRUE, NOW(), NOW()),
        (v_tenant_id, 'Fruta y Pulpa Barra Petra', 'gramos', 999999.0, 10.0, 499999.0, 0.0, 500000.0, true, NULL, TRUE, NOW(), NOW()),
        (v_tenant_id, 'Agua y Refrescos Insumo Base', 'pieza', 999999.0, 10.0, 499999.0, 0.0, 500000.0, true, NULL, TRUE, NOW(), NOW());

    -- 5. INSERTAR NUEVAS CATEGORÍAS Y PRODUCTOS OFICIALES
    -- Categoria: ENTRADAS
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'ENTRADAS', 'Entradas frías, calientes y bocados para iniciar.', 1, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 110.00, NULL, 'COPA DE FRUTOS ROJOS', 'Mix de frutos rojos, yogurt, granola y miel.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Fruta y Pulpa Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 195.00, NULL, 'FRENCH TOAST', 'Pan Brioche de la casa, endulzado y servido con mousse de chocolate blanco y semi amargo.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 40.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 215.00, NULL, 'FRENCH TOAST ESPECIAL PETRA', 'Pan Brioche de la casa, endulzado y servido con mousse de chocolate blanco y semi amargo. Versión especial.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 60.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 150.00, NULL, 'BOWL DE AÇAÍ', 'Açaí licuado con leche de coco y plátano, acompañado con topping a elegir (coco, pepita de calabaza, granola, chocolate, arandano) con un toque de miel de agave.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Fruta y Pulpa Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 150.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 215.00, NULL, 'BETABEL ROSTIZADO', '100gr de betabel rostizado con jocoque, pimienta limón, pistaches y reducción de vino tinto.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Guarnición y Vegetales Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 30.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 180.00, NULL, 'TOSTADAS DE ATÚN 2 PZS', '50 gr de atún laminado en salsa ponzu ligeramente picante, aguacate, chile serrano, pepino y mayonesa.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 2.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 210.00, NULL, 'CROQUETA DE PLATANO MACHO', 'Relleno de filete de res y queso gouda, sobre un mole de flor de calabaza hecho en casa.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 60.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 40.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 275.00, NULL, 'EMPANADAS DE CAMARON Y MARACUYÁ', '3 pzs rellenas de camarón, queso manchego, tomate, con salsa de maracuyá y chipotle.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 90.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 40.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 185.00, NULL, 'MINI CROQUETAS DE JAMON SERRANO', 'Crujientes croquetas de Bechamel, con aderezo de chipotle.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 135.00, NULL, 'TRUFFLE FRIES', 'Papas corte recto (300gr), parmesano y aceite de trufa.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Guarnición y Vegetales Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 300.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 20.00, FALSE);
    END IF;

    -- Categoria: ENCHILADAS
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'ENCHILADAS', 'Enchiladas tradicionales y de autor servidas con frijoles y guarniciones.', 2, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 225.00, NULL, 'ENCHILADAS PETRA (LAS AMARILLITAS)', 'Salsa de pimiento con un toque de chile manzano, acompañadas de queso, crema y cebolla. (100 gr. de Pollo)', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Insumo Base Alimento Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 210.00, NULL, 'ENCHILADAS VERDES O ROJAS', 'Rellenas con 100 gr. de pollo, servidas con queso, crema, cebolla y frijoles refritos.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Insumo Base Alimento Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 215.00, NULL, 'ENCHILADAS SUIZAS VERDES', 'Rellenas de 100 gr. de pollo, gratinadas con queso, crema, cebolla y frijoles refritos.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;

    -- Categoria: CHILAQUILES
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'CHILAQUILES', 'Chilaquiles crujientes con salsas tradicionales y especiales de la casa.', 3, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 205.00, NULL, 'CHILAQUILES VERDES O ROJOS', 'Proteína a elegir: huevo 2 pzs, pollo deshebrado, chorizo, milanesa de pollo. Servidos con queso, crema y cebolla. (100 gr. de proteína).', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Insumo Base Alimento Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 225.00, NULL, 'ESPECIALES PETRA', 'En salsa blanca de Habanero y queso, gratinados, servidos con 100 gr de arrachera, con aguacate, crema y cebolla.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 215.00, NULL, 'CHILAQUILES ENFRIJOLADOS', 'Salsa de frijol con un toque de chipotle, chorizo (100gr) servidos con crema, queso, aguacate y migas de chicharron.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Insumo Base Alimento Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    -- Categoria: BREAKFAST & BRUNCH
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'BREAKFAST & BRUNCH', 'Desayunos, toasts, omelettes y waffles.', 4, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 260.00, NULL, 'CHICKEN WAFFLE', 'Pechuga de pollo frito, salsa de maple natural o salsa de maple picosa, queso y papas fritas.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 150.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 190.00, NULL, 'AVOCADO TOAST', 'Cremoso de aguacate servido sobre pan hogaza, 1 pza de huevo duro, sal pimienta y aceite de oliva.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Guarnición y Vegetales Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 80.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 245.00, NULL, 'SALMÓN TOAST', 'Cremoso de aguacate servido sobre pan hogaza, 50 gr de salmón ahumado, arugula y reducción de vino tinto.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 245.00, NULL, 'BENEDICTINOS PETRA', 'Huevos pochados servidos sobre english muffin, 100 gr de salmón ahumado, bañados en salsa holandesa.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 210.00, NULL, 'OMELETTE MILPA', 'Relleno de, champiñón, granos de elote, rajas poblanas, chilacayote y queso panela. Bañado en salsa poblana.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Insumo Base Alimento Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Guarnición y Vegetales Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 240.00, NULL, 'OMELETTE SALMON', 'Relleno de salmón ahumado, espinaca, requesón, sobre una salsa roja de tomate. Acompañado con mix de lechugas.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 60.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 40.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 185.00, NULL, 'HUEVOS AL GUSTO', '(Jamón, chorizo, a la mexicana, tocino), acompañados de chilaquiles y frijoles refritos.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 80.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Insumo Base Alimento Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    -- Categoria: ENSALADAS
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'ENSALADAS', 'Ensaladas frescas y aderezos de autor.', 5, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 245.00, NULL, 'BURRATA SALAD', 'Frescas rodajas de tomate y burrata, aceite de oliva, hojas de albahaca con reducción de vino tinto.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 120.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Guarnición y Vegetales Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 195.00, NULL, 'ENSALADA DE BETABEL', 'Betabel rostizado, con arugula, perlas de queso de cabra y vinagreta de vino.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Guarnición y Vegetales Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 120.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 40.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 255.00, NULL, 'ENSALADA CÉSAR PETRA (POLLO)', 'Lechuga troceada, queso parmesano y aderezo echo en casa. Con 100 gr de pollo.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Guarnición y Vegetales Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 270.00, NULL, 'ENSALADA CÉSAR PETRA (CAMARÓN)', 'Lechuga troceada, queso parmesano y aderezo echo en casa. Con 100 gr de camarón.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Guarnición y Vegetales Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 270.00, NULL, 'ENSALADA CÉSAR PETRA (ARRACHERA)', 'Lechuga troceada, queso parmesano y aderezo echo en casa. Con 100 gr de arrachera.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Guarnición y Vegetales Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;

    -- Categoria: SOPAS Y PASTAS
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'SOPAS Y PASTAS', 'Caldos, cremas y pastas frescas preparadas al momento.', 6, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 180.00, NULL, 'EL APAPACHO', 'Cosecha de la milpa con fondo de pollo, acompañada de chile verde, cebolla y tropiezos de panela. (Pollo 50 gr)', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Insumo Base Alimento Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 225.00, NULL, 'SOPA DE TORTILLA', 'Servida con camarón y pulpo al gratín, julianas de tortilla, aguacate, queso panela, crema, chicharrón y guindillas de chile guajillo.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 80.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Insumo Base Alimento Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 215.00, NULL, 'PASTA TRAVIATA', 'Fetuccini al pomodoro cremoso ligeramente picante, servida con punta de filete de Res y queso parmesano. (50 gr de Filete)', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Insumo Base Alimento Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 230.00, NULL, 'PAPPARDELLE RELLENO DE RAGÚ', 'Pasta fresca rellena de ragú de res (180 gr), servido en salsa blanca de jamón serrano y vino blanco.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 180.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Insumo Base Alimento Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    -- Categoria: LOS ESPECIALES
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'LOS ESPECIALES', 'Cortes selectos, pescados y especialidades de la casa.', 7, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 410.00, NULL, 'FILETE MAR Y TIERRA', '170 gr de Filete mignon acompañado de 2 pzs de camarones, en salsa roja de langostinos y micro retoños.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 200.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Guarnición y Vegetales Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 385.00, NULL, 'PECHUGA MATAMOROS', 'Pechuga de pollo rellena de surimi y queso crema, salsa bechamel, salsa de mango picante y ensalada verde.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 180.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 400.00, NULL, 'SALMÓN SPICY', '180 gr de filete de salmón asado, puré de guisantes, manzana asada, salsa citrica y ensalada verde.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 180.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Guarnición y Vegetales Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 315.00, NULL, 'PETRA BURGUER', '150 gr de carne de res choice, queso gouda, mermelada de tocino, aderezo de la casa con papas gajo.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 150.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 220.00, NULL, 'FISH & CHIPS', '100 gr de pescado, con un rebozado de cerveza, repollo, salsa tartara y papas fritas.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Guarnición y Vegetales Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 150.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 210.00, NULL, 'SANDWICH DE ATÚN', 'Pan brioche tostado y ensalada de atún (receta de la casa) acompañado de guarnición a elegir (papa gajo, papa francesa o ensalada).', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 120.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 195.00, NULL, 'MENU INFANTIL', 'Pechuga de pollo empanizada, con guarnición de pasta a la crema y papas a la francesa. (100 gr)', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Guarnición y Vegetales Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;

    -- Categoria: SNACKS
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'SNACKS', 'Ciabattas, bagels, baguettes y opciones ligeras.', 8, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 125.00, NULL, 'CIABATTA DE POLLO AL GRILL', '100 gr de pechuga de pollo a la plancha, queso gouda gratinado, mermelada de tocino.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 115.00, NULL, 'CROISSANT DE PECHUGA DE PAVO', 'Pechuga de pavo, queso gouda, aderezo de chipotle.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 80.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 145.00, NULL, 'BAGEL DE SALMON AHUMADO', 'Bagel echo en casa, salmón ahumado, queso crema y alcaparra. (50 gr)', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 135.00, NULL, 'BAGUETTE DE CARNES FRIAS', 'Pan brioche, jamón serrano, pechuga de pavo, pepperoni, Arúgula, mayonesa y mostaza dijón. (100 gr)', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 60.00, NULL, 'PUDING DE CHÍA', 'Macerado 12hrs con leche de coco, acompañado con un mix de semillas y fruto secos.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 150.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Fruta y Pulpa Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 60.00, NULL, 'AVENA TRASNOCHADA', 'Macerado 12hrs con leche de deslactosada, acompañado con un mix de semillas y fruto secos.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 150.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Fruta y Pulpa Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;

    -- Categoria: POSTRES
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'POSTRES', 'Repostería fina, tartas y creaciones dulces.', 9, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 148.00, NULL, 'TARTA DE PISTACHE', 'Rellena de praliné de pistache, compota y frambuesas frescas.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Insumo Base Alimento Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Fruta y Pulpa Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 30.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 150.00, NULL, 'FAISAN PETRA', 'Base de bizcocho de almendra, crema catalana y mouse de chocolate, sobre galleta sable.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Insumo Base Alimento Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 40.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 145.00, NULL, 'PASTEL DE CHOCOLATE', 'Bizcocho de chocolate, relleno de ganache de chocolate y mermelada de chabacano.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Insumo Base Alimento Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 185.00, NULL, 'FRAISIER DE MARACUYÁ', 'Gelatina de maracuyá, bizcocho de vainilla, crema de hierbabuena.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Insumo Base Alimento Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Fruta y Pulpa Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 40.00, FALSE);
    END IF;

    -- Categoria: PANADERÍA
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'PANADERÍA', 'Pan dulce horneado diariamente y panqués.', 10, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 70.00, NULL, 'PANQUE DE LIMÓN', 'Panqué de limón con semilla de amapola.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 75.00, NULL, 'PANQUE DE PLÁTANO', 'Panqué de plátano con chocolate y nuez.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 65.00, NULL, 'CROISSANT', 'Croissant clásico horneado de mantequilla.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 65.00, NULL, 'CHOCOLATÍN', 'Chocolatín hojaldrado con chocolate.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 75.00, NULL, 'DANÉS', 'Pan danés dulce tradicional.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 35.00, NULL, 'GALLETA DE AVENA', 'Galleta de avena casera.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    -- Categoria: EXTRAS
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'EXTRAS', 'Porciones y proteínas adicionales para complementar tu orden.', 11, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 15.00, NULL, 'HUEVO EXTRA 1 PZA', 'Huevo extra 1 pza.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Insumo Base Alimento Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 50.00, NULL, 'CHORIZO EXTRA 100 GR', 'Chorizo extra 100 gr.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 75.00, NULL, 'ARRACHERA EXTRA 100 GR', 'Arrachera extra 100 gr.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 40.00, NULL, 'POLLO EXTRA 100 GR', 'Pollo extra 100 gr.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 55.00, NULL, 'PECHUGA DE POLLO 100 GR', 'Pechuga de pollo 100 gr.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Proteína Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 45.00, NULL, 'BOLA DE HELADO EXTRA', 'Bola de helado extra.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 70.00, NULL, 'PAPA GAJO 200 GR', 'Papa gajo 200 gr.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Guarnición y Vegetales Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 200.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 65.00, NULL, 'CANASTA DE PAN 60 GR', 'Canasta de pan 60 gr.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'COCINA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Panadería y Masas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    -- Categoria: CAFÉ
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'CAFÉ', 'Café de especialidad, espressos y métodos.', 12, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 45.00, NULL, 'ESPRESSO', 'Espresso sencillo de especialidad.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Café de Grano Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 18.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 55.00, NULL, 'ESPRESSO DOBLE', 'Doble extracción de espresso.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Café de Grano Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 36.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 70.00, NULL, 'ESPRESSO TONIC', 'Espresso servido sobre agua tónica y hielo.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Café de Grano Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 18.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Agua y Refrescos Insumo Base' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 50.00, NULL, 'AMERICANO', 'Espresso diluido con agua caliente.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Café de Grano Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 18.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 55.00, NULL, 'AEROCANO', 'Americano texturizado y aireado al vapor.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Café de Grano Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 18.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 60.00, NULL, 'LATTE', 'Espresso con leche vaporizada y capa delgada de espuma.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Café de Grano Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 18.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 200.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 60.00, NULL, 'FLAT WHITE', 'Doble ristretto con microespuma de leche sedosa.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Café de Grano Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 25.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 150.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 90.00, NULL, 'AFFOGATTO (HELADO) VAINILLA', 'Bola de helado de vainilla ahogada con un shot de espresso.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Café de Grano Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 18.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Lácteos y Quesos Cocina Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 75.00, NULL, 'COLD BREW', 'Extracción en frío durante 18 horas.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Café de Grano Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 30.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 75.00, NULL, 'MOKA BLANCO / OSCURO', 'Espresso con leche y chocolate blanco u oscuro a elegir.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Café de Grano Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 18.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 200.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 70.00, NULL, 'CAPUCHINO', 'Espresso, leche vaporizada y abundante espuma de leche.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Café de Grano Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 18.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 180.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 85.00, NULL, 'CAPUCHINO SABORES', 'Capuchino con jarabe a elegir: caramelo, vainilla francesa, avellana, crema irlandesa, chocolate.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Café de Grano Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 18.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 180.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Jarabes y Sabores Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 30.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 75.00, NULL, 'CARAMEL MACCHIATO', 'Leche vaporizada con vainilla manchada de espresso y rejilla de caramelo.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Café de Grano Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 18.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 180.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Jarabes y Sabores Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 20.00, FALSE);
    END IF;

    -- Categoria: BEBIDAS SIN CAFÉ
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'BEBIDAS SIN CAFÉ', 'Chocolates, lattes de especialidad, jugos y aguas.', 13, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 60.00, NULL, 'CHOCOLATE BLANCO / OSCURO', 'Chocolate artesanal blanco u oscuro con leche.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 250.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Jarabes y Sabores Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 40.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 80.00, NULL, 'CHOCOLATE BELGA', 'Auténtico chocolate belga fundido en leche cremada.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 250.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Jarabes y Sabores Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 180.00, NULL, 'BAD BUNNY', 'Frangelico, leche cremada, conejo turín.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 200.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Jarabes y Sabores Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 40.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 80.00, NULL, 'CHAI LATTE CLÁSICO', 'Especias masala chai con leche vaporizada.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Tés y Polvos Base (Matcha/Chai/Taro)' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 30.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 220.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 95.00, NULL, 'CHAI LATTE SABORES', 'Chai latte con sabor a elegir: caramelo, vainilla francesa, avellana, crema irlandesa, chocolate.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Tés y Polvos Base (Matcha/Chai/Taro)' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 30.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 220.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Jarabes y Sabores Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 30.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 80.00, NULL, 'TARO', 'Infusión cremosa de raíz de taro con leche.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Tés y Polvos Base (Matcha/Chai/Taro)' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 30.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 220.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 80.00, NULL, 'GOLDEN MILK', 'Leche dorada con cúrcuma, canela, jengibre y especias.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Tés y Polvos Base (Matcha/Chai/Taro)' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 20.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 220.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 38.00, NULL, 'REFRESCO', 'Refresco embotellado 355 ml (variedad).', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Agua y Refrescos Insumo Base' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 20.00, NULL, 'AGUA EMBOTELLADA', 'Agua purificada natural 600 ml.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Agua y Refrescos Insumo Base' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 25.00, NULL, 'AGUA MINERAL', 'Agua mineral gasificada 355 ml.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Agua y Refrescos Insumo Base' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 58.00, NULL, 'AGUA MINERAL PERRIER', 'Agua mineral Perrier 330 ml.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Agua y Refrescos Insumo Base' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 60.00, NULL, 'JUGO DE NARANJA', 'Jugo de naranja natural recién exprimido.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Fruta y Pulpa Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 250.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 70.00, NULL, 'JUGO VERDE', 'Jugo detox con naranja, piña, apio, nopal y espinaca.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Fruta y Pulpa Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 250.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 70.00, NULL, 'JUGO FRUTOS ROJOS', 'Jugo antioxidante de frutos rojos naturales.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Fruta y Pulpa Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 250.00, FALSE);
    END IF;

    -- Categoria: TÉS E INFUSIONES
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'TÉS E INFUSIONES', 'Matcha ceremonial, tés puros e infusiones frutales.', 14, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 85.00, NULL, 'MATCHA (AGUA)', 'Matcha ceremonial batido con agua.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Tés y Polvos Base (Matcha/Chai/Taro)' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 15.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 95.00, NULL, 'MATCHA LATTE (LECHE)', 'Matcha ceremonial batido con leche vaporizada.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Tés y Polvos Base (Matcha/Chai/Taro)' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 15.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 220.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 115.00, NULL, 'MATCHA FRUTAL', 'Matcha con pulpa frutal a elegir: mango, fresa o frutos rojos.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Tés y Polvos Base (Matcha/Chai/Taro)' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 15.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Fruta y Pulpa Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 80.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 65.00, NULL, 'TÉ NEGRO', 'Té negro en hoja suelta de alta gama.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Tés y Polvos Base (Matcha/Chai/Taro)' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 10.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 65.00, NULL, 'TÉ VERDE NARANJA Y MANGO', 'Té verde aromático con toques de naranja y mango.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Tés y Polvos Base (Matcha/Chai/Taro)' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 10.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 75.00, NULL, 'TISANAS', 'Tisana frutal a elegir: frutos rojos o frutos de la pasión.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Fruta y Pulpa Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 30.00, FALSE);
    END IF;

    -- Categoria: FRAPPES
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'FRAPPES', 'Bebidas frappeadas a base de café, té o fruta natural.', 15, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 85.00, NULL, 'FRAPUCCINO CLÁSICO', 'Frappé clásico a base de espresso y leche cremada.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Café de Grano Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 18.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 150.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 95.00, NULL, 'FRAPUCCINO SABORES', 'Frappé con sabor a elegir: caramelo, vainilla francesa, avellana, crema irlandesa, chocolate.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Café de Grano Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 18.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 150.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Jarabes y Sabores Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 30.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 95.00, NULL, 'FRAPPE CREAM', 'Frappé sin café a base de crema con sabor: caramelo, vainilla francesa, avellana, crema irlandesa, chocolate.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 200.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Jarabes y Sabores Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 40.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 105.00, NULL, 'FRAPPE MATCHA', 'Frappé cremoso de matcha ceremonial.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Tés y Polvos Base (Matcha/Chai/Taro)' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 20.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 180.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 105.00, NULL, 'FRAPPE CHAI', 'Frappé especiado masala chai con leche.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Tés y Polvos Base (Matcha/Chai/Taro)' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 25.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 180.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 100.00, NULL, 'FRAPPE TARO', 'Frappé cremoso de taro dulce.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Tés y Polvos Base (Matcha/Chai/Taro)' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 25.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Leche y Bases Líquidas Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 180.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 90.00, NULL, 'FRAPPE FRUTA NATURAL', '(Base de agua, mango, fresa o frutos rojos) agrega miguelito y chamoy por $10.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Fruta y Pulpa Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 150.00, FALSE);
    END IF;

    -- Categoria: REFRESHERS
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'REFRESHERS', 'Bebidas refrescantes de autor sin alcohol.', 16, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 60.00, NULL, 'LIMONADA', 'Limonada natural o mineral recién preparada.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Fruta y Pulpa Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 60.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Agua y Refrescos Insumo Base' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 60.00, NULL, 'NARANJADA', 'Naranjada natural o mineral recién preparada.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Fruta y Pulpa Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 100.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Agua y Refrescos Insumo Base' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 75.00, NULL, 'MOCKTAIL', 'Coctelería fresca de autor sin alcohol con frutas y botánicos.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Fruta y Pulpa Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 80.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Jarabes y Sabores Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 30.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 75.00, NULL, 'MOJITO SIN ALCOHOL', 'Macerado de hierbabuena fresca, jugo de limón, jarabe y agua mineral.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Fruta y Pulpa Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 50.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Jarabes y Sabores Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 30.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Agua y Refrescos Insumo Base' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    -- Categoria: SODAS ITALIANAS
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    VALUES (v_tenant_id, 'SODAS ITALIANAS', 'Sodas artesanales carbonatadas infusionadas.', 17, TRUE, NOW(), NOW())
    RETURNING id INTO v_cat_id;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 85.00, NULL, 'SODA ITALIANA TÉ VERDE NARANJA Y MANGO', 'Soda carbonatada infusionada con té verde, naranja y mango.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Jarabes y Sabores Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 40.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Agua y Refrescos Insumo Base' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 95.00, NULL, 'SODA ITALIANA TISANA FRUTOS DE LA PASIÓN', 'Soda carbonatada con infusión de tisana frutos de la pasión.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Jarabes y Sabores Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 40.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Agua y Refrescos Insumo Base' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    INSERT INTO tenant_menu_product (category_id, precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, disponible_kiosko, area, created_at, updated_at)
    VALUES (v_cat_id, 95.00, NULL, 'SODA ITALIANA TISANA FRUTOS ROJOS', 'Soda carbonatada con infusión de tisana frutos rojos naturales.', 9999.0, 10.0, 'pieza', TRUE, TRUE, FALSE, TRUE, TRUE, 'BARRA', NOW(), NOW())
    RETURNING id INTO v_prod_id;
    INSERT INTO tenant_menu_product_category (product_id, category_id) VALUES (v_prod_id, v_cat_id);
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Jarabes y Sabores Barra Petra' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 40.00, FALSE);
    END IF;
    SELECT id INTO v_insumo_id FROM insumo WHERE tenant_id = v_tenant_id AND nombre = 'Agua y Refrescos Insumo Base' LIMIT 1;
    IF v_insumo_id IS NOT NULL THEN
        INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable) VALUES (v_prod_id, v_insumo_id, 1.00, FALSE);
    END IF;

    -- 6. AJUSTAR SECUENCIAS DE POSTGRESQL
    PERFORM setval('tenant_menu_category_id_seq', COALESCE((SELECT MAX(id) FROM tenant_menu_category), 1));
    PERFORM setval('tenant_menu_product_id_seq', COALESCE((SELECT MAX(id) FROM tenant_menu_product), 1));
    PERFORM setval('insumo_id_seq', COALESCE((SELECT MAX(id) FROM insumo), 1));
    PERFORM setval('product_recipe_id_seq', COALESCE((SELECT MAX(id) FROM product_recipe), 1));
    PERFORM setval('tenant_user_id_seq', COALESCE((SELECT MAX(id) FROM tenant_user), 1));

    RAISE NOTICE '¡Menú Oficial de Restaurante Petra cargado exitosamente!';
END $$;
