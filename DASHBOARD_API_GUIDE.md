# Guía de Uso de Endpoints del Dashboard

## Formato de Fechas

Los endpoints del dashboard aceptan fechas en formato ISO 8601. El backend ahora soporta múltiples variantes:

### ✅ Formatos Aceptados:

1. **Formato básico (sin zona horaria):**
   ```
   2026-01-01T06:00:00
   ```

2. **Con milisegundos (sin zona horaria):**
   ```
   2026-01-01T06:00:00.000
   ```

3. **Con zona horaria UTC (Z):**
   ```
   2026-01-01T06:00:00Z
   2026-01-01T06:00:00.000Z
   ```

4. **Con offset de zona horaria:**
   ```
   2026-01-01T06:00:00+00:00
   2026-01-01T06:00:00-05:00
   ```

### Nota sobre LocalDateTime
Los endpoints usan `LocalDateTime` en el backend, que NO almacena zona horaria. Cuando envías una fecha con zona horaria (como `Z` para UTC), el backend:
- Parsea la fecha correctamente
- Convierte a LocalDateTime descartando la zona horaria
- Asume que todas las fechas están en la misma zona horaria del servidor

## Ejemplos de Uso

### 1. KPI 1: Total de Clientes Registrados

**Endpoint:** `GET /api/dashboard/customers/total`

**Ejemplo 1 - Formato simple:**
```
http://localhost:8080/api/dashboard/customers/total?tenantId=24&from=2026-01-01T06:00:00&to=2026-02-19T04:54:49
```

**Ejemplo 2 - Con milisegundos:**
```
http://localhost:8080/api/dashboard/customers/total?tenantId=24&from=2026-01-01T06:00:00.000&to=2026-02-19T04:54:49.568
```

**Ejemplo 3 - Con zona horaria UTC (tu formato actual):**
```
http://localhost:8080/api/dashboard/customers/total?tenantId=24&from=2026-01-01T06:00:00.000Z&to=2026-02-19T04:54:49.568Z
```

**Respuesta:**
```json
200
```

---

### 2. KPI 2: Clientes Nuevos por Periodo

**Endpoint:** `GET /api/dashboard/customers/new-by-period`

**Parámetros:**
- `tenantId`: ID del tenant (Long)
- `period`: Periodo de agrupación (`day`, `week`, `month`)
- `from`: Fecha inicio
- `to`: Fecha fin

**Ejemplo:**
```
http://localhost:8080/api/dashboard/customers/new-by-period?tenantId=24&period=day&from=2026-01-01T00:00:00&to=2026-02-19T23:59:59
```

**Respuesta:**
```json
[
  {
    "period": "2026-01-01",
    "count": 15
  },
  {
    "period": "2026-01-02",
    "count": 23
  }
]
```

---

### 3. KPI 3: Estadísticas de Cupones

**Endpoint:** `GET /api/dashboard/coupons/stats`

**Ejemplo:**
```
http://localhost:8080/api/dashboard/coupons/stats?tenantId=24&from=2026-01-01T00:00:00&to=2026-02-19T23:59:59
```

---

### 4. KPI 5 y 6: Resumen de Ventas

**Endpoint:** `GET /api/dashboard/sales/summary`

**Ejemplo:**
```
http://localhost:8080/api/dashboard/sales/summary?tenantId=24&from=2026-01-01T00:00:00&to=2026-02-19T23:59:59
```

**Respuesta:**
```json
{
  "totalSales": 125000.50,
  "averageTicket": 850.75,
  "couponTransactions": 147
}
```

---

### 5. KPI 7: Rendimiento por Campaña

**Endpoint:** `GET /api/dashboard/campaigns/performance`

**Ejemplo:**
```
http://localhost:8080/api/dashboard/campaigns/performance?tenantId=24&from=2026-01-01T00:00:00&to=2026-02-19T23:59:59
```

---

## Endpoints de Métricas Comandix

### 6. Tasa de Recompra

**Endpoint:** `GET /api/dashboard/comandix/repeat-purchase-rate`

**Ejemplo:**
```
http://localhost:8080/api/dashboard/comandix/repeat-purchase-rate?tenantId=24&from=2026-01-01T00:00:00&to=2026-02-19T23:59:59
```

---

### 7. Ventas Identificadas vs Generales

**Endpoint:** `GET /api/dashboard/comandix/identified-vs-general`

**Ejemplo:**
```
http://localhost:8080/api/dashboard/comandix/identified-vs-general?tenantId=24&from=2026-01-01T00:00:00&to=2026-02-19T23:59:59
```

---

### 8. Customer Lifetime Value (LTV)

**Endpoint:** `GET /api/dashboard/comandix/customer-ltv`

**Parámetros adicionales:**
- `limit`: Límite de resultados (default: 50)

**Ejemplo:**
```
http://localhost:8080/api/dashboard/comandix/customer-ltv?tenantId=24&from=2026-01-01T00:00:00&to=2026-02-19T23:59:59&limit=100
```

---

### 9. Tasa de Conversión de Cupón

**Endpoint:** `GET /api/dashboard/comandix/coupon-conversion`

**Ejemplo:**
```
http://localhost:8080/api/dashboard/comandix/coupon-conversion?tenantId=24&from=2026-01-01T00:00:00&to=2026-02-19T23:59:59
```

---

### 10. Análisis de Personalización

**Endpoint:** `GET /api/dashboard/comandix/customization-analysis`

**Ejemplo:**
```
http://localhost:8080/api/dashboard/comandix/customization-analysis?tenantId=24&from=2026-01-01T00:00:00&to=2026-02-19T23:59:59
```

---

### 11. ROI por Campaña

**Endpoint:** `GET /api/dashboard/comandix/campaign-roi`

**Ejemplo:**
```
http://localhost:8080/api/dashboard/comandix/campaign-roi?tenantId=24&from=2026-01-01T00:00:00&to=2026-02-19T23:59:59
```

---

## Errores Comunes

### Error 400: Formato de Fecha Inválido

Si recibes este error, verifica que:
1. Las fechas sigan el formato ISO 8601
2. La fecha y hora estén separadas por `T`
3. Si incluyes zona horaria, use `Z` para UTC o `+HH:mm`/`-HH:mm` para otras zonas

**Mensaje de error típico:**
```json
{
  "code": 400,
  "message": "Formato de fecha inválido para el parámetro 'from'. Use formato ISO 8601: yyyy-MM-dd'T'HH:mm:ss (ejemplo: 2026-01-01T06:00:00) o yyyy-MM-dd'T'HH:mm:ss.SSS (ejemplo: 2026-01-01T06:00:00.000). También puede incluir zona horaria: 2026-01-01T06:00:00Z",
  "data": []
}
```

---

## Recomendaciones

### Para JavaScript/Frontend:

```javascript
// Opción 1: Formato simple sin zona horaria
const from = '2026-01-01T06:00:00';
const to = '2026-02-19T04:54:49';

// Opción 2: Desde Date object (remueve la Z al final)
const fromDate = new Date('2026-01-01T06:00:00.000Z');
const from = fromDate.toISOString().replace('Z', '');

// Opción 3: Mantener la Z (ahora también funciona)
const from = new Date('2026-01-01').toISOString(); // "2026-01-01T00:00:00.000Z"
```

### URL Encoding:

Si usas fetch o axios, los parámetros se codificarán automáticamente. Los dos puntos `:` se convertirán a `%3A`.

```javascript
const params = new URLSearchParams({
  tenantId: 24,
  from: '2026-01-01T06:00:00.000Z',
  to: '2026-02-19T04:54:49.568Z'
});

fetch(`http://localhost:8080/api/dashboard/customers/total?${params}`)
  .then(response => response.json())
  .then(data => console.log(data));
```

---

## Swagger UI

También puedes probar todos estos endpoints desde Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

Busca el tag **"Dashboard"** para ver todos los endpoints disponibles con sus ejemplos.
