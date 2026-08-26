# Lealtix Service

## 📊 **NUEVO: Dashboard de Reportes (2026-01-03)**

El backend ahora incluye **endpoints completos para dashboard de negocio** con 7 KPIs:
- ✅ Total de clientes y clientes nuevos por periodo
- ✅ Cupones creados vs redimidos con % de redención
- ✅ Ventas totales y ticket promedio
- ✅ Rendimiento completo por campaña

📖 **Ver documentación**: 
- Guía rápida: `IMPLEMENTACION_RAPIDA.md`
- Documentación técnica: `DASHBOARD_README.md`
- Resumen de cambios: `CAMBIOS_DASHBOARD.md`

🚀 **Para implementar**: Ejecutar `.\ejecutar-migracion-dashboard.ps1`

---

# Lealtix Service

Backend del proyecto **Lealtix**, encargado de gestionar el **pre-registro de usuarios y la generación de invitaciones** para la plataforma.

---

## 📌 Descripción

Este servicio backend está desarrollado en **Java Spring Boot** y tiene como objetivo:

- Recibir datos de pre-registro (nombre y email) desde el frontend Angular.
- Validar que el email no esté registrado previamente.
- Almacenar pre-registros en **PostgreSQL**.
- Generar invitaciones con token único para completar el registro del usuario.
- Mantener el estado de cada pre-registro e invitación (`PENDING`, `INVITED`, `REGISTERED`, etc.).

---

## 🛠 Tecnologías

- **Lenguaje:** Java 17+
- **Framework:** Spring Boot 3.x
- **Base de datos:** PostgreSQL
- **ORM:** Spring Data JPA / Hibernate
- **Gestión de dependencias:** Maven
- **Otros:** Lombok (opcional para reducir boilerplate)

---

## ⚡ Instalación y ejecución local

### 1. Clonar el repositorio

```bash
git clone https://github.com/syborx-git/lealtix-BE.git
cd lealtix-BE
```

### 2. Configurar variables de entorno

Copia la plantilla y configura tu archivo `.env`:

```bash
cp .env.example .env
```

### 3. Ejecutar con Maven

```bash
mvn clean spring-boot:run
```

---

## 🚀 Despliegue en Servidor Hetzner VPS (Docker + Neon DB)

Esta sección detalla cómo operar el Backend dentro del stack completo de **Lealtix** en un servidor Hetzner compartido (donde otros proyectos usan los puertos `80` y `443`).

### 🗺️ Mapeo de Puertos y Arquitectura

| Servicio | Puerto Host (VPS) | Puerto Interno | RAM Asignada | Notas |
| :--- | :--- | :--- | :--- | :--- |
| **`lealtix-backend`** | **`8082`** | `8080` | **768 MB máx** | Spring Boot 3 / Java 17 con JVM afinada (`-Xms256m -Xmx512m`). |
| **`lealtix-main`** | **`3000`** | `80` | **128 MB máx** | Frontend Landing / Cliente en Nginx Alpine. |
| **`lealtix-dashboard`** | **`4201`** | `80` | **128 MB máx** | Frontend Panel Admin / Mesero en Nginx Alpine. |
| **Neon PostgreSQL** | Cloud (Externo) | `5432` | **0 MB (VPS)** | Conexión SSL pooled (`?sslmode=require`). |

---

### 📜 Scripts y Comandos Principales

#### 1. Preparación del Servidor (Solo la primera vez)

```bash
# A. Crear Swap de 4GB para soportar builds sin saturar la RAM física
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# B. Abrir los puertos en el Firewall (UFW)
sudo ufw allow 3000/tcp
sudo ufw allow 4201/tcp
sudo ufw allow 8082/tcp
sudo ufw reload
```
> **Explicación:** El Swap evita que el proceso de compilación (`mvn` y `npm`) agote la memoria RAM y bloquee el servidor. Las reglas UFW permiten el acceso externo a los puertos de Lealtix sin tocar los puertos de otros proyectos.

---

#### 2. Iniciar Todo el Stack de Lealtix

Desde la carpeta raíz del proyecto (`/var/www/lealtix`):

```bash
# Compilar imágenes y levantar contenedores en segundo plano
docker compose up -d --build
```
> **Explicación:** Construye las imágenes Docker de los 3 servicios aplicando multi-stage caching, configura la red interna `lealtix-net`, aplica los límites de memoria de 768MB para el BE y levanta todo en segundo plano (`-d`).

---

#### 3. Iniciar o Reiniciar Solo el Backend

```bash
# Reiniciar backend sin tocar los frontends (útil tras cambiar el .env):
docker compose restart lealtix-backend

# Reconstruir solo el backend tras actualizar código:
docker compose up -d --build --no-deps lealtix-backend
```
> **Explicación:** `--no-deps` asegura que únicamente se reconstruya y reinicie el contenedor de `lealtix-backend` de forma instantánea sin interrumpir a los usuarios que navegan en los frontends.

---

#### 4. Monitoreo y Verificación de Logs

```bash
# A. Ver logs en tiempo real del Backend (conexión a Neon DB, Flyway, endpoints):
docker compose logs -f lealtix-backend

# B. Ver estado general de los contenedores:
docker compose ps

# C. Monitorear el consumo de memoria RAM y CPU en tiempo real:
docker stats
```
> **Explicación:** `docker stats` permite comprobar que el Backend se mantenga estable entre 350 MB y 480 MB de RAM y que el stack completo consuma menos de 600 MB.

---

#### 5. Detener los Servicios

```bash
# Detener los contenedores sin borrar volúmenes ni imágenes:
docker compose down
```

---

#### 6. Actualización Rápida de Código (Deploy de Nuevos Cambios)

```bash
cd /var/www/lealtix/lealtix-BE
git pull origin develop
cd ..
docker compose up -d --build --no-deps lealtix-backend
```
