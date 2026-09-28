import subprocess
import json
import os

psql_path = r"C:\Program Files\PostgreSQL\17\bin\psql.exe"
env = os.environ.copy()
env["PGPASSWORD"] = "admin"

def query_json(sql):
    cmd = [psql_path, "-U", "postgres", "-d", "lealtix_db", "-t", "-A", "-c", sql]
    res = subprocess.run(cmd, capture_output=True, text=True, env=env, encoding="utf-8")
    if res.returncode != 0:
        raise Exception(res.stderr)
    out = res.stdout.strip()
    if not out or out == "":
        return []
    return json.loads(out)

# 1. Categorías
cats = query_json("""
SELECT json_agg(json_build_object(
    'id', id,
    'nombre', nombre,
    'descripcion', coalesce(descripcion, ''),
    'display_order', coalesce(display_order, 0),
    'is_active', is_active
) ORDER BY display_order, id)
FROM tenant_menu_category WHERE tenant_id = 1;
""")

# 2. Insumos
insumos = query_json("""
SELECT json_agg(json_build_object(
    'id', id,
    'nombre', nombre,
    'unidad', coalesce(unidad, 'pieza'),
    'stock', coalesce(stock, 0),
    'stock_minimo', coalesce(stock_minimo, 0),
    'stock_bodega', coalesce(stock_bodega, 0),
    'stock_cocina', coalesce(stock_cocina, 0),
    'stock_barra', coalesce(stock_barra, 0),
    'es_bebida', es_bebida,
    'precio_venta', precio_venta,
    'is_active', is_active
) ORDER BY id)
FROM insumo WHERE tenant_id = 1;
""")

# 3. Productos y sub-recetas
products = query_json("""
SELECT json_agg(json_build_object(
    'id', p.id,
    'nombre', p.nombre,
    'descripcion', coalesce(p.descripcion, ''),
    'precio', p.precio,
    'img_url', coalesce(p.img_url, ''),
    'stock', coalesce(p.stock, 0),
    'stock_minimo', coalesce(p.stock_minimo, 0),
    'unidad', coalesce(p.unidad, 'pieza'),
    'venta_individual', coalesce(p.venta_individual, false),
    'auto_availability', coalesce(p.auto_availability, true),
    'es_sub_receta', coalesce(p.es_sub_receta, false),
    'is_active', p.is_active,
    'category_nombre', c.nombre
) ORDER BY p.es_sub_receta, p.id)
FROM tenant_menu_product p
JOIN tenant_menu_category c ON p.category_id = c.id
WHERE c.tenant_id = 1;
""")

# 4. Asignaciones multicategoría
prod_cats = query_json("""
SELECT json_agg(json_build_object(
    'product_nombre', p.nombre,
    'category_nombre', c.nombre
) ORDER BY p.id, c.id)
FROM tenant_menu_product_category pc
JOIN tenant_menu_product p ON pc.product_id = p.id
JOIN tenant_menu_category c ON pc.category_id = c.id
WHERE c.tenant_id = 1;
""")

# 5. Recetas
recipes = query_json("""
SELECT json_agg(json_build_object(
    'dish_nombre', p.nombre,
    'insumo_nombre', i.nombre,
    'cantidad', r.cantidad,
    'modificable', coalesce(r.modificable, false)
) ORDER BY p.id, r.id)
FROM product_recipe r
JOIN tenant_menu_product p ON r.dish_product_id = p.id
JOIN insumo i ON r.insumo_id = i.id
JOIN tenant_menu_category c ON p.category_id = c.id
WHERE c.tenant_id = 1;
""")

# 6. Sub-recetas asignadas a platillos
sub_recetas = query_json("""
SELECT json_agg(json_build_object(
    'dish_nombre', dp.nombre,
    'subreceta_nombre', sr.nombre,
    'modificable', coalesce(psr.modificable, false),
    'precio', psr.precio
) ORDER BY dp.id, psr.id)
FROM product_sub_receta psr
JOIN tenant_menu_product dp ON psr.dish_product_id = dp.id
JOIN tenant_menu_product sr ON psr.sub_receta_id = sr.id
JOIN tenant_menu_category c ON dp.category_id = c.id
WHERE c.tenant_id = 1;
""")

# 7. Adicionales
additionals = query_json("""
SELECT json_agg(json_build_object(
    'dish_nombre', dp.nombre,
    'insumo_nombre', i.nombre,
    'cantidad', pa.cantidad,
    'precio', pa.precio
) ORDER BY dp.id, pa.id)
FROM product_additional pa
JOIN tenant_menu_product dp ON pa.dish_product_id = dp.id
JOIN insumo i ON pa.insumo_id = i.id
JOIN tenant_menu_category c ON dp.category_id = c.id
WHERE c.tenant_id = 1;
""")

data = {
    'categories': cats,
    'insumos': insumos,
    'products': products,
    'product_categories': prod_cats,
    'recipes': recipes,
    'sub_recetas': sub_recetas,
    'additionals': additionals if additionals else []
}

with open("scratch/menu_petra_dump.json", "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("Export completed successfully:")
print(f"Categories: {len(cats)}")
print(f"Insumos: {len(insumos)}")
print(f"Products: {len(products)}")
print(f"Product categories: {len(prod_cats)}")
print(f"Recipes: {len(recipes)}")
print(f"Sub-recetas: {len(sub_recetas)}")
print(f"Additionals: {len(additionals) if additionals else 0}")
