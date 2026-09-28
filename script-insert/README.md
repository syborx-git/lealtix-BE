# Scripts de Inserción de Menú - Lealtix Backend

Este directorio contiene los scripts SQL preparados para inicializar o restaurar la configuración completa del menú gastronómico, recetas, sub-recetas e insumos en la base de datos PostgreSQL de **Lealtix**.

---

## 📄 Archivo Principal

* **`insert_menu_restaurante_petra.sql`**: Script completo e idempotente con el catálogo gastronómico de "Restaurante Petra" (desayunos, comidas, postres, entradas, especialidades, sub-recetas e insumos).

---

## 📊 Contenido y Resumen de Datos

El script inserta y relaciona de forma 100% relacional:

| Elemento | Cantidad | Descripción |
| :--- | :---: | :--- |
| **Categorías del Menú** | **14** | `DESAYUNOS`, `ENCHILADAS`, `CHILAQUILES`, `ESPECIALES`, `HUEVOS`, `COMIDAS`, `ENTRADAS`, `SOPAS Y PASTAS`, `ENSALADAS`, `PLATOS FUERTES`, `MENÚ INFANTIL`, `EXTRAS`, `POSTRES` y `Preparaciones` (categoría técnica para sub-recetas). |
| **Insumos de Cocina** | **113** | Materias primas y proteínas con unidades de medida (`gramos`, `mililitros`, `pieza`), stocks iniciales y mínimos distribuidos en cocina y bodega. |
| **Sub-recetas / Preparaciones** | **29** | Salsas caseras (salsa verde, mole especial, salsa amarillita petra, pomodoro, habanero blanco, etc.), aderezos, purés y guarniciones base (`es_sub_receta = true`, `precio = 0.00`). |
| **Platillos y Complementos** | **58** | Platillos vendibles en Comandix POS con precios oficiales, descripciones detalladas del menú y auto-disponibilidad activada. |
| **Recetas (Líneas de Composición)** | **332** | Gramajes y cantidades exactas de insumos requeridos por cada platillo y sub-receta (`product_recipe`), indicando ingredientes modificables (exclusiones) y base. |
| **Sub-recetas Asignadas a Platillos** | **48** | Enlaces entre platillos y sus preparaciones intermedias (`product_sub_receta`), configurando salsas, aderezos o frijoles refritos que componen el plato. |
| **Asignaciones Multicategoría** | **87** | Enlaces en `tenant_menu_product_category`. |

---

## 🛠️ Características de Arquitectura del Script

1. **Resolución Dinámica de Tenant**:
   - No depende de un `tenant_id = 1` fijo.
   - Detecta automáticamente el primer tenant disponible o busca por slug (`demo`, `restaurante-petra`, `la-taqueria-demo`).
   - Si la base de datos está totalmente vacía, crea un usuario y tenant base para que las claves foráneas nunca fallen.
2. **Sin IDs Quemados (Hardcoded IDs)**:
   - Todas las relaciones (recetas, sub-recetas, categorías) se resuelven mediante `JOIN` por nombre exacto dentro del tenant objetivo.
   - Es compatible tanto con bases de datos recién creadas como con bases de datos que ya tienen otros productos.
3. **Idempotencia y Seguridad**:
   - Utiliza cláusulas `WHERE NOT EXISTS` para evitar duplicar registros si el script se ejecuta más de una vez.
   - No borra pedidos existentes (`client_order_item`), evitando errores de integridad referencial.
   - Contiene un flag opcional `v_force_reset := FALSE;` por si el administrador desea forzar un reseteo total desde cero.
4. **Alineación de Secuencias**:
   - Ejecuta `setval()` al final para asegurar que las secuencias de PostgreSQL (`_id_seq`) queden sincronizadas con los IDs generados, permitiendo que la aplicación Spring Boot continúe guardando nuevos registros sin errores de duplicidad.

---

## 🚀 Cómo Ejecutar

### Opción 1: Mediante consola `psql` (Línea de Comandos)

```bash
# En Windows (usando ruta de PostgreSQL):
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -d lealtix_db -f "lealtix-be/script-insert/insert_menu_restaurante_petra.sql"

# En Linux / macOS / Docker:
psql -U postgres -d lealtix_db -f insert_menu_restaurante_petra.sql
```

### Opción 2: Mediante interfaz gráfica (pgAdmin, DBeaver, Neon Console)

1. Abre tu herramienta de base de datos conectada a PostgreSQL.
2. Abre una ventana de consulta SQL (Query Tool / SQL Editor).
3. Abre o pega el contenido completo de `insert_menu_restaurante_petra.sql`.
4. Ejecuta todo el script (`F5` o `Execute Script`).
5. Verifica en la pestaña de mensajes la notificación de éxito:
   ```text
   NOTICE: Utilizando tenant existente con ID: 1
   NOTICE: ¡Carga del Menú de Restaurante Petra finalizada con éxito para el tenant ID: 1!
   ```
