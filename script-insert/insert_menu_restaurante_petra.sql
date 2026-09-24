-- ==============================================================================
-- LEALTIX - SCRIPT DE CARGA DEL MENÚ COMPLETO (RESTAURANTE PETRA)
-- ==============================================================================
-- Este script inserta la configuración completa del menú gastronómico:
--   1. Categorías del Menú (14 categorías)
--   2. Insumos de Inventario y Materia Prima (113 insumos)
--   3. Sub-recetas / Preparaciones Intermedias (29 sub-recetas)
--   4. Platillos y Extras del Menú (58 platillos y complementos)
--   5. Asignación de Categorías a Productos (tenant_menu_product_category)
--   6. Recetas de Platillos y Sub-recetas (332 líneas en product_recipe)
--   7. Vinculación de Sub-recetas a Platillos (48 asignaciones en product_sub_receta)
--
-- CARACTERÍSTICAS DE DISEÑO:
--   * 100% Relacional y Dinámico: No depende de IDs quemados/hardcodeados.
--   * Idempotente y Seguro: Emplea comprobaciones WHERE NOT EXISTS y ON CONFLICT
--     para evitar duplicados sin romper llaves foráneas de pedidos preexistentes.
--   * Compatible con cualquier Tenant: Se vincula al tenant existente (slug 'demo',
--     'restaurante-petra' o el primer tenant). Si la BD está vacía, crea uno base.
--   * Ajusta automáticamente las secuencias (setval) al finalizar.
--
-- CÓMO EJECUTAR:
--   Opción A) psql -U postgres -d lealtix_db -f insert_menu_restaurante_petra.sql
--   Opción B) En pgAdmin / DBeaver / Neon Console: Copiar todo el contenido y Ejecutar.
-- ==============================================================================

BEGIN;

DO $$
DECLARE
    v_tenant_id BIGINT;
    v_user_id BIGINT;
    -- Cambiar a TRUE únicamente si se desea forzar la eliminación total de comandas
    -- y menú de prueba previo para reinsertar todo desde cero.
    v_force_reset BOOLEAN := FALSE;
BEGIN
    -- --------------------------------------------------------------------------
    -- 0. RESOLUCIÓN DEL TENANT OBJETIVO
    -- --------------------------------------------------------------------------
    SELECT id INTO v_tenant_id FROM tenant WHERE slug IN ('demo', 'restaurante-petra', 'la-taqueria-demo') ORDER BY id ASC LIMIT 1;
    
    IF v_tenant_id IS NULL THEN
        SELECT id INTO v_tenant_id FROM tenant ORDER BY id ASC LIMIT 1;
    END IF;

    -- Si no existe ningún tenant en la base de datos, se crea uno base para demostración
    IF v_tenant_id IS NULL THEN
        SELECT id INTO v_user_id FROM app_user WHERE email = 'admin@demo.com' LIMIT 1;
        IF v_user_id IS NULL THEN
            INSERT INTO app_user (full_name, email, password_hash, is_active, created_at, updated_at)
            VALUES ('Administrador Petra', 'admin@restaurante-petra.com', '$2a$10$hIYO4NaVFOupU3G03sT1x.X5cB/pu6FX/z7JFVHy2Y1xzqJBH9W2O', true, NOW(), NOW())
            RETURNING id INTO v_user_id;
        END IF;

        INSERT INTO tenant (
            nombre_negocio, direccion, telefono, tipo_negocio,
            slug, uid_tenant, schedules,
            slogan, kitchen_module_enabled, kitchen_enabled_at,
            is_active, created_at, updated_at, user_id
        )
        VALUES (
            'Restaurante Petra', 'Av. Gourmet 100, CDMX', '+52 55 1122 3344', 'Restaurante',
            'restaurante-petra', 'UID-PETRA-001', 'Mar-Dom: 08:00-23:00',
            'Cocina de autor y tradición', true, NOW(),
            true, NOW(), NOW(), v_user_id
        )
        RETURNING id INTO v_tenant_id;
        
        RAISE NOTICE 'Se ha creado un nuevo tenant con ID: %', v_tenant_id;
    ELSE
        RAISE NOTICE 'Utilizando tenant existente con ID: %', v_tenant_id;
    END IF;

    -- --------------------------------------------------------------------------
    -- 1. LIMPIEZA CONDICIONAL (Sólo si v_force_reset = TRUE)
    -- --------------------------------------------------------------------------
    IF v_force_reset THEN
        RAISE NOTICE 'v_force_reset activado: Limpiando pedidos y menú previo del tenant %...', v_tenant_id;

        -- Limpieza de ítems de órdenes de clientes
        DELETE FROM client_order_item WHERE product_id IN (
            SELECT p.id FROM tenant_menu_product p 
            JOIN tenant_menu_category c ON p.category_id = c.id 
            WHERE c.tenant_id = v_tenant_id
        );

        -- Limpieza de cross-selling
        DELETE FROM product_cross_selling WHERE tenant_id = v_tenant_id;

        -- Limpieza de sub-recetas asignadas
        DELETE FROM product_sub_receta 
        WHERE dish_product_id IN (
            SELECT p.id FROM tenant_menu_product p 
            JOIN tenant_menu_category c ON p.category_id = c.id 
            WHERE c.tenant_id = v_tenant_id
        ) OR sub_receta_id IN (
            SELECT p.id FROM tenant_menu_product p 
            JOIN tenant_menu_category c ON p.category_id = c.id 
            WHERE c.tenant_id = v_tenant_id
        );

        -- Limpieza de recetas
        DELETE FROM product_recipe 
        WHERE dish_product_id IN (
            SELECT p.id FROM tenant_menu_product p 
            JOIN tenant_menu_category c ON p.category_id = c.id 
            WHERE c.tenant_id = v_tenant_id
        );

        -- Limpieza de adicionales
        DELETE FROM product_additional 
        WHERE dish_product_id IN (
            SELECT p.id FROM tenant_menu_product p 
            JOIN tenant_menu_category c ON p.category_id = c.id 
            WHERE c.tenant_id = v_tenant_id
        );

        -- Limpieza de multicategorías
        DELETE FROM tenant_menu_product_category 
        WHERE product_id IN (
            SELECT p.id FROM tenant_menu_product p 
            JOIN tenant_menu_category c ON p.category_id = c.id 
            WHERE c.tenant_id = v_tenant_id
        );

        -- Limpieza de productos y preparaciones
        DELETE FROM tenant_menu_product 
        WHERE category_id IN (
            SELECT id FROM tenant_menu_category WHERE tenant_id = v_tenant_id
        );

        -- Limpieza de insumos
        DELETE FROM insumo WHERE tenant_id = v_tenant_id;

        -- Limpieza de categorías
        DELETE FROM tenant_menu_category WHERE tenant_id = v_tenant_id;

        RAISE NOTICE 'Menú previo reseteado con éxito.';
    END IF;

    -- --------------------------------------------------------------------------
    -- 2. INSERTAR CATEGORÍAS DEL MENÚ (Idempotente)
    -- --------------------------------------------------------------------------
    INSERT INTO tenant_menu_category (tenant_id, nombre, descripcion, display_order, is_active, created_at, updated_at)
    SELECT v_tenant_id, c.nombre, c.descripcion, c.display_order, c.is_active, NOW(), NOW()
    FROM (VALUES

        ('DESAYUNOS'::varchar, ''::varchar, 1::int, true::bool),
        ('ENCHILADAS'::varchar, ''::varchar, 2::int, true::bool),
        ('CHILAQUILES'::varchar, ''::varchar, 3::int, true::bool),
        ('ESPECIALES'::varchar, ''::varchar, 4::int, true::bool),
        ('HUEVOS'::varchar, ''::varchar, 5::int, true::bool),
        ('COMIDAS'::varchar, ''::varchar, 6::int, true::bool),
        ('ENTRADAS'::varchar, ''::varchar, 7::int, true::bool),
        ('SOPAS Y PASTAS'::varchar, ''::varchar, 8::int, true::bool),
        ('ENSALADAS'::varchar, ''::varchar, 9::int, true::bool),
        ('PLATOS FUERTES'::varchar, ''::varchar, 10::int, true::bool),
        ('MENÚ INFANTIL'::varchar, ''::varchar, 11::int, true::bool),
        ('EXTRAS'::varchar, ''::varchar, 12::int, true::bool),
        ('POSTRES'::varchar, ''::varchar, 13::int, true::bool),
        ('Preparaciones'::varchar, 'Sub-recetas (preparaciones intermedias)'::varchar, 99::int, false::bool)
    ) AS c(nombre, descripcion, display_order, is_active)
    WHERE NOT EXISTS (
        SELECT 1 FROM tenant_menu_category ec 
        WHERE ec.tenant_id = v_tenant_id AND ec.nombre = c.nombre
    );

    -- --------------------------------------------------------------------------
    -- 3. INSERTAR INSUMOS DE COCINA Y MATERIA PRIMA (Idempotente)
    -- --------------------------------------------------------------------------
    INSERT INTO insumo (
        tenant_id, nombre, unidad, stock, stock_minimo,
        stock_bodega, stock_cocina, stock_barra,
        es_bebida, precio_venta, is_active, created_at, updated_at
    )
    SELECT 
        v_tenant_id, i.nombre, i.unidad, i.stock, i.stock_minimo,
        i.stock_bodega, i.stock_cocina, i.stock_barra,
        i.es_bebida, i.precio_venta, i.is_active, NOW(), NOW()
    FROM (VALUES

        ('Tortilla de maíz'::varchar, 'pieza'::varchar, 38::float8, 10::float8, 50::float8, 38::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Huevo fresco'::varchar, 'pieza'::varchar, 38::float8, 10::float8, 50::float8, 38::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Pan brioche'::varchar, 'pieza'::varchar, 45::float8, 10::float8, 50::float8, 45::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Pan ciabatta'::varchar, 'pieza'::varchar, 50::float8, 10::float8, 50::float8, 50::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Pan artesanal'::varchar, 'pieza'::varchar, 43::float8, 10::float8, 50::float8, 43::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('English muffin'::varchar, 'pieza'::varchar, 48::float8, 10::float8, 50::float8, 48::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Croqueta de plátano macho cruda'::varchar, 'pieza'::varchar, 50::float8, 10::float8, 50::float8, 50::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Empanada de camarón y maracuyá cruda'::varchar, 'pieza'::varchar, 50::float8, 10::float8, 50::float8, 50::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Rueda de queso provolone'::varchar, 'pieza'::varchar, 50::float8, 10::float8, 50::float8, 50::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Rollo de pechuga especial crudo'::varchar, 'pieza'::varchar, 50::float8, 10::float8, 50::float8, 50::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Base de galleta sable'::varchar, 'pieza'::varchar, 50::float8, 10::float8, 50::float8, 50::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Canasta de pan'::varchar, 'pieza'::varchar, 50::float8, 10::float8, 50::float8, 50::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Camarón gigante U15'::varchar, 'pieza'::varchar, 50::float8, 10::float8, 50::float8, 50::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Aguacate hass entero'::varchar, 'pieza'::varchar, 50::float8, 10::float8, 50::float8, 50::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Totopos'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Pechuga de pollo deshebrada'::varchar, 'gramos'::varchar, 4600::float8, 500::float8, 5000::float8, 4600::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Pechuga de pollo fileteada'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Cecina de res'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Arrachera marinada'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Chorizo de cerdo'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Jamón de pavo'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Tocino ahumado'::varchar, 'gramos'::varchar, 4900::float8, 500::float8, 5000::float8, 4900::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Jamón serrano'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Filete de res'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Rib eye choice'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Picanha de res'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Filete de salmón fresco'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Salmón ahumado laminado'::varchar, 'gramos'::varchar, 4800::float8, 500::float8, 5000::float8, 4800::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Atún fresco laminado'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Pulpo cocido'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Camarón pacotilla'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Surimi'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Queso fresco de rancho'::varchar, 'gramos'::varchar, 4880::float8, 500::float8, 5000::float8, 4880::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Queso gouda rallado'::varchar, 'gramos'::varchar, 4960::float8, 500::float8, 5000::float8, 4960::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Queso manchego rallado'::varchar, 'gramos'::varchar, 4960::float8, 500::float8, 5000::float8, 4960::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Queso parmesano rallado'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Queso panela en cubos'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Queso de cabra'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Queso mascarpone'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Requesón fresco'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Queso crema'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Frijol negro en grano'::varchar, 'gramos'::varchar, 4680::float8, 500::float8, 5000::float8, 4680::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Manteca de cerdo'::varchar, 'gramos'::varchar, 4940::float8, 500::float8, 5000::float8, 4940::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Pasta fettuccini'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Pasta pappardelle'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Frutos rojos frescos'::varchar, 'gramos'::varchar, 4480::float8, 500::float8, 5000::float8, 4480::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Granola de la casa'::varchar, 'gramos'::varchar, 4840::float8, 500::float8, 5000::float8, 4840::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Fresas frescas'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Durazno fresco'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Almendra fileteada'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Nuez caramelizada'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Arándanos deshidratados'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Mix de lechugas orgánicas'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Lechuga orejona'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Espinaca baby'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Calabaza italiana'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Champiñones frescos'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Granos de elote amarillo'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Chilacayote picado'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Pepino fresco'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Manzana verde'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Papa cambray'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Papa gajo congelada'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Hojuelas de puré de papa'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Espárragos frescos'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Mix de vegetales'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Mantequilla sin sal'::varchar, 'gramos'::varchar, 4700::float8, 500::float8, 5000::float8, 4700::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Harina de trigo'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Azúcar estándar'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Chocolate semi-amargo'::varchar, 'gramos'::varchar, 4865::float8, 500::float8, 5000::float8, 4865::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Chocolate blanco'::varchar, 'gramos'::varchar, 4880::float8, 500::float8, 5000::float8, 4880::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Pistache pelado'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Frambuesas frescas'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Pasta de mole tradicional'::varchar, 'gramos'::varchar, 4920::float8, 500::float8, 5000::float8, 4920::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Achiote en pasta'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Tomate verde'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Jitomate guaje'::varchar, 'gramos'::varchar, 4900::float8, 500::float8, 5000::float8, 4900::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Cebolla blanca'::varchar, 'gramos'::varchar, 4860::float8, 500::float8, 5000::float8, 4860::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Cebolla morada'::varchar, 'gramos'::varchar, 4980::float8, 500::float8, 5000::float8, 4980::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Chile serrano'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Chile poblano'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Chile manzano'::varchar, 'gramos'::varchar, 4940::float8, 500::float8, 5000::float8, 4940::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Chile habanero'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Chile guajillo seco'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Pimiento amarillo'::varchar, 'gramos'::varchar, 4760::float8, 500::float8, 5000::float8, 4760::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Cilantro fresco'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Epazote fresco'::varchar, 'gramos'::varchar, 4980::float8, 500::float8, 5000::float8, 4980::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Albahaca fresca'::varchar, 'gramos'::varchar, 4990::float8, 500::float8, 5000::float8, 4990::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Perejil fresco'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Ajo pelado'::varchar, 'gramos'::varchar, 4995::float8, 500::float8, 5000::float8, 4995::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Flor de calabaza'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Esquites al mezcal'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Chicharrón de cerdo crujiente'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Helado de vainilla'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Fruta picada de temporada'::varchar, 'gramos'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Crema ácida de rancho'::varchar, 'mililitros'::varchar, 4730::float8, 500::float8, 5000::float8, 4730::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Crema para batir'::varchar, 'mililitros'::varchar, 4810::float8, 500::float8, 5000::float8, 4810::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Leche entera'::varchar, 'mililitros'::varchar, 4800::float8, 500::float8, 5000::float8, 4800::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Aceite vegetal para freír'::varchar, 'mililitros'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Aceite de oliva extra virgen'::varchar, 'mililitros'::varchar, 4990::float8, 500::float8, 5000::float8, 4990::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Salsa de soya'::varchar, 'mililitros'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Mayonesa de la casa'::varchar, 'mililitros'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Mostaza dijon'::varchar, 'mililitros'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Miel de abeja'::varchar, 'mililitros'::varchar, 4920::float8, 500::float8, 5000::float8, 4920::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Vino tinto de cocina'::varchar, 'mililitros'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Vino blanco de cocina'::varchar, 'mililitros'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Vinagre de manzana'::varchar, 'mililitros'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Pulpa de maracuyá'::varchar, 'mililitros'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Pulpa de mango'::varchar, 'mililitros'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Jugo de limón natural'::varchar, 'mililitros'::varchar, 4980::float8, 500::float8, 5000::float8, 4980::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Jugo de naranja natural'::varchar, 'mililitros'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Caldo de pollo concentrado'::varchar, 'mililitros'::varchar, 4940::float8, 500::float8, 5000::float8, 4940::float8, 0::float8, false::bool, NULL::numeric, true::bool),
        ('Café americano preparado'::varchar, 'mililitros'::varchar, 5000::float8, 500::float8, 5000::float8, 5000::float8, 0::float8, false::bool, NULL::numeric, true::bool)
    ) AS i(nombre, unidad, stock, stock_minimo, stock_bodega, stock_cocina, stock_barra, es_bebida, precio_venta, is_active)
    WHERE NOT EXISTS (
        SELECT 1 FROM insumo ei 
        WHERE ei.tenant_id = v_tenant_id AND ei.nombre = i.nombre
    );

    -- --------------------------------------------------------------------------
    -- 4. INSERTAR SUB-RECETAS / PREPARACIONES INTERMEDIAS (Idempotente)
    -- --------------------------------------------------------------------------
    -- Las sub-recetas tienen es_sub_receta = true, venta_individual = false, precio = 0.00
    -- y se asignan a la categoría 'Preparaciones'.
    INSERT INTO tenant_menu_product (
        category_id, precio, img_url, nombre, descripcion,
        stock, stock_minimo, unidad, venta_individual,
        auto_availability, es_sub_receta, is_active, created_at, updated_at
    )
    SELECT 
        c.id,
        sub.precio,
        sub.img_url,
        sub.nombre,
        sub.descripcion,
        sub.stock,
        sub.stock_minimo,
        sub.unidad,
        sub.venta_individual,
        sub.auto_availability,
        sub.es_sub_receta,
        sub.is_active,
        NOW(),
        NOW()
    FROM (VALUES

        (0.0::numeric, NULL::varchar, 'Salsa Verde de la Casa'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Salsa Roja Ranchera'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Salsa Amarillita Petra'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Mole Especial Petra'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Frijoles Refritos de la Casa'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Salsa Blanca de Habanero'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Salsa Pomodoro Cremosa'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Cremoso de Aguacate'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Salsa Poblana'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Salsa Holandesa'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Salsa Ponzu Cítrica'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Mole de Flor de Calabaza'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Salsa de Maracuyá y Chipotle'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Salsa de Piña Tatemada y Habanero'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Fondo de Pollo Milpa'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Salsa 3 Quesos'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Ragú de Res Tradicional'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Aderezo César Hecho en Casa'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Vinagreta de Mostaza y Miel'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Vinagreta de Naranja'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Chimichurri de la Casa'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Salsa Roja de Langostinos'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Salsa Bechamel'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Salsa de Mango Picante'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Puré de Guisantes'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Adobo Sarandeado para Pulpo'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Compota de Vino Tinto'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Mousse de Chocolate Bicolor'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar),
        (0.0::numeric, NULL::varchar, 'Praliné de Pistache'::varchar, 'Sub-receta de cocina'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, true::bool, true::bool, 'Preparaciones'::varchar)
    ) AS sub(precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, cat_nombre)
    JOIN tenant_menu_category c ON c.nombre = sub.cat_nombre AND c.tenant_id = v_tenant_id
    WHERE NOT EXISTS (
        SELECT 1 FROM tenant_menu_product ep
        JOIN tenant_menu_category ec ON ep.category_id = ec.id
        WHERE ec.tenant_id = v_tenant_id AND ep.nombre = sub.nombre
    );

    -- --------------------------------------------------------------------------
    -- 5. INSERTAR PLATILLOS Y EXTRAS DEL MENÚ (Idempotente)
    -- --------------------------------------------------------------------------
    INSERT INTO tenant_menu_product (
        category_id, precio, img_url, nombre, descripcion,
        stock, stock_minimo, unidad, venta_individual,
        auto_availability, es_sub_receta, is_active, created_at, updated_at
    )
    SELECT 
        c.id,
        p.precio,
        p.img_url,
        p.nombre,
        p.descripcion,
        p.stock,
        p.stock_minimo,
        p.unidad,
        p.venta_individual,
        p.auto_availability,
        p.es_sub_receta,
        p.is_active,
        NOW(),
        NOW()
    FROM (VALUES

        (155.0::numeric, NULL::varchar, 'COPA DE FRUTOS ROJOS'::varchar, 'Mix de frutos rojos, yogurt, granola y miel. '::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, false::bool, true::bool, 'DESAYUNOS'::varchar),
        (225.0::numeric, NULL::varchar, 'PAN FRANCES'::varchar, 'Pan Brioche de la casa, endulzado y servido con mouse de chocolate blanco y semi amargo'::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, false::bool, true::bool, 'DESAYUNOS'::varchar),
        (35.0::numeric, NULL::varchar, 'PAN ARTESANAL'::varchar, 'Pan del día con el toque casero de Petra. '::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, false::bool, true::bool, 'DESAYUNOS'::varchar),
        (225.0::numeric, NULL::varchar, 'ENCHILADAS PETRA (LAS AMARILLITAS)'::varchar, 'Salsa de pimiento con un toque de chile manzano, acompañadas de queso, crema y cebolla. (100 gr. de Pollo) '::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ENCHILADAS'::varchar),
        (210.0::numeric, NULL::varchar, 'ENCHILADAS VERDES O ROJAS'::varchar, 'Rellenas con 100 gr. de pollo, servidas con queso, crema, cebolla y frijoles refritos. '::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ENCHILADAS'::varchar),
        (215.0::numeric, NULL::varchar, 'ENCHILADAS SUIZAS VERDES'::varchar, 'Rellenas de 100 gr. de pollo, gratinadas con queso, crema, cebolla y frijoles refritos. '::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ENCHILADAS'::varchar),
        (215.0::numeric, NULL::varchar, 'ENMOLADAS PETRA'::varchar, 'Rellenas con 100 gr de pollo, servidas con mole de la casa, queso, crema y aros de cebolla. '::varchar, 0::float8, 0::float8, 'pieza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ENCHILADAS'::varchar),
        (205.0::numeric, NULL::varchar, 'CHILAQUILES VERDES O ROJOS'::varchar, 'Proteína a elegir: huevo 2 pzs, pollo deshebrado, chorizo, cecina, milanesa de pollo. Servidos con queso, crema y cebolla. (100 gr. de proteína).'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'CHILAQUILES'::varchar),
        (225.0::numeric, NULL::varchar, 'ESPECIALES PETRA'::varchar, 'En salsa blanca de Habanero y queso, gratinados, servidos con 100 gr de arrachera, con aguacate, crema y cebolla.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'CHILAQUILES'::varchar),
        (225.0::numeric, NULL::varchar, 'SANDWICH PARMESANO'::varchar, '150 gr de pechuga de pollo crujiente en pan ciabatta, salsa de tomate, queso gratinado, espinaca y papas fritas.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ESPECIALES'::varchar),
        (245.0::numeric, NULL::varchar, 'AVOCADO TOAST'::varchar, 'Cremoso de aguacate servido sobre pan ciabatta, 1 pza de huevo duro, 50 gr de salmón ahumado y reducción de vino tinto.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ESPECIALES'::varchar),
        (215.0::numeric, NULL::varchar, 'GRILL CHEESE'::varchar, 'Pan brioche con mantequilla de hierbas italianas, relleno de 100 gr de tocino y mezcla de quesos, con un shot de crema de tomate.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ESPECIALES'::varchar),
        (210.0::numeric, NULL::varchar, 'OMELETTE MILPA'::varchar, 'Relleno de calabaza, champiñón, granos de elote, rajas poblanas, chilacayote y queso panela. Bañado en salsa poblana.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'HUEVOS'::varchar),
        (230.0::numeric, NULL::varchar, 'OMELETTE SALMON'::varchar, 'Relleno de salmón ahumado, espinaca, requesón, sobre una salsa roja de tomate. Acompañado con mix de lechugas.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'HUEVOS'::varchar),
        (215.0::numeric, NULL::varchar, 'BENEDICTINOS PETRA'::varchar, 'Huevos pochados servidos sobre english muffin, 100 gr de salmón ahumado, bañados en salsa holandesa.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'HUEVOS'::varchar),
        (235.0::numeric, NULL::varchar, 'APORREADO PETRA'::varchar, '100 gr de cecina, 2 pzs de huevo, salsa de su elección, guarnición de frijoles refritos.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'HUEVOS'::varchar),
        (135.0::numeric, NULL::varchar, 'HUEVOS AL GUSTO'::varchar, '(Jamón, chorizo, a la mexicana, tocino), acompañados de chilaquiles y frijoles refritos.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'HUEVOS'::varchar),
        (150.0::numeric, NULL::varchar, 'TOSTADAS DE ATÚN 2 PZS'::varchar, '50 gr de atún laminado en salsa ponzu ligeramente picante, aguacate, chile serrano, pepino y mayonesa.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ENTRADAS'::varchar),
        (275.0::numeric, NULL::varchar, 'TOSTADAS DE PULPO ASADO 3 PZS'::varchar, '150 gr de pulpo asado, aceite de chiles secos, cremoso de aguacate, cebolla encurtida y lechuga.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ENTRADAS'::varchar),
        (210.0::numeric, NULL::varchar, 'CROQUETA DE PLATANO MACHO'::varchar, 'Relleno de filete de res y queso gouda, sobre un mole de flor de calabaza hecho en casa.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ENTRADAS'::varchar),
        (275.0::numeric, NULL::varchar, 'EMPANADAS DE CAMARON Y MARACUYÁ'::varchar, '3 pzs rellenas de camarón, queso manchego, tomate rostizado, con salsa de maracuyá y chipotle.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ENTRADAS'::varchar),
        (295.0::numeric, NULL::varchar, 'QUESO PROVOLONE'::varchar, 'Con mermelada de tomate y pimientos asados, aromatizado con romero y reducción de vino tinto.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ENTRADAS'::varchar),
        (225.0::numeric, NULL::varchar, 'TACOS DE PICANHA 3 PZS'::varchar, 'Con costra de queso, cebolla caramelizada, salsa de piña tatemada y habanero.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ENTRADAS'::varchar),
        (180.0::numeric, NULL::varchar, 'EL APAPACHO'::varchar, 'Cosecha de la milpa con fondo de pollo, acompañada de chile verde, cebolla y tropiezos de panela. (Pollo 50 gr)'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'SOPAS Y PASTAS'::varchar),
        (195.0::numeric, NULL::varchar, 'CREMA DE PISTACHE'::varchar, 'Servida con esferas de queso de cabra y pistache tostado. (180 ml)'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'SOPAS Y PASTAS'::varchar),
        (255.0::numeric, NULL::varchar, 'SOPA DE TORTILLA'::varchar, 'Servida con pulpo y camarón al gratín, julianas de tortilla, aguacate, queso panela, crema, chicharrón y guindillas de chile guajillo.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'SOPAS Y PASTAS'::varchar),
        (215.0::numeric, NULL::varchar, 'PASTA NORDICA'::varchar, 'Feticcini en salsa de 3 quesos y salmón, queso parmesano. (50 gr de Salmón)'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'SOPAS Y PASTAS'::varchar),
        (215.0::numeric, NULL::varchar, 'PASTA TRAVIATA'::varchar, 'Fetuccini al pomodoro cremoso ligeramente picante, servida con punta de filete de Res y queso parmesano. (50 gr de Filete)'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'SOPAS Y PASTAS'::varchar),
        (230.0::numeric, NULL::varchar, 'PAPPARDELLE RELLENO DE RAGÚ'::varchar, 'Pasta fresca rellena de ragú de res (180 gr), servido en salsa blanca de jamón serrano y vino blanco.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'SOPAS Y PASTAS'::varchar),
        (195.0::numeric, NULL::varchar, 'ENSALADA RUBÍ'::varchar, 'Mix de lechugas, fresas, perlas de queso de cabra, arándanos, nuez caramelizada y vinagreta de mostaza.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ENSALADAS'::varchar),
        (195.0::numeric, NULL::varchar, 'ENSALADA PEACH'::varchar, 'Mix de lechugas, durazno, almendra fileteada, queso parmesano y vinagreta de naranja.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ENSALADAS'::varchar),
        (255.0::numeric, NULL::varchar, 'ENSALADA CÉSAR PETRA (POLLO)'::varchar, 'Hojas de lechuga, queso parmesano y aderezo hecho en casa. Con 100 gr de pechuga de pollo.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ENSALADAS'::varchar),
        (270.0::numeric, NULL::varchar, 'ENSALADA CÉSAR PETRA (CAMARÓN)'::varchar, 'Hojas de lechuga, queso parmesano y aderezo hecho en casa. Con 100 gr de camarón.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ENSALADAS'::varchar),
        (270.0::numeric, NULL::varchar, 'ENSALADA CÉSAR PETRA (ARRACHERA)'::varchar, 'Hojas de lechuga, queso parmesano y aderezo hecho en casa. Con 100 gr de arrachera.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'ENSALADAS'::varchar),
        (595.0::numeric, NULL::varchar, 'BIFE PETRA (CHOICE)'::varchar, 'Ribe ye al grill con cremoso de papa, chimichurri y chiles toreados. (250 gr)'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'PLATOS FUERTES'::varchar),
        (410.0::numeric, NULL::varchar, 'FILETE MAR Y TIERRA'::varchar, '170 gr de Filete mignon acompañado de 2 pzs de camarones, en salsa roja de langostinos y micro retoños.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'PLATOS FUERTES'::varchar),
        (385.0::numeric, NULL::varchar, 'PECHUGA MATAMOROS'::varchar, 'Pechuga de pollo rellena de surimi y queso crema, salsa bechamel, salsa de mango picante y ensalada verde.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'PLATOS FUERTES'::varchar),
        (365.0::numeric, NULL::varchar, 'PECHUGA ESPECIAL PETRA'::varchar, 'Rollo de pollo relleno de esquites al mezcal, en salsa de flor de calabaza.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'PLATOS FUERTES'::varchar),
        (400.0::numeric, NULL::varchar, 'SALMÓN SPICY'::varchar, '180 gr de filete de salmón asado, puré de guisantes, manzana asada, salsa cítrica y ensalada verde.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'PLATOS FUERTES'::varchar),
        (415.0::numeric, NULL::varchar, 'PULPO ENDIABLADO'::varchar, '160 gr de tentáculo de pulpo sarandeado y servido con papa cambray al ajillo y cebolla encurtida.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'PLATOS FUERTES'::varchar),
        (295.0::numeric, NULL::varchar, 'MENU INFANTIL'::varchar, 'Pechuga de pollo empanizada, con guarnición de pasta a la crema y papas a la francesa. (100 gr)'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'MENÚ INFANTIL'::varchar),
        (15.0::numeric, NULL::varchar, 'HUEVO EXTRA 1 PZA'::varchar, 'Huevo extra 1 pza.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'EXTRAS'::varchar),
        (50.0::numeric, NULL::varchar, 'CHORIZO EXTRA 100 GR'::varchar, 'Chorizo extra 100 gr.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'EXTRAS'::varchar),
        (75.0::numeric, NULL::varchar, 'ARRACHERA EXTRA 100 GR'::varchar, 'Arrachera extra 100 gr.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'EXTRAS'::varchar),
        (40.0::numeric, NULL::varchar, 'POLLO EXTRA 100 GR'::varchar, 'Pollo extra 100 gr.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'EXTRAS'::varchar),
        (65.0::numeric, NULL::varchar, 'CECINA EXTRA 100 GR'::varchar, 'Cecina extra 100 gr.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'EXTRAS'::varchar),
        (15.0::numeric, NULL::varchar, 'BOLA DE HELADO EXTRA'::varchar, 'Bola de helado extra.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'EXTRAS'::varchar),
        (75.0::numeric, NULL::varchar, 'ARMA TU DESAYUNO COMPLETO'::varchar, 'Café americano o té, fruta de temporada 120 gr, jugo de naranja copa de 4 oz.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'EXTRAS'::varchar),
        (70.0::numeric, NULL::varchar, 'PAPA GAJO 200 GR'::varchar, 'Papa gajo 200 gr.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'EXTRAS'::varchar),
        (85.0::numeric, NULL::varchar, 'PURE DE PAPA 120 GR'::varchar, 'Puré de papa 120 gr.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'EXTRAS'::varchar),
        (80.0::numeric, NULL::varchar, 'MIX DE VEGETALES 120 GR'::varchar, 'Mix de vegetales 120 gr.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'EXTRAS'::varchar),
        (135.0::numeric, NULL::varchar, 'ESPARRAGOS CON JAMON SERRANO 120 GR'::varchar, 'Espárragos con jamón serrano 120 gr.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'EXTRAS'::varchar),
        (55.0::numeric, NULL::varchar, 'PECHUGA DE POLLO 100 GR'::varchar, 'Pechuga de pollo 100 gr.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'EXTRAS'::varchar),
        (65.0::numeric, NULL::varchar, 'CANASTA DE PAN 60 GR'::varchar, 'Canasta de pan 60 gr.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'EXTRAS'::varchar),
        (175.0::numeric, NULL::varchar, 'CHEESE CAKE'::varchar, 'Con compota de vino tinto.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'POSTRES'::varchar),
        (175.0::numeric, NULL::varchar, 'TARTA DE PISTACHE'::varchar, 'Rellena de praliné de pistache, compota y frambuesas frescas.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'POSTRES'::varchar),
        (185.0::numeric, NULL::varchar, 'PETITE FOURS CHOCOLATE'::varchar, 'Base de bizcocho de almendra, crema catalana y mouse de chocolate, sobre galleta sable.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'POSTRES'::varchar),
        (175.0::numeric, NULL::varchar, 'TARTA DE QUESO MASCARPONE'::varchar, 'Tarta de chocolate con queso mascarpone y frutos rojos.'::varchar, 0::float8, 0::float8, 'pza'::varchar, false::bool, true::bool, false::bool, true::bool, 'POSTRES'::varchar)
    ) AS p(precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, cat_nombre)
    JOIN tenant_menu_category c ON c.nombre = p.cat_nombre AND c.tenant_id = v_tenant_id
    WHERE NOT EXISTS (
        SELECT 1 FROM tenant_menu_product ep
        JOIN tenant_menu_category ec ON ep.category_id = ec.id
        WHERE ec.tenant_id = v_tenant_id AND ep.nombre = p.nombre
    );

    -- --------------------------------------------------------------------------
    -- 6. ASIGNACIÓN MULTICATEGORÍA (tenant_menu_product_category)
    -- --------------------------------------------------------------------------
    INSERT INTO tenant_menu_product_category (product_id, category_id)
    SELECT p.id, c.id
    FROM (VALUES

        ('COPA DE FRUTOS ROJOS'::varchar, 'DESAYUNOS'::varchar),
        ('PAN FRANCES'::varchar, 'DESAYUNOS'::varchar),
        ('PAN ARTESANAL'::varchar, 'DESAYUNOS'::varchar),
        ('ENCHILADAS PETRA (LAS AMARILLITAS)'::varchar, 'ENCHILADAS'::varchar),
        ('ENCHILADAS VERDES O ROJAS'::varchar, 'ENCHILADAS'::varchar),
        ('ENCHILADAS SUIZAS VERDES'::varchar, 'ENCHILADAS'::varchar),
        ('ENMOLADAS PETRA'::varchar, 'ENCHILADAS'::varchar),
        ('CHILAQUILES VERDES O ROJOS'::varchar, 'CHILAQUILES'::varchar),
        ('ESPECIALES PETRA'::varchar, 'CHILAQUILES'::varchar),
        ('SANDWICH PARMESANO'::varchar, 'ESPECIALES'::varchar),
        ('AVOCADO TOAST'::varchar, 'ESPECIALES'::varchar),
        ('GRILL CHEESE'::varchar, 'ESPECIALES'::varchar),
        ('OMELETTE MILPA'::varchar, 'HUEVOS'::varchar),
        ('OMELETTE SALMON'::varchar, 'HUEVOS'::varchar),
        ('BENEDICTINOS PETRA'::varchar, 'HUEVOS'::varchar),
        ('APORREADO PETRA'::varchar, 'HUEVOS'::varchar),
        ('HUEVOS AL GUSTO'::varchar, 'HUEVOS'::varchar),
        ('TOSTADAS DE ATÚN 2 PZS'::varchar, 'ENTRADAS'::varchar),
        ('TOSTADAS DE PULPO ASADO 3 PZS'::varchar, 'ENTRADAS'::varchar),
        ('CROQUETA DE PLATANO MACHO'::varchar, 'ENTRADAS'::varchar),
        ('EMPANADAS DE CAMARON Y MARACUYÁ'::varchar, 'ENTRADAS'::varchar),
        ('QUESO PROVOLONE'::varchar, 'ENTRADAS'::varchar),
        ('TACOS DE PICANHA 3 PZS'::varchar, 'ENTRADAS'::varchar),
        ('EL APAPACHO'::varchar, 'SOPAS Y PASTAS'::varchar),
        ('CREMA DE PISTACHE'::varchar, 'SOPAS Y PASTAS'::varchar),
        ('SOPA DE TORTILLA'::varchar, 'SOPAS Y PASTAS'::varchar),
        ('PASTA NORDICA'::varchar, 'SOPAS Y PASTAS'::varchar),
        ('PASTA TRAVIATA'::varchar, 'SOPAS Y PASTAS'::varchar),
        ('PAPPARDELLE RELLENO DE RAGÚ'::varchar, 'SOPAS Y PASTAS'::varchar),
        ('ENSALADA RUBÍ'::varchar, 'ENSALADAS'::varchar),
        ('ENSALADA PEACH'::varchar, 'ENSALADAS'::varchar),
        ('ENSALADA CÉSAR PETRA (POLLO)'::varchar, 'ENSALADAS'::varchar),
        ('ENSALADA CÉSAR PETRA (CAMARÓN)'::varchar, 'ENSALADAS'::varchar),
        ('ENSALADA CÉSAR PETRA (ARRACHERA)'::varchar, 'ENSALADAS'::varchar),
        ('BIFE PETRA (CHOICE)'::varchar, 'PLATOS FUERTES'::varchar),
        ('FILETE MAR Y TIERRA'::varchar, 'PLATOS FUERTES'::varchar),
        ('PECHUGA MATAMOROS'::varchar, 'PLATOS FUERTES'::varchar),
        ('PECHUGA ESPECIAL PETRA'::varchar, 'PLATOS FUERTES'::varchar),
        ('SALMÓN SPICY'::varchar, 'PLATOS FUERTES'::varchar),
        ('PULPO ENDIABLADO'::varchar, 'PLATOS FUERTES'::varchar),
        ('MENU INFANTIL'::varchar, 'MENÚ INFANTIL'::varchar),
        ('HUEVO EXTRA 1 PZA'::varchar, 'EXTRAS'::varchar),
        ('CHORIZO EXTRA 100 GR'::varchar, 'EXTRAS'::varchar),
        ('ARRACHERA EXTRA 100 GR'::varchar, 'EXTRAS'::varchar),
        ('POLLO EXTRA 100 GR'::varchar, 'EXTRAS'::varchar),
        ('CECINA EXTRA 100 GR'::varchar, 'EXTRAS'::varchar),
        ('BOLA DE HELADO EXTRA'::varchar, 'EXTRAS'::varchar),
        ('ARMA TU DESAYUNO COMPLETO'::varchar, 'EXTRAS'::varchar),
        ('PAPA GAJO 200 GR'::varchar, 'EXTRAS'::varchar),
        ('PURE DE PAPA 120 GR'::varchar, 'EXTRAS'::varchar),
        ('MIX DE VEGETALES 120 GR'::varchar, 'EXTRAS'::varchar),
        ('ESPARRAGOS CON JAMON SERRANO 120 GR'::varchar, 'EXTRAS'::varchar),
        ('PECHUGA DE POLLO 100 GR'::varchar, 'EXTRAS'::varchar),
        ('CANASTA DE PAN 60 GR'::varchar, 'EXTRAS'::varchar),
        ('CHEESE CAKE'::varchar, 'POSTRES'::varchar),
        ('TARTA DE PISTACHE'::varchar, 'POSTRES'::varchar),
        ('PETITE FOURS CHOCOLATE'::varchar, 'POSTRES'::varchar),
        ('TARTA DE QUESO MASCARPONE'::varchar, 'POSTRES'::varchar),
        ('Salsa Verde de la Casa'::varchar, 'Preparaciones'::varchar),
        ('Salsa Roja Ranchera'::varchar, 'Preparaciones'::varchar),
        ('Salsa Amarillita Petra'::varchar, 'Preparaciones'::varchar),
        ('Mole Especial Petra'::varchar, 'Preparaciones'::varchar),
        ('Frijoles Refritos de la Casa'::varchar, 'Preparaciones'::varchar),
        ('Salsa Blanca de Habanero'::varchar, 'Preparaciones'::varchar),
        ('Salsa Pomodoro Cremosa'::varchar, 'Preparaciones'::varchar),
        ('Cremoso de Aguacate'::varchar, 'Preparaciones'::varchar),
        ('Salsa Poblana'::varchar, 'Preparaciones'::varchar),
        ('Salsa Holandesa'::varchar, 'Preparaciones'::varchar),
        ('Salsa Ponzu Cítrica'::varchar, 'Preparaciones'::varchar),
        ('Mole de Flor de Calabaza'::varchar, 'Preparaciones'::varchar),
        ('Salsa de Maracuyá y Chipotle'::varchar, 'Preparaciones'::varchar),
        ('Salsa de Piña Tatemada y Habanero'::varchar, 'Preparaciones'::varchar),
        ('Fondo de Pollo Milpa'::varchar, 'Preparaciones'::varchar),
        ('Salsa 3 Quesos'::varchar, 'Preparaciones'::varchar),
        ('Ragú de Res Tradicional'::varchar, 'Preparaciones'::varchar),
        ('Aderezo César Hecho en Casa'::varchar, 'Preparaciones'::varchar),
        ('Vinagreta de Mostaza y Miel'::varchar, 'Preparaciones'::varchar),
        ('Vinagreta de Naranja'::varchar, 'Preparaciones'::varchar),
        ('Chimichurri de la Casa'::varchar, 'Preparaciones'::varchar),
        ('Salsa Roja de Langostinos'::varchar, 'Preparaciones'::varchar),
        ('Salsa Bechamel'::varchar, 'Preparaciones'::varchar),
        ('Salsa de Mango Picante'::varchar, 'Preparaciones'::varchar),
        ('Puré de Guisantes'::varchar, 'Preparaciones'::varchar),
        ('Adobo Sarandeado para Pulpo'::varchar, 'Preparaciones'::varchar),
        ('Compota de Vino Tinto'::varchar, 'Preparaciones'::varchar),
        ('Mousse de Chocolate Bicolor'::varchar, 'Preparaciones'::varchar),
        ('Praliné de Pistache'::varchar, 'Preparaciones'::varchar)
    ) AS pc(product_nombre, cat_nombre)
    JOIN tenant_menu_product p ON p.nombre = pc.product_nombre
        AND p.category_id IN (SELECT id FROM tenant_menu_category WHERE tenant_id = v_tenant_id)
    JOIN tenant_menu_category c ON c.nombre = pc.cat_nombre AND c.tenant_id = v_tenant_id
    WHERE NOT EXISTS (
        SELECT 1 FROM tenant_menu_product_category epc 
        WHERE epc.product_id = p.id AND epc.category_id = c.id
    );

    -- --------------------------------------------------------------------------
    -- 7. INSERTAR RECETAS (product_recipe) - Platillos y Sub-recetas
    -- --------------------------------------------------------------------------
    INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable)
    SELECT 
        p.id,
        i.id,
        r.cantidad,
        r.modificable
    FROM (VALUES

        ('COPA DE FRUTOS ROJOS'::varchar, 'Frutos rojos frescos'::varchar, 100.0::numeric, false::bool),
        ('COPA DE FRUTOS ROJOS'::varchar, 'Granola de la casa'::varchar, 40.0::numeric, false::bool),
        ('COPA DE FRUTOS ROJOS'::varchar, 'Miel de abeja'::varchar, 20.0::numeric, false::bool),
        ('PAN FRANCES'::varchar, 'Pan brioche'::varchar, 1.0::numeric, false::bool),
        ('PAN FRANCES'::varchar, 'Huevo fresco'::varchar, 1.0::numeric, false::bool),
        ('PAN FRANCES'::varchar, 'Leche entera'::varchar, 50.0::numeric, false::bool),
        ('PAN FRANCES'::varchar, 'Mantequilla sin sal'::varchar, 15.0::numeric, false::bool),
        ('PAN FRANCES'::varchar, 'Frutos rojos frescos'::varchar, 30.0::numeric, false::bool),
        ('PAN ARTESANAL'::varchar, 'Pan artesanal'::varchar, 1.0::numeric, false::bool),
        ('PAN ARTESANAL'::varchar, 'Mantequilla sin sal'::varchar, 20.0::numeric, false::bool),
        ('ENCHILADAS PETRA (LAS AMARILLITAS)'::varchar, 'Tortilla de maíz'::varchar, 3.0::numeric, false::bool),
        ('ENCHILADAS PETRA (LAS AMARILLITAS)'::varchar, 'Pechuga de pollo deshebrada'::varchar, 100.0::numeric, false::bool),
        ('ENCHILADAS PETRA (LAS AMARILLITAS)'::varchar, 'Queso fresco de rancho'::varchar, 30.0::numeric, true::bool),
        ('ENCHILADAS PETRA (LAS AMARILLITAS)'::varchar, 'Crema ácida de rancho'::varchar, 30.0::numeric, true::bool),
        ('ENCHILADAS PETRA (LAS AMARILLITAS)'::varchar, 'Cebolla blanca'::varchar, 20.0::numeric, true::bool),
        ('ENCHILADAS VERDES O ROJAS'::varchar, 'Tortilla de maíz'::varchar, 3.0::numeric, false::bool),
        ('ENCHILADAS VERDES O ROJAS'::varchar, 'Pechuga de pollo deshebrada'::varchar, 100.0::numeric, false::bool),
        ('ENCHILADAS VERDES O ROJAS'::varchar, 'Queso fresco de rancho'::varchar, 30.0::numeric, false::bool),
        ('ENCHILADAS VERDES O ROJAS'::varchar, 'Crema ácida de rancho'::varchar, 30.0::numeric, false::bool),
        ('ENCHILADAS VERDES O ROJAS'::varchar, 'Cebolla blanca'::varchar, 20.0::numeric, false::bool),
        ('ENCHILADAS SUIZAS VERDES'::varchar, 'Tortilla de maíz'::varchar, 3.0::numeric, false::bool),
        ('ENCHILADAS SUIZAS VERDES'::varchar, 'Pechuga de pollo deshebrada'::varchar, 100.0::numeric, false::bool),
        ('ENCHILADAS SUIZAS VERDES'::varchar, 'Queso manchego rallado'::varchar, 60.0::numeric, false::bool),
        ('ENCHILADAS SUIZAS VERDES'::varchar, 'Crema ácida de rancho'::varchar, 30.0::numeric, false::bool),
        ('ENCHILADAS SUIZAS VERDES'::varchar, 'Cebolla blanca'::varchar, 20.0::numeric, false::bool),
        ('ENMOLADAS PETRA'::varchar, 'Tortilla de maíz'::varchar, 3.0::numeric, false::bool),
        ('ENMOLADAS PETRA'::varchar, 'Pechuga de pollo deshebrada'::varchar, 100.0::numeric, false::bool),
        ('ENMOLADAS PETRA'::varchar, 'Queso fresco de rancho'::varchar, 30.0::numeric, false::bool),
        ('ENMOLADAS PETRA'::varchar, 'Crema ácida de rancho'::varchar, 30.0::numeric, false::bool),
        ('ENMOLADAS PETRA'::varchar, 'Cebolla morada'::varchar, 20.0::numeric, false::bool),
        ('CHILAQUILES VERDES O ROJOS'::varchar, 'Totopos'::varchar, 120.0::numeric, false::bool),
        ('CHILAQUILES VERDES O ROJOS'::varchar, 'Pechuga de pollo deshebrada'::varchar, 100.0::numeric, false::bool),
        ('CHILAQUILES VERDES O ROJOS'::varchar, 'Queso fresco de rancho'::varchar, 35.0::numeric, false::bool),
        ('CHILAQUILES VERDES O ROJOS'::varchar, 'Crema ácida de rancho'::varchar, 35.0::numeric, false::bool),
        ('CHILAQUILES VERDES O ROJOS'::varchar, 'Cebolla blanca'::varchar, 20.0::numeric, false::bool),
        ('ESPECIALES PETRA'::varchar, 'Totopos'::varchar, 120.0::numeric, false::bool),
        ('ESPECIALES PETRA'::varchar, 'Arrachera marinada'::varchar, 100.0::numeric, false::bool),
        ('ESPECIALES PETRA'::varchar, 'Aguacate hass entero'::varchar, 1.0::numeric, false::bool),
        ('ESPECIALES PETRA'::varchar, 'Queso gouda rallado'::varchar, 50.0::numeric, false::bool),
        ('ESPECIALES PETRA'::varchar, 'Crema ácida de rancho'::varchar, 30.0::numeric, false::bool),
        ('ESPECIALES PETRA'::varchar, 'Cebolla morada'::varchar, 20.0::numeric, false::bool),
        ('SANDWICH PARMESANO'::varchar, 'Pan ciabatta'::varchar, 1.0::numeric, false::bool),
        ('SANDWICH PARMESANO'::varchar, 'Pechuga de pollo fileteada'::varchar, 150.0::numeric, false::bool),
        ('SANDWICH PARMESANO'::varchar, 'Queso parmesano rallado'::varchar, 40.0::numeric, false::bool),
        ('SANDWICH PARMESANO'::varchar, 'Espinaca baby'::varchar, 30.0::numeric, false::bool),
        ('SANDWICH PARMESANO'::varchar, 'Papa gajo congelada'::varchar, 150.0::numeric, false::bool),
        ('SANDWICH PARMESANO'::varchar, 'Aceite vegetal para freír'::varchar, 100.0::numeric, false::bool),
        ('AVOCADO TOAST'::varchar, 'Pan ciabatta'::varchar, 1.0::numeric, false::bool),
        ('AVOCADO TOAST'::varchar, 'Huevo fresco'::varchar, 1.0::numeric, false::bool),
        ('AVOCADO TOAST'::varchar, 'Salmón ahumado laminado'::varchar, 50.0::numeric, false::bool),
        ('AVOCADO TOAST'::varchar, 'Vino tinto de cocina'::varchar, 30.0::numeric, false::bool),
        ('GRILL CHEESE'::varchar, 'Pan brioche'::varchar, 1.0::numeric, false::bool),
        ('GRILL CHEESE'::varchar, 'Tocino ahumado'::varchar, 100.0::numeric, false::bool),
        ('GRILL CHEESE'::varchar, 'Queso gouda rallado'::varchar, 40.0::numeric, false::bool),
        ('GRILL CHEESE'::varchar, 'Queso manchego rallado'::varchar, 40.0::numeric, false::bool),
        ('GRILL CHEESE'::varchar, 'Mantequilla sin sal'::varchar, 20.0::numeric, false::bool),
        ('OMELETTE MILPA'::varchar, 'Huevo fresco'::varchar, 3.0::numeric, false::bool),
        ('OMELETTE MILPA'::varchar, 'Calabaza italiana'::varchar, 40.0::numeric, false::bool),
        ('OMELETTE MILPA'::varchar, 'Champiñones frescos'::varchar, 30.0::numeric, false::bool),
        ('OMELETTE MILPA'::varchar, 'Granos de elote amarillo'::varchar, 30.0::numeric, false::bool),
        ('OMELETTE MILPA'::varchar, 'Chilacayote picado'::varchar, 30.0::numeric, false::bool),
        ('OMELETTE MILPA'::varchar, 'Queso panela en cubos'::varchar, 40.0::numeric, false::bool),
        ('OMELETTE MILPA'::varchar, 'Mantequilla sin sal'::varchar, 15.0::numeric, false::bool),
        ('OMELETTE SALMON'::varchar, 'Huevo fresco'::varchar, 3.0::numeric, false::bool),
        ('OMELETTE SALMON'::varchar, 'Salmón ahumado laminado'::varchar, 60.0::numeric, false::bool),
        ('OMELETTE SALMON'::varchar, 'Espinaca baby'::varchar, 30.0::numeric, false::bool),
        ('OMELETTE SALMON'::varchar, 'Requesón fresco'::varchar, 40.0::numeric, false::bool),
        ('OMELETTE SALMON'::varchar, 'Mix de lechugas orgánicas'::varchar, 40.0::numeric, false::bool),
        ('OMELETTE SALMON'::varchar, 'Mantequilla sin sal'::varchar, 15.0::numeric, false::bool),
        ('BENEDICTINOS PETRA'::varchar, 'English muffin'::varchar, 1.0::numeric, false::bool),
        ('BENEDICTINOS PETRA'::varchar, 'Huevo fresco'::varchar, 2.0::numeric, false::bool),
        ('BENEDICTINOS PETRA'::varchar, 'Salmón ahumado laminado'::varchar, 100.0::numeric, false::bool),
        ('APORREADO PETRA'::varchar, 'Cecina de res'::varchar, 100.0::numeric, false::bool),
        ('APORREADO PETRA'::varchar, 'Huevo fresco'::varchar, 2.0::numeric, false::bool),
        ('APORREADO PETRA'::varchar, 'Aceite vegetal para freír'::varchar, 20.0::numeric, false::bool),
        ('HUEVOS AL GUSTO'::varchar, 'Huevo fresco'::varchar, 2.0::numeric, false::bool),
        ('HUEVOS AL GUSTO'::varchar, 'Jamón de pavo'::varchar, 40.0::numeric, false::bool),
        ('HUEVOS AL GUSTO'::varchar, 'Chorizo de cerdo'::varchar, 40.0::numeric, false::bool),
        ('HUEVOS AL GUSTO'::varchar, 'Totopos'::varchar, 40.0::numeric, false::bool),
        ('TOSTADAS DE ATÚN 2 PZS'::varchar, 'Tortilla de maíz'::varchar, 2.0::numeric, false::bool),
        ('TOSTADAS DE ATÚN 2 PZS'::varchar, 'Atún fresco laminado'::varchar, 50.0::numeric, false::bool),
        ('TOSTADAS DE ATÚN 2 PZS'::varchar, 'Aguacate hass entero'::varchar, 1.0::numeric, false::bool),
        ('TOSTADAS DE ATÚN 2 PZS'::varchar, 'Chile serrano'::varchar, 10.0::numeric, false::bool),
        ('TOSTADAS DE ATÚN 2 PZS'::varchar, 'Pepino fresco'::varchar, 30.0::numeric, false::bool),
        ('TOSTADAS DE ATÚN 2 PZS'::varchar, 'Mayonesa de la casa'::varchar, 25.0::numeric, false::bool),
        ('TOSTADAS DE PULPO ASADO 3 PZS'::varchar, 'Tortilla de maíz'::varchar, 3.0::numeric, false::bool),
        ('TOSTADAS DE PULPO ASADO 3 PZS'::varchar, 'Pulpo cocido'::varchar, 150.0::numeric, false::bool),
        ('TOSTADAS DE PULPO ASADO 3 PZS'::varchar, 'Cebolla morada'::varchar, 30.0::numeric, false::bool),
        ('TOSTADAS DE PULPO ASADO 3 PZS'::varchar, 'Mix de lechugas orgánicas'::varchar, 30.0::numeric, false::bool),
        ('TOSTADAS DE PULPO ASADO 3 PZS'::varchar, 'Aceite de oliva extra virgen'::varchar, 15.0::numeric, false::bool),
        ('CROQUETA DE PLATANO MACHO'::varchar, 'Croqueta de plátano macho cruda'::varchar, 1.0::numeric, false::bool),
        ('CROQUETA DE PLATANO MACHO'::varchar, 'Filete de res'::varchar, 50.0::numeric, false::bool),
        ('CROQUETA DE PLATANO MACHO'::varchar, 'Queso gouda rallado'::varchar, 30.0::numeric, false::bool),
        ('CROQUETA DE PLATANO MACHO'::varchar, 'Aceite vegetal para freír'::varchar, 80.0::numeric, false::bool),
        ('EMPANADAS DE CAMARON Y MARACUYÁ'::varchar, 'Empanada de camarón y maracuyá cruda'::varchar, 3.0::numeric, false::bool),
        ('EMPANADAS DE CAMARON Y MARACUYÁ'::varchar, 'Camarón pacotilla'::varchar, 80.0::numeric, false::bool),
        ('EMPANADAS DE CAMARON Y MARACUYÁ'::varchar, 'Queso manchego rallado'::varchar, 40.0::numeric, false::bool),
        ('EMPANADAS DE CAMARON Y MARACUYÁ'::varchar, 'Jitomate guaje'::varchar, 40.0::numeric, false::bool),
        ('EMPANADAS DE CAMARON Y MARACUYÁ'::varchar, 'Aceite vegetal para freír'::varchar, 100.0::numeric, false::bool),
        ('QUESO PROVOLONE'::varchar, 'Rueda de queso provolone'::varchar, 1.0::numeric, false::bool),
        ('QUESO PROVOLONE'::varchar, 'Pimiento amarillo'::varchar, 50.0::numeric, false::bool),
        ('QUESO PROVOLONE'::varchar, 'Jitomate guaje'::varchar, 50.0::numeric, false::bool),
        ('QUESO PROVOLONE'::varchar, 'Vino tinto de cocina'::varchar, 30.0::numeric, false::bool),
        ('QUESO PROVOLONE'::varchar, 'Aceite de oliva extra virgen'::varchar, 15.0::numeric, false::bool),
        ('TACOS DE PICANHA 3 PZS'::varchar, 'Tortilla de maíz'::varchar, 3.0::numeric, false::bool),
        ('TACOS DE PICANHA 3 PZS'::varchar, 'Picanha de res'::varchar, 150.0::numeric, false::bool),
        ('TACOS DE PICANHA 3 PZS'::varchar, 'Queso gouda rallado'::varchar, 60.0::numeric, false::bool),
        ('TACOS DE PICANHA 3 PZS'::varchar, 'Cebolla blanca'::varchar, 40.0::numeric, false::bool),
        ('EL APAPACHO'::varchar, 'Pechuga de pollo deshebrada'::varchar, 50.0::numeric, false::bool),
        ('EL APAPACHO'::varchar, 'Queso panela en cubos'::varchar, 40.0::numeric, false::bool),
        ('EL APAPACHO'::varchar, 'Chile serrano'::varchar, 15.0::numeric, false::bool),
        ('EL APAPACHO'::varchar, 'Cebolla blanca'::varchar, 20.0::numeric, false::bool),
        ('EL APAPACHO'::varchar, 'Granos de elote amarillo'::varchar, 30.0::numeric, false::bool),
        ('CREMA DE PISTACHE'::varchar, 'Pistache pelado'::varchar, 40.0::numeric, false::bool),
        ('CREMA DE PISTACHE'::varchar, 'Queso de cabra'::varchar, 40.0::numeric, false::bool),
        ('CREMA DE PISTACHE'::varchar, 'Crema para batir'::varchar, 120.0::numeric, false::bool),
        ('CREMA DE PISTACHE'::varchar, 'Leche entera'::varchar, 60.0::numeric, false::bool),
        ('CREMA DE PISTACHE'::varchar, 'Mantequilla sin sal'::varchar, 15.0::numeric, false::bool),
        ('SOPA DE TORTILLA'::varchar, 'Totopos'::varchar, 60.0::numeric, false::bool),
        ('SOPA DE TORTILLA'::varchar, 'Pulpo cocido'::varchar, 40.0::numeric, false::bool),
        ('SOPA DE TORTILLA'::varchar, 'Camarón pacotilla'::varchar, 40.0::numeric, false::bool),
        ('SOPA DE TORTILLA'::varchar, 'Queso panela en cubos'::varchar, 40.0::numeric, false::bool),
        ('SOPA DE TORTILLA'::varchar, 'Aguacate hass entero'::varchar, 1.0::numeric, false::bool),
        ('SOPA DE TORTILLA'::varchar, 'Crema ácida de rancho'::varchar, 30.0::numeric, false::bool),
        ('SOPA DE TORTILLA'::varchar, 'Chicharrón de cerdo crujiente'::varchar, 30.0::numeric, false::bool),
        ('SOPA DE TORTILLA'::varchar, 'Chile guajillo seco'::varchar, 10.0::numeric, false::bool),
        ('PASTA NORDICA'::varchar, 'Pasta fettuccini'::varchar, 120.0::numeric, false::bool),
        ('PASTA NORDICA'::varchar, 'Filete de salmón fresco'::varchar, 50.0::numeric, false::bool),
        ('PASTA NORDICA'::varchar, 'Queso parmesano rallado'::varchar, 25.0::numeric, false::bool),
        ('PASTA TRAVIATA'::varchar, 'Pasta fettuccini'::varchar, 120.0::numeric, false::bool),
        ('PASTA TRAVIATA'::varchar, 'Filete de res'::varchar, 50.0::numeric, false::bool),
        ('PASTA TRAVIATA'::varchar, 'Queso parmesano rallado'::varchar, 25.0::numeric, false::bool),
        ('PAPPARDELLE RELLENO DE RAGÚ'::varchar, 'Pasta pappardelle'::varchar, 180.0::numeric, false::bool),
        ('PAPPARDELLE RELLENO DE RAGÚ'::varchar, 'Jamón serrano'::varchar, 30.0::numeric, false::bool),
        ('PAPPARDELLE RELLENO DE RAGÚ'::varchar, 'Crema para batir'::varchar, 60.0::numeric, false::bool),
        ('PAPPARDELLE RELLENO DE RAGÚ'::varchar, 'Vino blanco de cocina'::varchar, 30.0::numeric, false::bool),
        ('ENSALADA RUBÍ'::varchar, 'Mix de lechugas orgánicas'::varchar, 100.0::numeric, false::bool),
        ('ENSALADA RUBÍ'::varchar, 'Fresas frescas'::varchar, 50.0::numeric, false::bool),
        ('ENSALADA RUBÍ'::varchar, 'Queso de cabra'::varchar, 40.0::numeric, false::bool),
        ('ENSALADA RUBÍ'::varchar, 'Arándanos deshidratados'::varchar, 25.0::numeric, false::bool),
        ('ENSALADA RUBÍ'::varchar, 'Nuez caramelizada'::varchar, 30.0::numeric, false::bool),
        ('ENSALADA PEACH'::varchar, 'Mix de lechugas orgánicas'::varchar, 100.0::numeric, false::bool),
        ('ENSALADA PEACH'::varchar, 'Durazno fresco'::varchar, 60.0::numeric, false::bool),
        ('ENSALADA PEACH'::varchar, 'Almendra fileteada'::varchar, 30.0::numeric, false::bool),
        ('ENSALADA PEACH'::varchar, 'Queso parmesano rallado'::varchar, 30.0::numeric, false::bool),
        ('ENSALADA CÉSAR PETRA (POLLO)'::varchar, 'Lechuga orejona'::varchar, 120.0::numeric, false::bool),
        ('ENSALADA CÉSAR PETRA (POLLO)'::varchar, 'Pechuga de pollo fileteada'::varchar, 100.0::numeric, false::bool),
        ('ENSALADA CÉSAR PETRA (POLLO)'::varchar, 'Queso parmesano rallado'::varchar, 35.0::numeric, false::bool),
        ('ENSALADA CÉSAR PETRA (POLLO)'::varchar, 'Pan artesanal'::varchar, 1.0::numeric, false::bool),
        ('ENSALADA CÉSAR PETRA (CAMARÓN)'::varchar, 'Lechuga orejona'::varchar, 120.0::numeric, false::bool),
        ('ENSALADA CÉSAR PETRA (CAMARÓN)'::varchar, 'Camarón pacotilla'::varchar, 100.0::numeric, false::bool),
        ('ENSALADA CÉSAR PETRA (CAMARÓN)'::varchar, 'Queso parmesano rallado'::varchar, 35.0::numeric, false::bool),
        ('ENSALADA CÉSAR PETRA (CAMARÓN)'::varchar, 'Pan artesanal'::varchar, 1.0::numeric, false::bool),
        ('ENSALADA CÉSAR PETRA (ARRACHERA)'::varchar, 'Lechuga orejona'::varchar, 120.0::numeric, false::bool),
        ('ENSALADA CÉSAR PETRA (ARRACHERA)'::varchar, 'Arrachera marinada'::varchar, 100.0::numeric, false::bool),
        ('ENSALADA CÉSAR PETRA (ARRACHERA)'::varchar, 'Queso parmesano rallado'::varchar, 35.0::numeric, false::bool),
        ('ENSALADA CÉSAR PETRA (ARRACHERA)'::varchar, 'Pan artesanal'::varchar, 1.0::numeric, false::bool),
        ('BIFE PETRA (CHOICE)'::varchar, 'Rib eye choice'::varchar, 250.0::numeric, false::bool),
        ('BIFE PETRA (CHOICE)'::varchar, 'Hojuelas de puré de papa'::varchar, 60.0::numeric, false::bool),
        ('BIFE PETRA (CHOICE)'::varchar, 'Mantequilla sin sal'::varchar, 25.0::numeric, false::bool),
        ('BIFE PETRA (CHOICE)'::varchar, 'Leche entera'::varchar, 40.0::numeric, false::bool),
        ('BIFE PETRA (CHOICE)'::varchar, 'Chile serrano'::varchar, 20.0::numeric, false::bool),
        ('FILETE MAR Y TIERRA'::varchar, 'Filete de res'::varchar, 170.0::numeric, false::bool),
        ('FILETE MAR Y TIERRA'::varchar, 'Camarón gigante U15'::varchar, 2.0::numeric, false::bool),
        ('FILETE MAR Y TIERRA'::varchar, 'Mantequilla sin sal'::varchar, 20.0::numeric, false::bool),
        ('PECHUGA MATAMOROS'::varchar, 'Pechuga de pollo fileteada'::varchar, 180.0::numeric, false::bool),
        ('PECHUGA MATAMOROS'::varchar, 'Surimi'::varchar, 40.0::numeric, false::bool),
        ('PECHUGA MATAMOROS'::varchar, 'Queso crema'::varchar, 40.0::numeric, false::bool),
        ('PECHUGA MATAMOROS'::varchar, 'Mix de lechugas orgánicas'::varchar, 40.0::numeric, false::bool),
        ('PECHUGA ESPECIAL PETRA'::varchar, 'Rollo de pechuga especial crudo'::varchar, 1.0::numeric, false::bool),
        ('PECHUGA ESPECIAL PETRA'::varchar, 'Pechuga de pollo fileteada'::varchar, 180.0::numeric, false::bool),
        ('PECHUGA ESPECIAL PETRA'::varchar, 'Esquites al mezcal'::varchar, 80.0::numeric, false::bool),
        ('SALMÓN SPICY'::varchar, 'Filete de salmón fresco'::varchar, 180.0::numeric, false::bool),
        ('SALMÓN SPICY'::varchar, 'Manzana verde'::varchar, 50.0::numeric, false::bool),
        ('SALMÓN SPICY'::varchar, 'Mix de lechugas orgánicas'::varchar, 40.0::numeric, false::bool),
        ('SALMÓN SPICY'::varchar, 'Aceite de oliva extra virgen'::varchar, 15.0::numeric, false::bool),
        ('PULPO ENDIABLADO'::varchar, 'Pulpo cocido'::varchar, 160.0::numeric, false::bool),
        ('PULPO ENDIABLADO'::varchar, 'Papa cambray'::varchar, 100.0::numeric, false::bool),
        ('PULPO ENDIABLADO'::varchar, 'Ajo pelado'::varchar, 15.0::numeric, false::bool),
        ('PULPO ENDIABLADO'::varchar, 'Cebolla morada'::varchar, 30.0::numeric, false::bool),
        ('PULPO ENDIABLADO'::varchar, 'Aceite de oliva extra virgen'::varchar, 25.0::numeric, false::bool),
        ('MENU INFANTIL'::varchar, 'Pechuga de pollo fileteada'::varchar, 100.0::numeric, false::bool),
        ('MENU INFANTIL'::varchar, 'Harina de trigo'::varchar, 30.0::numeric, false::bool),
        ('MENU INFANTIL'::varchar, 'Huevo fresco'::varchar, 1.0::numeric, false::bool),
        ('MENU INFANTIL'::varchar, 'Papa gajo congelada'::varchar, 100.0::numeric, false::bool),
        ('MENU INFANTIL'::varchar, 'Pasta fettuccini'::varchar, 60.0::numeric, false::bool),
        ('MENU INFANTIL'::varchar, 'Crema para batir'::varchar, 30.0::numeric, false::bool),
        ('MENU INFANTIL'::varchar, 'Mantequilla sin sal'::varchar, 10.0::numeric, false::bool),
        ('HUEVO EXTRA 1 PZA'::varchar, 'Huevo fresco'::varchar, 1.0::numeric, false::bool),
        ('CHORIZO EXTRA 100 GR'::varchar, 'Chorizo de cerdo'::varchar, 100.0::numeric, false::bool),
        ('ARRACHERA EXTRA 100 GR'::varchar, 'Arrachera marinada'::varchar, 100.0::numeric, false::bool),
        ('POLLO EXTRA 100 GR'::varchar, 'Pechuga de pollo deshebrada'::varchar, 100.0::numeric, false::bool),
        ('CECINA EXTRA 100 GR'::varchar, 'Cecina de res'::varchar, 100.0::numeric, false::bool),
        ('BOLA DE HELADO EXTRA'::varchar, 'Helado de vainilla'::varchar, 80.0::numeric, false::bool),
        ('ARMA TU DESAYUNO COMPLETO'::varchar, 'Café americano preparado'::varchar, 180.0::numeric, false::bool),
        ('ARMA TU DESAYUNO COMPLETO'::varchar, 'Fruta picada de temporada'::varchar, 120.0::numeric, false::bool),
        ('ARMA TU DESAYUNO COMPLETO'::varchar, 'Jugo de naranja natural'::varchar, 120.0::numeric, false::bool),
        ('PAPA GAJO 200 GR'::varchar, 'Papa gajo congelada'::varchar, 200.0::numeric, false::bool),
        ('PAPA GAJO 200 GR'::varchar, 'Aceite vegetal para freír'::varchar, 50.0::numeric, false::bool),
        ('PURE DE PAPA 120 GR'::varchar, 'Hojuelas de puré de papa'::varchar, 60.0::numeric, false::bool),
        ('PURE DE PAPA 120 GR'::varchar, 'Leche entera'::varchar, 40.0::numeric, false::bool),
        ('PURE DE PAPA 120 GR'::varchar, 'Mantequilla sin sal'::varchar, 20.0::numeric, false::bool),
        ('MIX DE VEGETALES 120 GR'::varchar, 'Mix de vegetales'::varchar, 120.0::numeric, false::bool),
        ('MIX DE VEGETALES 120 GR'::varchar, 'Mantequilla sin sal'::varchar, 15.0::numeric, false::bool),
        ('ESPARRAGOS CON JAMON SERRANO 120 GR'::varchar, 'Espárragos frescos'::varchar, 90.0::numeric, false::bool),
        ('ESPARRAGOS CON JAMON SERRANO 120 GR'::varchar, 'Jamón serrano'::varchar, 30.0::numeric, false::bool),
        ('ESPARRAGOS CON JAMON SERRANO 120 GR'::varchar, 'Aceite de oliva extra virgen'::varchar, 10.0::numeric, false::bool),
        ('PECHUGA DE POLLO 100 GR'::varchar, 'Pechuga de pollo fileteada'::varchar, 100.0::numeric, false::bool),
        ('CANASTA DE PAN 60 GR'::varchar, 'Canasta de pan'::varchar, 1.0::numeric, false::bool),
        ('CHEESE CAKE'::varchar, 'Queso crema'::varchar, 80.0::numeric, false::bool),
        ('CHEESE CAKE'::varchar, 'Crema para batir'::varchar, 40.0::numeric, false::bool),
        ('CHEESE CAKE'::varchar, 'Azúcar estándar'::varchar, 30.0::numeric, false::bool),
        ('CHEESE CAKE'::varchar, 'Base de galleta sable'::varchar, 1.0::numeric, false::bool),
        ('TARTA DE PISTACHE'::varchar, 'Base de galleta sable'::varchar, 1.0::numeric, false::bool),
        ('TARTA DE PISTACHE'::varchar, 'Frambuesas frescas'::varchar, 40.0::numeric, false::bool),
        ('PETITE FOURS CHOCOLATE'::varchar, 'Base de galleta sable'::varchar, 1.0::numeric, false::bool),
        ('PETITE FOURS CHOCOLATE'::varchar, 'Almendra fileteada'::varchar, 20.0::numeric, false::bool),
        ('PETITE FOURS CHOCOLATE'::varchar, 'Azúcar estándar'::varchar, 20.0::numeric, false::bool),
        ('TARTA DE QUESO MASCARPONE'::varchar, 'Queso mascarpone'::varchar, 90.0::numeric, false::bool),
        ('TARTA DE QUESO MASCARPONE'::varchar, 'Chocolate semi-amargo'::varchar, 40.0::numeric, false::bool),
        ('TARTA DE QUESO MASCARPONE'::varchar, 'Frutos rojos frescos'::varchar, 40.0::numeric, false::bool),
        ('TARTA DE QUESO MASCARPONE'::varchar, 'Base de galleta sable'::varchar, 1.0::numeric, false::bool),
        ('Salsa Verde de la Casa'::varchar, 'Tomate verde'::varchar, 120.0::numeric, false::bool),
        ('Salsa Verde de la Casa'::varchar, 'Chile serrano'::varchar, 20.0::numeric, false::bool),
        ('Salsa Verde de la Casa'::varchar, 'Cebolla blanca'::varchar, 30.0::numeric, false::bool),
        ('Salsa Verde de la Casa'::varchar, 'Cilantro fresco'::varchar, 15.0::numeric, false::bool),
        ('Salsa Verde de la Casa'::varchar, 'Ajo pelado'::varchar, 5.0::numeric, false::bool),
        ('Salsa Roja Ranchera'::varchar, 'Jitomate guaje'::varchar, 120.0::numeric, false::bool),
        ('Salsa Roja Ranchera'::varchar, 'Chile guajillo seco'::varchar, 15.0::numeric, false::bool),
        ('Salsa Roja Ranchera'::varchar, 'Cebolla blanca'::varchar, 25.0::numeric, false::bool),
        ('Salsa Roja Ranchera'::varchar, 'Ajo pelado'::varchar, 5.0::numeric, false::bool),
        ('Salsa Amarillita Petra'::varchar, 'Pimiento amarillo'::varchar, 80.0::numeric, false::bool),
        ('Salsa Amarillita Petra'::varchar, 'Chile manzano'::varchar, 20.0::numeric, false::bool),
        ('Salsa Amarillita Petra'::varchar, 'Crema ácida de rancho'::varchar, 50.0::numeric, false::bool),
        ('Salsa Amarillita Petra'::varchar, 'Cebolla blanca'::varchar, 20.0::numeric, false::bool),
        ('Mole Especial Petra'::varchar, 'Pasta de mole tradicional'::varchar, 80.0::numeric, false::bool),
        ('Mole Especial Petra'::varchar, 'Caldo de pollo concentrado'::varchar, 60.0::numeric, false::bool),
        ('Mole Especial Petra'::varchar, 'Chocolate semi-amargo'::varchar, 15.0::numeric, false::bool),
        ('Frijoles Refritos de la Casa'::varchar, 'Frijol negro en grano'::varchar, 80.0::numeric, false::bool),
        ('Frijoles Refritos de la Casa'::varchar, 'Manteca de cerdo'::varchar, 15.0::numeric, false::bool),
        ('Frijoles Refritos de la Casa'::varchar, 'Cebolla blanca'::varchar, 10.0::numeric, false::bool),
        ('Frijoles Refritos de la Casa'::varchar, 'Epazote fresco'::varchar, 5.0::numeric, false::bool),
        ('Salsa Blanca de Habanero'::varchar, 'Crema ácida de rancho'::varchar, 60.0::numeric, false::bool),
        ('Salsa Blanca de Habanero'::varchar, 'Chile habanero'::varchar, 10.0::numeric, false::bool),
        ('Salsa Blanca de Habanero'::varchar, 'Queso crema'::varchar, 30.0::numeric, false::bool),
        ('Salsa Blanca de Habanero'::varchar, 'Mantequilla sin sal'::varchar, 15.0::numeric, false::bool),
        ('Salsa Pomodoro Cremosa'::varchar, 'Jitomate guaje'::varchar, 100.0::numeric, false::bool),
        ('Salsa Pomodoro Cremosa'::varchar, 'Crema para batir'::varchar, 30.0::numeric, false::bool),
        ('Salsa Pomodoro Cremosa'::varchar, 'Albahaca fresca'::varchar, 10.0::numeric, false::bool),
        ('Salsa Pomodoro Cremosa'::varchar, 'Ajo pelado'::varchar, 5.0::numeric, false::bool),
        ('Salsa Pomodoro Cremosa'::varchar, 'Aceite de oliva extra virgen'::varchar, 10.0::numeric, false::bool),
        ('Cremoso de Aguacate'::varchar, 'Aguacate hass entero'::varchar, 1.0::numeric, false::bool),
        ('Cremoso de Aguacate'::varchar, 'Crema ácida de rancho'::varchar, 30.0::numeric, false::bool),
        ('Cremoso de Aguacate'::varchar, 'Jugo de limón natural'::varchar, 10.0::numeric, false::bool),
        ('Cremoso de Aguacate'::varchar, 'Cilantro fresco'::varchar, 5.0::numeric, false::bool),
        ('Salsa Poblana'::varchar, 'Chile poblano'::varchar, 60.0::numeric, false::bool),
        ('Salsa Poblana'::varchar, 'Crema ácida de rancho'::varchar, 40.0::numeric, false::bool),
        ('Salsa Poblana'::varchar, 'Caldo de pollo concentrado'::varchar, 30.0::numeric, false::bool),
        ('Salsa Poblana'::varchar, 'Mantequilla sin sal'::varchar, 10.0::numeric, false::bool),
        ('Salsa Holandesa'::varchar, 'Huevo fresco'::varchar, 2.0::numeric, false::bool),
        ('Salsa Holandesa'::varchar, 'Mantequilla sin sal'::varchar, 40.0::numeric, false::bool),
        ('Salsa Holandesa'::varchar, 'Jugo de limón natural'::varchar, 10.0::numeric, false::bool),
        ('Salsa Ponzu Cítrica'::varchar, 'Salsa de soya'::varchar, 20.0::numeric, false::bool),
        ('Salsa Ponzu Cítrica'::varchar, 'Jugo de limón natural'::varchar, 15.0::numeric, false::bool),
        ('Salsa Ponzu Cítrica'::varchar, 'Jugo de naranja natural'::varchar, 15.0::numeric, false::bool),
        ('Mole de Flor de Calabaza'::varchar, 'Flor de calabaza'::varchar, 50.0::numeric, false::bool),
        ('Mole de Flor de Calabaza'::varchar, 'Caldo de pollo concentrado'::varchar, 50.0::numeric, false::bool),
        ('Mole de Flor de Calabaza'::varchar, 'Cebolla blanca'::varchar, 20.0::numeric, false::bool),
        ('Mole de Flor de Calabaza'::varchar, 'Chile poblano'::varchar, 15.0::numeric, false::bool),
        ('Salsa de Maracuyá y Chipotle'::varchar, 'Pulpa de maracuyá'::varchar, 40.0::numeric, false::bool),
        ('Salsa de Maracuyá y Chipotle'::varchar, 'Chile habanero'::varchar, 10.0::numeric, false::bool),
        ('Salsa de Maracuyá y Chipotle'::varchar, 'Miel de abeja'::varchar, 15.0::numeric, false::bool),
        ('Salsa de Maracuyá y Chipotle'::varchar, 'Mantequilla sin sal'::varchar, 10.0::numeric, false::bool),
        ('Salsa de Piña Tatemada y Habanero'::varchar, 'Chile habanero'::varchar, 10.0::numeric, false::bool),
        ('Salsa de Piña Tatemada y Habanero'::varchar, 'Jugo de naranja natural'::varchar, 20.0::numeric, false::bool),
        ('Salsa de Piña Tatemada y Habanero'::varchar, 'Cebolla morada'::varchar, 15.0::numeric, false::bool),
        ('Salsa de Piña Tatemada y Habanero'::varchar, 'Miel de abeja'::varchar, 10.0::numeric, false::bool),
        ('Fondo de Pollo Milpa'::varchar, 'Caldo de pollo concentrado'::varchar, 250.0::numeric, false::bool),
        ('Fondo de Pollo Milpa'::varchar, 'Cebolla blanca'::varchar, 20.0::numeric, false::bool),
        ('Fondo de Pollo Milpa'::varchar, 'Cilantro fresco'::varchar, 10.0::numeric, false::bool),
        ('Salsa 3 Quesos'::varchar, 'Crema para batir'::varchar, 80.0::numeric, false::bool),
        ('Salsa 3 Quesos'::varchar, 'Queso gouda rallado'::varchar, 30.0::numeric, false::bool),
        ('Salsa 3 Quesos'::varchar, 'Queso manchego rallado'::varchar, 30.0::numeric, false::bool),
        ('Salsa 3 Quesos'::varchar, 'Queso parmesano rallado'::varchar, 20.0::numeric, false::bool),
        ('Salsa 3 Quesos'::varchar, 'Vino blanco de cocina'::varchar, 20.0::numeric, false::bool),
        ('Ragú de Res Tradicional'::varchar, 'Filete de res'::varchar, 120.0::numeric, false::bool),
        ('Ragú de Res Tradicional'::varchar, 'Jitomate guaje'::varchar, 50.0::numeric, false::bool),
        ('Ragú de Res Tradicional'::varchar, 'Cebolla blanca'::varchar, 20.0::numeric, false::bool),
        ('Ragú de Res Tradicional'::varchar, 'Vino tinto de cocina'::varchar, 20.0::numeric, false::bool),
        ('Aderezo César Hecho en Casa'::varchar, 'Mayonesa de la casa'::varchar, 40.0::numeric, false::bool),
        ('Aderezo César Hecho en Casa'::varchar, 'Mostaza dijon'::varchar, 10.0::numeric, false::bool),
        ('Aderezo César Hecho en Casa'::varchar, 'Queso parmesano rallado'::varchar, 15.0::numeric, false::bool),
        ('Aderezo César Hecho en Casa'::varchar, 'Jugo de limón natural'::varchar, 10.0::numeric, false::bool),
        ('Aderezo César Hecho en Casa'::varchar, 'Ajo pelado'::varchar, 5.0::numeric, false::bool),
        ('Vinagreta de Mostaza y Miel'::varchar, 'Mostaza dijon'::varchar, 20.0::numeric, false::bool),
        ('Vinagreta de Mostaza y Miel'::varchar, 'Miel de abeja'::varchar, 15.0::numeric, false::bool),
        ('Vinagreta de Mostaza y Miel'::varchar, 'Vinagre de manzana'::varchar, 10.0::numeric, false::bool),
        ('Vinagreta de Mostaza y Miel'::varchar, 'Aceite de oliva extra virgen'::varchar, 25.0::numeric, false::bool),
        ('Vinagreta de Naranja'::varchar, 'Jugo de naranja natural'::varchar, 30.0::numeric, false::bool),
        ('Vinagreta de Naranja'::varchar, 'Aceite de oliva extra virgen'::varchar, 20.0::numeric, false::bool),
        ('Vinagreta de Naranja'::varchar, 'Miel de abeja'::varchar, 10.0::numeric, false::bool),
        ('Vinagreta de Naranja'::varchar, 'Vinagre de manzana'::varchar, 10.0::numeric, false::bool),
        ('Chimichurri de la Casa'::varchar, 'Perejil fresco'::varchar, 20.0::numeric, false::bool),
        ('Chimichurri de la Casa'::varchar, 'Ajo pelado'::varchar, 10.0::numeric, false::bool),
        ('Chimichurri de la Casa'::varchar, 'Aceite de oliva extra virgen'::varchar, 35.0::numeric, false::bool),
        ('Chimichurri de la Casa'::varchar, 'Vinagre de manzana'::varchar, 15.0::numeric, false::bool),
        ('Salsa Roja de Langostinos'::varchar, 'Caldo de pollo concentrado'::varchar, 80.0::numeric, false::bool),
        ('Salsa Roja de Langostinos'::varchar, 'Jitomate guaje'::varchar, 40.0::numeric, false::bool),
        ('Salsa Roja de Langostinos'::varchar, 'Crema para batir'::varchar, 30.0::numeric, false::bool),
        ('Salsa Roja de Langostinos'::varchar, 'Vino blanco de cocina'::varchar, 20.0::numeric, false::bool),
        ('Salsa Bechamel'::varchar, 'Mantequilla sin sal'::varchar, 20.0::numeric, false::bool),
        ('Salsa Bechamel'::varchar, 'Harina de trigo'::varchar, 20.0::numeric, false::bool),
        ('Salsa Bechamel'::varchar, 'Leche entera'::varchar, 80.0::numeric, false::bool),
        ('Salsa de Mango Picante'::varchar, 'Pulpa de mango'::varchar, 50.0::numeric, false::bool),
        ('Salsa de Mango Picante'::varchar, 'Chile habanero'::varchar, 10.0::numeric, false::bool),
        ('Salsa de Mango Picante'::varchar, 'Jugo de limón natural'::varchar, 10.0::numeric, false::bool),
        ('Puré de Guisantes'::varchar, 'Mix de vegetales'::varchar, 80.0::numeric, false::bool),
        ('Puré de Guisantes'::varchar, 'Mantequilla sin sal'::varchar, 20.0::numeric, false::bool),
        ('Puré de Guisantes'::varchar, 'Crema ácida de rancho'::varchar, 20.0::numeric, false::bool),
        ('Adobo Sarandeado para Pulpo'::varchar, 'Chile guajillo seco'::varchar, 30.0::numeric, false::bool),
        ('Adobo Sarandeado para Pulpo'::varchar, 'Achiote en pasta'::varchar, 20.0::numeric, false::bool),
        ('Adobo Sarandeado para Pulpo'::varchar, 'Mayonesa de la casa'::varchar, 30.0::numeric, false::bool),
        ('Adobo Sarandeado para Pulpo'::varchar, 'Jugo de limón natural'::varchar, 15.0::numeric, false::bool),
        ('Compota de Vino Tinto'::varchar, 'Vino tinto de cocina'::varchar, 50.0::numeric, false::bool),
        ('Compota de Vino Tinto'::varchar, 'Frutos rojos frescos'::varchar, 40.0::numeric, false::bool),
        ('Compota de Vino Tinto'::varchar, 'Azúcar estándar'::varchar, 20.0::numeric, false::bool),
        ('Mousse de Chocolate Bicolor'::varchar, 'Chocolate semi-amargo'::varchar, 30.0::numeric, false::bool),
        ('Mousse de Chocolate Bicolor'::varchar, 'Chocolate blanco'::varchar, 30.0::numeric, false::bool),
        ('Mousse de Chocolate Bicolor'::varchar, 'Crema para batir'::varchar, 40.0::numeric, false::bool),
        ('Praliné de Pistache'::varchar, 'Pistache pelado'::varchar, 50.0::numeric, false::bool),
        ('Praliné de Pistache'::varchar, 'Azúcar estándar'::varchar, 30.0::numeric, false::bool),
        ('Praliné de Pistache'::varchar, 'Crema para batir'::varchar, 30.0::numeric, false::bool)
    ) AS r(dish_nombre, insumo_nombre, cantidad, modificable)
    JOIN tenant_menu_product p ON p.nombre = r.dish_nombre
        AND p.category_id IN (SELECT id FROM tenant_menu_category WHERE tenant_id = v_tenant_id)
    JOIN insumo i ON i.nombre = r.insumo_nombre AND i.tenant_id = v_tenant_id
    WHERE NOT EXISTS (
        SELECT 1 FROM product_recipe er
        WHERE er.dish_product_id = p.id AND er.insumo_id = i.id
    );

    -- --------------------------------------------------------------------------
    -- 8. ASIGNAR SUB-RECETAS A PLATILLOS (product_sub_receta)
    -- --------------------------------------------------------------------------
    INSERT INTO product_sub_receta (dish_product_id, sub_receta_id, modificable, precio)
    SELECT 
        dp.id,
        sr.id,
        s.modificable,
        s.precio
    FROM (VALUES

        ('PAN FRANCES'::varchar, 'Mousse de Chocolate Bicolor'::varchar, false::bool, NULL::numeric),
        ('ENCHILADAS PETRA (LAS AMARILLITAS)'::varchar, 'Salsa Amarillita Petra'::varchar, false::bool, NULL::numeric),
        ('ENCHILADAS PETRA (LAS AMARILLITAS)'::varchar, 'Frijoles Refritos de la Casa'::varchar, false::bool, NULL::numeric),
        ('ENCHILADAS VERDES O ROJAS'::varchar, 'Salsa Verde de la Casa'::varchar, false::bool, NULL::numeric),
        ('ENCHILADAS VERDES O ROJAS'::varchar, 'Frijoles Refritos de la Casa'::varchar, false::bool, NULL::numeric),
        ('ENCHILADAS SUIZAS VERDES'::varchar, 'Salsa Verde de la Casa'::varchar, false::bool, NULL::numeric),
        ('ENCHILADAS SUIZAS VERDES'::varchar, 'Frijoles Refritos de la Casa'::varchar, false::bool, NULL::numeric),
        ('ENMOLADAS PETRA'::varchar, 'Mole Especial Petra'::varchar, false::bool, NULL::numeric),
        ('ENMOLADAS PETRA'::varchar, 'Frijoles Refritos de la Casa'::varchar, false::bool, NULL::numeric),
        ('CHILAQUILES VERDES O ROJOS'::varchar, 'Salsa Verde de la Casa'::varchar, false::bool, NULL::numeric),
        ('CHILAQUILES VERDES O ROJOS'::varchar, 'Frijoles Refritos de la Casa'::varchar, false::bool, NULL::numeric),
        ('ESPECIALES PETRA'::varchar, 'Salsa Blanca de Habanero'::varchar, false::bool, NULL::numeric),
        ('SANDWICH PARMESANO'::varchar, 'Salsa Pomodoro Cremosa'::varchar, false::bool, NULL::numeric),
        ('AVOCADO TOAST'::varchar, 'Cremoso de Aguacate'::varchar, false::bool, NULL::numeric),
        ('GRILL CHEESE'::varchar, 'Salsa Pomodoro Cremosa'::varchar, false::bool, NULL::numeric),
        ('OMELETTE MILPA'::varchar, 'Salsa Poblana'::varchar, false::bool, NULL::numeric),
        ('OMELETTE SALMON'::varchar, 'Salsa Roja Ranchera'::varchar, false::bool, NULL::numeric),
        ('BENEDICTINOS PETRA'::varchar, 'Salsa Holandesa'::varchar, false::bool, NULL::numeric),
        ('APORREADO PETRA'::varchar, 'Salsa Roja Ranchera'::varchar, false::bool, NULL::numeric),
        ('APORREADO PETRA'::varchar, 'Frijoles Refritos de la Casa'::varchar, false::bool, NULL::numeric),
        ('HUEVOS AL GUSTO'::varchar, 'Salsa Roja Ranchera'::varchar, false::bool, NULL::numeric),
        ('HUEVOS AL GUSTO'::varchar, 'Frijoles Refritos de la Casa'::varchar, false::bool, NULL::numeric),
        ('TOSTADAS DE ATÚN 2 PZS'::varchar, 'Salsa Ponzu Cítrica'::varchar, false::bool, NULL::numeric),
        ('TOSTADAS DE PULPO ASADO 3 PZS'::varchar, 'Cremoso de Aguacate'::varchar, false::bool, NULL::numeric),
        ('CROQUETA DE PLATANO MACHO'::varchar, 'Mole de Flor de Calabaza'::varchar, false::bool, NULL::numeric),
        ('EMPANADAS DE CAMARON Y MARACUYÁ'::varchar, 'Salsa de Maracuyá y Chipotle'::varchar, false::bool, NULL::numeric),
        ('TACOS DE PICANHA 3 PZS'::varchar, 'Salsa de Piña Tatemada y Habanero'::varchar, false::bool, NULL::numeric),
        ('EL APAPACHO'::varchar, 'Fondo de Pollo Milpa'::varchar, false::bool, NULL::numeric),
        ('SOPA DE TORTILLA'::varchar, 'Fondo de Pollo Milpa'::varchar, false::bool, NULL::numeric),
        ('PASTA NORDICA'::varchar, 'Salsa 3 Quesos'::varchar, false::bool, NULL::numeric),
        ('PASTA TRAVIATA'::varchar, 'Salsa Pomodoro Cremosa'::varchar, false::bool, NULL::numeric),
        ('PAPPARDELLE RELLENO DE RAGÚ'::varchar, 'Ragú de Res Tradicional'::varchar, false::bool, NULL::numeric),
        ('ENSALADA RUBÍ'::varchar, 'Vinagreta de Mostaza y Miel'::varchar, false::bool, NULL::numeric),
        ('ENSALADA PEACH'::varchar, 'Vinagreta de Naranja'::varchar, false::bool, NULL::numeric),
        ('ENSALADA CÉSAR PETRA (POLLO)'::varchar, 'Aderezo César Hecho en Casa'::varchar, false::bool, NULL::numeric),
        ('ENSALADA CÉSAR PETRA (CAMARÓN)'::varchar, 'Aderezo César Hecho en Casa'::varchar, false::bool, NULL::numeric),
        ('ENSALADA CÉSAR PETRA (ARRACHERA)'::varchar, 'Aderezo César Hecho en Casa'::varchar, false::bool, NULL::numeric),
        ('BIFE PETRA (CHOICE)'::varchar, 'Chimichurri de la Casa'::varchar, false::bool, NULL::numeric),
        ('FILETE MAR Y TIERRA'::varchar, 'Salsa Roja de Langostinos'::varchar, false::bool, NULL::numeric),
        ('PECHUGA MATAMOROS'::varchar, 'Salsa Bechamel'::varchar, false::bool, NULL::numeric),
        ('PECHUGA MATAMOROS'::varchar, 'Salsa de Mango Picante'::varchar, false::bool, NULL::numeric),
        ('PECHUGA ESPECIAL PETRA'::varchar, 'Mole de Flor de Calabaza'::varchar, false::bool, NULL::numeric),
        ('SALMÓN SPICY'::varchar, 'Puré de Guisantes'::varchar, false::bool, NULL::numeric),
        ('PULPO ENDIABLADO'::varchar, 'Adobo Sarandeado para Pulpo'::varchar, false::bool, NULL::numeric),
        ('CHEESE CAKE'::varchar, 'Compota de Vino Tinto'::varchar, false::bool, NULL::numeric),
        ('TARTA DE PISTACHE'::varchar, 'Praliné de Pistache'::varchar, false::bool, NULL::numeric),
        ('TARTA DE PISTACHE'::varchar, 'Compota de Vino Tinto'::varchar, false::bool, NULL::numeric),
        ('PETITE FOURS CHOCOLATE'::varchar, 'Mousse de Chocolate Bicolor'::varchar, false::bool, NULL::numeric)
    ) AS s(dish_nombre, subreceta_nombre, modificable, precio)
    JOIN tenant_menu_product dp ON dp.nombre = s.dish_nombre
        AND dp.category_id IN (SELECT id FROM tenant_menu_category WHERE tenant_id = v_tenant_id)
    JOIN tenant_menu_product sr ON sr.nombre = s.subreceta_nombre
        AND sr.category_id IN (SELECT id FROM tenant_menu_category WHERE tenant_id = v_tenant_id)
    WHERE NOT EXISTS (
        SELECT 1 FROM product_sub_receta epsr
        WHERE epsr.dish_product_id = dp.id AND epsr.sub_receta_id = sr.id
    );

    -- --------------------------------------------------------------------------
    -- 9. AJUSTE DE SECUENCIAS POSTGRESQL (setval)
    -- --------------------------------------------------------------------------
    PERFORM setval('tenant_menu_category_id_seq', COALESCE((SELECT MAX(id) FROM tenant_menu_category), 1));
    PERFORM setval('insumo_id_seq', COALESCE((SELECT MAX(id) FROM insumo), 1));
    PERFORM setval('tenant_menu_product_id_seq', COALESCE((SELECT MAX(id) FROM tenant_menu_product), 1));
    PERFORM setval('product_recipe_id_seq', COALESCE((SELECT MAX(id) FROM product_recipe), 1));
    PERFORM setval('product_sub_receta_id_seq', COALESCE((SELECT MAX(id) FROM product_sub_receta), 1));

    RAISE NOTICE '¡Carga del Menú de Restaurante Petra finalizada con éxito para el tenant ID: %!', v_tenant_id;
END $$;

COMMIT;
