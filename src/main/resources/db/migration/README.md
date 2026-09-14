# DB Migrations — Lealtix Service

Flyway migrations consolidadas. Cada archivo agrupa un módulo de negocio completo.

## Archivos

| Versión | Módulo | Describe |
|---------|--------|----------|
| V0 | Esquema Base Fundacional | Crea las 19 tablas del modelo base pre-existentes (`app_user`, `role`, `user_role`, `tenant`, `tenant_config`, `tenant_payment`, `pre_registro`, `invitations`, `email_log`, `tenant_customer`, `campaign_template`, `campaign`, `campaign_result`, `coupon_redemption`, `tenant_menu_category`, `tenant_menu_product`, `insumo`, `product_recipe`, `product_additional`) |
| V1 | Campañas | `campaign` (columnas finales, constraints), `promotion_reward`, `campaign_result`, `campaign_email`, `campaign_email_payload` |
| V2 | Lealtad / Clientes | `tenant_customer` (accepted_promotions, active), `coupon` (estado final), `coupon_redemption` (campos de cálculo) |
| V3 | Comandix — Órdenes | `client_order` (estado final completo, 18+ columnas), `client_order_item` |
| V4 | ChatBot + Cross-Selling | `product_cross_selling`, `chatbot_session`, `chatbot_message` |
| V5 | Usuarios, Roles y Permisos | `tenant` (kitchen), `tenant_config`, `tenant_user` (con HOSTESS + sueldo_mensual), `user_permission`, `permission` (catálogo completo), `role_permission` (asignaciones finales) |
| V6 | Cocina e Inventario | `product_additional.precio`, `insumo` (bebidas), `tenant_menu_product_category`, `insumo_category`, `tenant_menu_product` (auto_availability, es_sub_receta), `product_sub_receta`, `restock_history` |
| V7 | Hostess | `mesa`, `reserva` |
| V8 | Mermas y Alergias | `merma`, `allergy`, `tenant_customer_allergy` |
| V9 | Demo Seed | Tenant "Restaurante Demo", usuarios por rol, config y mesas (**solo local**) |

## Credenciales Demo (solo local)

Endpoint: `POST /api/tenant/auth/login`
Body: `{ "email": "...", "password": "..." }`

| Rol | Email | Password |
|-----|-------|----------|
| ADMIN | admin@demo.com | admin123 |
| MESERO | mesero@demo.com | mesero123 |
| COCINA | cocina@demo.com | cocina123 |
| CAJA | caja@demo.com | caja123 |
| MARKETING | marketing@demo.com | marketing123 |
| HOSTESS | hostess@demo.com | hostess123 |

## Notas de consolidación

- **V17+V18 eliminadas**: se cancelaban entre sí (neto cero sobre `business_id`).
- **V6 duplicado eliminado**: `V6__ensure_promotion_reward_description_length.sql` era redundante — `description VARCHAR(500)` ya estaba definida desde V3.
- Los archivos anteriores están en `db/migration_backup_<timestamp>/` como respaldo.