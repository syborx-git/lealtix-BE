import json
import os

with open("scratch/menu_petra_dump.json", "r", encoding="utf-8") as f:
    data = json.load(f)

categories = data["categories"]
insumos = data["insumos"]
products = data["products"]
prod_cats = data["product_categories"]
recipes = data["recipes"]
sub_recetas = data["sub_recetas"]
additionals = data.get("additionals", [])

def escape_sql(val):
    if val is None:
        return "NULL"
    s = str(val).replace("'", "''")
    return f"'{s}'"

def num_sql(val, default="0.00"):
    if val is None:
        return default
    return str(val)

def bool_sql(val):
    return "true" if val else "false"

out = []
out.append("""-- ==============================================================================
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
""")

cat_vals = []
for c in categories:
    nom = escape_sql(c['nombre'])
    desc = escape_sql(c['descripcion'])
    dord = c['display_order']
    act = bool_sql(c['is_active'])
    cat_vals.append(f"        ({nom}::varchar, {desc}::varchar, {dord}::int, {act}::bool)")
out.append(",\n".join(cat_vals))
out.append("""    ) AS c(nombre, descripcion, display_order, is_active)
    WHERE NOT EXISTS (
        SELECT 1 FROM tenant_menu_category ec 
        WHERE ec.tenant_id = v_tenant_id AND ec.nombre = c.nombre
    );
""")

out.append("""    -- --------------------------------------------------------------------------
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
""")

ins_vals = []
for i in insumos:
    nom = escape_sql(i['nombre'])
    uni = escape_sql(i['unidad'])
    stk = num_sql(i['stock'])
    smin = num_sql(i['stock_minimo'])
    sbod = num_sql(i['stock_bodega'])
    scoc = num_sql(i['stock_cocina'])
    sbar = num_sql(i['stock_barra'])
    esb = bool_sql(i['es_bebida'])
    pv = escape_sql(i['precio_venta']) if i['precio_venta'] is not None else "NULL"
    act = bool_sql(i['is_active'])
    ins_vals.append(f"        ({nom}::varchar, {uni}::varchar, {stk}::float8, {smin}::float8, {sbod}::float8, {scoc}::float8, {sbar}::float8, {esb}::bool, {pv}::numeric, {act}::bool)")

out.append(",\n".join(ins_vals))
out.append("""    ) AS i(nombre, unidad, stock, stock_minimo, stock_bodega, stock_cocina, stock_barra, es_bebida, precio_venta, is_active)
    WHERE NOT EXISTS (
        SELECT 1 FROM insumo ei 
        WHERE ei.tenant_id = v_tenant_id AND ei.nombre = i.nombre
    );
""")

out.append("""    -- --------------------------------------------------------------------------
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
""")

subreceta_items = [p for p in products if p.get("es_sub_receta")]
sub_vals = []
for p in subreceta_items:
    pr = num_sql(p['precio'])
    img = escape_sql(p['img_url']) if p['img_url'] else "NULL"
    nom = escape_sql(p['nombre'])
    desc = escape_sql(p['descripcion'])
    stk = num_sql(p['stock'])
    smin = num_sql(p['stock_minimo'])
    uni = escape_sql(p['unidad'])
    vi = bool_sql(p['venta_individual'])
    aa = bool_sql(p['auto_availability'])
    esr = bool_sql(p['es_sub_receta'])
    act = bool_sql(p['is_active'])
    catn = escape_sql(p['category_nombre'])
    sub_vals.append(f"        ({pr}::numeric, {img}::varchar, {nom}::varchar, {desc}::varchar, {stk}::float8, {smin}::float8, {uni}::varchar, {vi}::bool, {aa}::bool, {esr}::bool, {act}::bool, {catn}::varchar)")

out.append(",\n".join(sub_vals))
out.append("""    ) AS sub(precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, cat_nombre)
    JOIN tenant_menu_category c ON c.nombre = sub.cat_nombre AND c.tenant_id = v_tenant_id
    WHERE NOT EXISTS (
        SELECT 1 FROM tenant_menu_product ep
        JOIN tenant_menu_category ec ON ep.category_id = ec.id
        WHERE ec.tenant_id = v_tenant_id AND ep.nombre = sub.nombre
    );
""")

out.append("""    -- --------------------------------------------------------------------------
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
""")

platillo_items = [p for p in products if not p.get("es_sub_receta")]
plat_vals = []
for p in platillo_items:
    pr = num_sql(p['precio'])
    img = escape_sql(p['img_url']) if p['img_url'] else "NULL"
    nom = escape_sql(p['nombre'])
    desc = escape_sql(p['descripcion'])
    stk = num_sql(p['stock'])
    smin = num_sql(p['stock_minimo'])
    uni = escape_sql(p['unidad'])
    vi = bool_sql(p['venta_individual'])
    aa = bool_sql(p['auto_availability'])
    esr = bool_sql(p['es_sub_receta'])
    act = bool_sql(p['is_active'])
    catn = escape_sql(p['category_nombre'])
    plat_vals.append(f"        ({pr}::numeric, {img}::varchar, {nom}::varchar, {desc}::varchar, {stk}::float8, {smin}::float8, {uni}::varchar, {vi}::bool, {aa}::bool, {esr}::bool, {act}::bool, {catn}::varchar)")

out.append(",\n".join(plat_vals))
out.append("""    ) AS p(precio, img_url, nombre, descripcion, stock, stock_minimo, unidad, venta_individual, auto_availability, es_sub_receta, is_active, cat_nombre)
    JOIN tenant_menu_category c ON c.nombre = p.cat_nombre AND c.tenant_id = v_tenant_id
    WHERE NOT EXISTS (
        SELECT 1 FROM tenant_menu_product ep
        JOIN tenant_menu_category ec ON ep.category_id = ec.id
        WHERE ec.tenant_id = v_tenant_id AND ep.nombre = p.nombre
    );
""")

out.append("""    -- --------------------------------------------------------------------------
    -- 6. ASIGNACIÓN MULTICATEGORÍA (tenant_menu_product_category)
    -- --------------------------------------------------------------------------
    INSERT INTO tenant_menu_product_category (product_id, category_id)
    SELECT p.id, c.id
    FROM (VALUES
""")

pc_vals = []
for pc in prod_cats:
    pnom = escape_sql(pc['product_nombre'])
    cnom = escape_sql(pc['category_nombre'])
    pc_vals.append(f"        ({pnom}::varchar, {cnom}::varchar)")

out.append(",\n".join(pc_vals))
out.append("""    ) AS pc(product_nombre, cat_nombre)
    JOIN tenant_menu_product p ON p.nombre = pc.product_nombre
        AND p.category_id IN (SELECT id FROM tenant_menu_category WHERE tenant_id = v_tenant_id)
    JOIN tenant_menu_category c ON c.nombre = pc.cat_nombre AND c.tenant_id = v_tenant_id
    WHERE NOT EXISTS (
        SELECT 1 FROM tenant_menu_product_category epc 
        WHERE epc.product_id = p.id AND epc.category_id = c.id
    );
""")

out.append("""    -- --------------------------------------------------------------------------
    -- 7. INSERTAR RECETAS (product_recipe) - Platillos y Sub-recetas
    -- --------------------------------------------------------------------------
    INSERT INTO product_recipe (dish_product_id, insumo_id, cantidad, modificable)
    SELECT 
        p.id,
        i.id,
        r.cantidad,
        r.modificable
    FROM (VALUES
""")

rec_vals = []
for r in recipes:
    pnom = escape_sql(r['dish_nombre'])
    inom = escape_sql(r['insumo_nombre'])
    cant = num_sql(r['cantidad'])
    mod = bool_sql(r['modificable'])
    rec_vals.append(f"        ({pnom}::varchar, {inom}::varchar, {cant}::numeric, {mod}::bool)")

out.append(",\n".join(rec_vals))
out.append("""    ) AS r(dish_nombre, insumo_nombre, cantidad, modificable)
    JOIN tenant_menu_product p ON p.nombre = r.dish_nombre
        AND p.category_id IN (SELECT id FROM tenant_menu_category WHERE tenant_id = v_tenant_id)
    JOIN insumo i ON i.nombre = r.insumo_nombre AND i.tenant_id = v_tenant_id
    WHERE NOT EXISTS (
        SELECT 1 FROM product_recipe er
        WHERE er.dish_product_id = p.id AND er.insumo_id = i.id
    );
""")

out.append("""    -- --------------------------------------------------------------------------
    -- 8. ASIGNAR SUB-RECETAS A PLATILLOS (product_sub_receta)
    -- --------------------------------------------------------------------------
    INSERT INTO product_sub_receta (dish_product_id, sub_receta_id, modificable, precio)
    SELECT 
        dp.id,
        sr.id,
        s.modificable,
        s.precio
    FROM (VALUES
""")

sr_vals = []
for s in sub_recetas:
    dp_nom = escape_sql(s['dish_nombre'])
    sr_nom = escape_sql(s['subreceta_nombre'])
    mod = bool_sql(s['modificable'])
    pr = num_sql(s['precio']) if s['precio'] is not None else "NULL"
    sr_vals.append(f"        ({dp_nom}::varchar, {sr_nom}::varchar, {mod}::bool, {pr}::numeric)")

out.append(",\n".join(sr_vals))
out.append("""    ) AS s(dish_nombre, subreceta_nombre, modificable, precio)
    JOIN tenant_menu_product dp ON dp.nombre = s.dish_nombre
        AND dp.category_id IN (SELECT id FROM tenant_menu_category WHERE tenant_id = v_tenant_id)
    JOIN tenant_menu_product sr ON sr.nombre = s.subreceta_nombre
        AND sr.category_id IN (SELECT id FROM tenant_menu_category WHERE tenant_id = v_tenant_id)
    WHERE NOT EXISTS (
        SELECT 1 FROM product_sub_receta epsr
        WHERE epsr.dish_product_id = dp.id AND epsr.sub_receta_id = sr.id
    );
""")

out.append("""    -- --------------------------------------------------------------------------
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
""")

sql_content = "\n".join(out)

os.makedirs("script-insert", exist_ok=True)
target_path = "script-insert/insert_menu_restaurante_petra.sql"
with open(target_path, "w", encoding="utf-8") as f:
    f.write(sql_content)

print(f"Generated SQL file at: {target_path}")
print(f"Total lines: {len(sql_content.splitlines())}")
print(f"Size: {len(sql_content.encode('utf-8'))} bytes")
