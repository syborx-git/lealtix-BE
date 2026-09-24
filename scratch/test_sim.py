import subprocess
import os

psql_path = r"C:\Program Files\PostgreSQL\17\bin\psql.exe"
env = os.environ.copy()
env["PGPASSWORD"] = "admin"

with open("script-insert/insert_menu_restaurante_petra.sql", "r", encoding="utf-8") as f:
    sql = f.read()

# Simularemos un nuevo tenant para probar la inserción completa desde cero
test_sql = """
BEGIN;

INSERT INTO app_user (full_name, email, password_hash, is_active, created_at, updated_at)
VALUES ('Test Owner', 'test_owner_sim@demo.com', 'hash', true, NOW(), NOW());

INSERT INTO tenant (nombre_negocio, slug, is_active, created_at, updated_at, user_id)
VALUES ('Restaurante Simulado', 'sim-tenant', true, NOW(), NOW(), (SELECT id FROM app_user WHERE email = 'test_owner_sim@demo.com'));

""" + sql.replace("BEGIN;", "").replace(
    "SELECT id INTO v_tenant_id FROM tenant WHERE slug IN ('demo', 'restaurante-petra', 'la-taqueria-demo') ORDER BY id ASC LIMIT 1;",
    "SELECT id INTO v_tenant_id FROM tenant WHERE slug = 'sim-tenant';"
).replace(
    "COMMIT;",
    """
    SELECT count(*) as total_cats FROM tenant_menu_category WHERE tenant_id = (SELECT id FROM tenant WHERE slug = 'sim-tenant');
    SELECT count(*) as total_insumos FROM insumo WHERE tenant_id = (SELECT id FROM tenant WHERE slug = 'sim-tenant');
    SELECT count(*) as total_products FROM tenant_menu_product p JOIN tenant_menu_category c ON p.category_id = c.id WHERE c.tenant_id = (SELECT id FROM tenant WHERE slug = 'sim-tenant');
    SELECT count(*) as total_subrecetas FROM tenant_menu_product p JOIN tenant_menu_category c ON p.category_id = c.id WHERE c.tenant_id = (SELECT id FROM tenant WHERE slug = 'sim-tenant') AND p.es_sub_receta = true;
    SELECT count(*) as total_subrecetas_asignadas FROM product_sub_receta psr JOIN tenant_menu_product p ON psr.dish_product_id = p.id JOIN tenant_menu_category c ON p.category_id = c.id WHERE c.tenant_id = (SELECT id FROM tenant WHERE slug = 'sim-tenant');
    SELECT count(*) as total_recetas FROM product_recipe pr JOIN tenant_menu_product p ON pr.dish_product_id = p.id JOIN tenant_menu_category c ON p.category_id = c.id WHERE c.tenant_id = (SELECT id FROM tenant WHERE slug = 'sim-tenant');
    ROLLBACK;
    """
)

with open("scratch/test_sim.sql", "w", encoding="utf-8") as f:
    f.write(test_sql)

res = subprocess.run([psql_path, "-U", "postgres", "-d", "lealtix_db", "-f", "scratch/test_sim.sql"], capture_output=True, text=True, env=env, encoding="utf-8")
print("STDOUT:")
print(res.stdout)
if res.stderr:
    print("STDERR:")
    print(res.stderr)
