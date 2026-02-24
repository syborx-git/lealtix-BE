# 📊 DOCUMENTACIÓN API - MÉTRICAS DE FIDELIZACIÓN COMANDIX

## Resumen de Implementación

Se han implementado **6 nuevos endpoints** para métricas avanzadas de fidelización en el módulo Comandix, enfocados en medir recompra, LTV, conversión de cupones y análisis de personalización.

---

## 🎯 ENDPOINTS NUEVOS IMPLEMENTADOS

### 1. **Tasa de Recompra (Repeat Purchase Rate)**

**Endpoint:** `GET /api/dashboard/comandix/repeat-purchase-rate`

**Descripción:** Calcula el porcentaje de clientes identificados que han realizado más de una compra.

**Parámetros:**
```
tenantId: Long (requerido) - ID del tenant
from: LocalDateTime (requerido) - Fecha inicio (formato: 2024-01-01T00:00:00)
to: LocalDateTime (requerido) - Fecha fin (formato: 2024-12-31T23:59:59)
```

**Ejemplo Request:**
```
GET /api/dashboard/comandix/repeat-purchase-rate?tenantId=1&from=2024-01-01T00:00:00&to=2024-12-31T23:59:59
```

**Respuesta (200 OK):**
```json
{
  "totalCustomers": 150,
  "repeatCustomers": 85,
  "repeatRate": 56.67,
  "oneTimeBuyers": 65,
  "multiTimeBuyers": 85
}
```

**Campos de Respuesta:**
- `totalCustomers`: Total de clientes únicos con al menos una orden
- `repeatCustomers`: Clientes con más de una orden
- `repeatRate`: Porcentaje de recompra (%)
- `oneTimeBuyers`: Clientes con solo una compra
- `multiTimeBuyers`: Clientes con múltiples compras

---

### 2. **Ventas Identificadas vs Generales**

**Endpoint:** `GET /api/dashboard/comandix/identified-vs-general`

**Descripción:** Compara ingresos y transacciones entre clientes registrados (identificados) y ventas anónimas (generales).

**Parámetros:**
```
tenantId: Long (requerido)
from: LocalDateTime (requerido)
to: LocalDateTime (requerido)
```

**Ejemplo Request:**
```
GET /api/dashboard/comandix/identified-vs-general?tenantId=1&from=2024-01-01T00:00:00&to=2024-12-31T23:59:59
```

**Respuesta (200 OK):**
```json
{
  "identifiedOrdersCount": 320,
  "identifiedRevenue": 45600.50,
  "identifiedAvgTicket": 142.50,
  "generalOrdersCount": 180,
  "generalRevenue": 12300.00,
  "generalAvgTicket": 68.33,
  "identifiedPercentage": 78.75,
  "generalPercentage": 21.25
}
```

**Campos de Respuesta:**
- `identifiedOrdersCount`: Número de órdenes con cliente identificado
- `identifiedRevenue`: Ingresos totales de clientes registrados
- `identifiedAvgTicket`: Ticket promedio de ventas identificadas
- `generalOrdersCount`: Número de órdenes sin cliente (ventas generales)
- `generalRevenue`: Ingresos de ventas anónimas
- `generalAvgTicket`: Ticket promedio de ventas generales
- `identifiedPercentage`: Porcentaje de ingresos identificados
- `generalPercentage`: Porcentaje de ingresos generales

---

### 3. **LTV - Customer Lifetime Value**

**Endpoint:** `GET /api/dashboard/comandix/customer-ltv`

**Descripción:** Lista de clientes ordenados por valor total generado (top clientes más valiosos).

**Parámetros:**
```
tenantId: Long (requerido)
from: LocalDateTime (requerido)
to: LocalDateTime (requerido)
limit: Integer (opcional, default: 50) - Top N clientes
```

**Ejemplo Request:**
```
GET /api/dashboard/comandix/customer-ltv?tenantId=1&from=2024-01-01T00:00:00&to=2024-12-31T23:59:59&limit=10
```

**Respuesta (200 OK):**
```json
[
  {
    "customerId": 45,
    "customerName": "Juan Pérez",
    "customerEmail": "juan.perez@example.com",
    "lifetimeValue": 8750.00,
    "totalOrders": 35,
    "averageOrderValue": 250.00,
    "firstPurchase": "2024-01-15T10:30:00",
    "lastPurchase": "2024-12-20T18:45:00"
  },
  {
    "customerId": 78,
    "customerName": "María García",
    "customerEmail": "maria.garcia@example.com",
    "lifetimeValue": 6200.50,
    "totalOrders": 28,
    "averageOrderValue": 221.45,
    "firstPurchase": "2024-02-01T12:00:00",
    "lastPurchase": "2024-12-18T16:20:00"
  }
]
```

**Campos de Respuesta:**
- `customerId`: ID del cliente
- `customerName`: Nombre del cliente
- `customerEmail`: Email del cliente
- `lifetimeValue`: Valor total generado por el cliente
- `totalOrders`: Número total de órdenes
- `averageOrderValue`: Valor promedio por orden
- `firstPurchase`: Fecha de primera compra
- `lastPurchase`: Fecha de última compra

---

### 4. **Tasa de Conversión de Cupón**

**Endpoint:** `GET /api/dashboard/comandix/coupon-conversion`

**Descripción:** Relación entre cupones emitidos, redimidos y usados en órdenes por campaña.

**Parámetros:**
```
tenantId: Long (requerido)
from: LocalDateTime (requerido)
to: LocalDateTime (requerido)
```

**Ejemplo Request:**
```
GET /api/dashboard/comandix/coupon-conversion?tenantId=1&from=2024-01-01T00:00:00&to=2024-12-31T23:59:59
```

**Respuesta (200 OK):**
```json
[
  {
    "campaignId": 12,
    "campaignName": "Descuento Navideño 2024",
    "totalCouponsIssued": 500,
    "totalCouponsRedeemed": 380,
    "ordersWithCoupon": 320,
    "conversionRate": 64.00,
    "revenueFromCoupons": 28400.00
  },
  {
    "campaignId": 15,
    "campaignName": "Black Friday",
    "totalCouponsIssued": 1000,
    "totalCouponsRedeemed": 720,
    "ordersWithCoupon": 680,
    "conversionRate": 68.00,
    "revenueFromCoupons": 52300.50
  }
]
```

**Campos de Respuesta:**
- `campaignId`: ID de la campaña
- `campaignName`: Nombre de la campaña
- `totalCouponsIssued`: Total de cupones emitidos
- `totalCouponsRedeemed`: Cupones redimidos (marcados como REDEEMED)
- `ordersWithCoupon`: Órdenes que usaron el cupón
- `conversionRate`: Tasa de conversión (órdenes/cupones emitidos %)
- `revenueFromCoupons`: Ingresos generados por órdenes con cupón

---

### 5. **Análisis de Personalización**

**Endpoint:** `GET /api/dashboard/comandix/customization-analysis`

**Descripción:** Frecuencia de términos en comentarios de items para identificar tendencias de gustos y preferencias.

**Parámetros:**
```
tenantId: Long (requerido)
from: LocalDateTime (requerido)
to: LocalDateTime (requerido)
```

**Ejemplo Request:**
```
GET /api/dashboard/comandix/customization-analysis?tenantId=1&from=2024-01-01T00:00:00&to=2024-12-31T23:59:59
```

**Respuesta (200 OK):**
```json
[
  {
    "keyword": "sin",
    "frequency": 245,
    "percentage": 32.5
  },
  {
    "keyword": "extra",
    "frequency": 189,
    "percentage": 25.1
  },
  {
    "keyword": "cebolla",
    "frequency": 134,
    "percentage": 17.8
  },
  {
    "keyword": "picante",
    "frequency": 98,
    "percentage": 13.0
  },
  {
    "keyword": "salsa",
    "frequency": 87,
    "percentage": 11.6
  }
]
```

**Campos de Respuesta:**
- `keyword`: Palabra clave encontrada en comentarios
- `frequency`: Número de veces que aparece
- `percentage`: Porcentaje respecto al total de comentarios

**Palabras clave analizadas:**
- sin, con, extra, poco, mucho
- caliente, frío, fría
- cebolla, tomate, lechuga, queso
- salsa, mayo, mayonesa
- picante, no picante
- crudo, cocido, aparte
- solo, sólo

---

### 6. **ROI por Campaña**

**Endpoint:** `GET /api/dashboard/comandix/campaign-roi`

**Descripción:** Calcula el retorno de inversión (ROI) por campaña: (Ingresos - Costo) / Costo * 100

**Parámetros:**
```
tenantId: Long (requerido)
from: LocalDateTime (requerido)
to: LocalDateTime (requerido)
```

**Ejemplo Request:**
```
GET /api/dashboard/comandix/campaign-roi?tenantId=1&from=2024-01-01T00:00:00&to=2024-12-31T23:59:59
```

**Respuesta (200 OK):**
```json
[
  {
    "campaignId": 12,
    "campaignName": "Descuento Navideño 2024",
    "campaignCost": 5000.00,
    "revenueGenerated": 28400.00,
    "profit": 23400.00,
    "roi": 468.00,
    "ordersCount": 320
  },
  {
    "campaignId": 15,
    "campaignName": "Black Friday",
    "campaignCost": 8500.00,
    "revenueGenerated": 52300.50,
    "profit": 43800.50,
    "roi": 515.30,
    "ordersCount": 680
  }
]
```

**Campos de Respuesta:**
- `campaignId`: ID de la campaña
- `campaignName`: Nombre de la campaña
- `campaignCost`: Costo estimado de la campaña
- `revenueGenerated`: Ingresos totales generados
- `profit`: Ganancia (ingresos - costo)
- `roi`: Retorno de inversión en porcentaje
- `ordersCount`: Número de órdenes con cupón de esta campaña

---

## 📋 CAMBIOS EN LA BASE DE DATOS

### Script de Migración: V13__add_comandix_loyalty_metrics_support.sql

```sql
-- 1. Hacer nullable customer_id en client_order (soporte ventas generales)
ALTER TABLE client_order ALTER COLUMN customer_id DROP NOT NULL;

-- 2. Agregar coupon_id a client_order
ALTER TABLE client_order ADD COLUMN coupon_id BIGINT;

-- 3. Agregar índices
CREATE INDEX idx_client_order_coupon_id ON client_order(coupon_id);
CREATE INDEX idx_client_order_tenant_coupon ON client_order(tenant_id, coupon_id);

-- 4. Agregar estimated_cost a campaign
ALTER TABLE campaign ADD COLUMN estimated_cost NUMERIC(10, 2);
```

---

## 🔧 ARCHIVOS CREADOS Y MODIFICADOS

### Nuevos DTOs Creados:
1. `RepeatPurchaseRateDTO.java`
2. `IdentifiedVsGeneralSalesDTO.java`
3. `CustomerLTVDTO.java`
4. `CouponConversionRateDTO.java`
5. `CustomizationAnalysisDTO.java`
6. `CampaignROIDTO.java`

### Entidades Modificadas:
1. `ClientOrder.java` - Agregado campo `couponId` y `customer` nullable
2. `Campaign.java` - Agregado campo `estimatedCost`

### Repositorios Modificados:
1. `ClientOrderRepository.java` - 6 nuevas queries con @Query
2. `ClientOrderItemRepository.java` - 3 nuevas queries para análisis

### Servicios Modificados:
1. `DashboardService.java` - Interface con 6 nuevos métodos
2. `DashboardServiceImpl.java` - Implementación de los 6 métodos

### Controllers Modificados:
1. `DashboardController.java` - 6 nuevos endpoints REST

---

## 💡 VALOR DE NEGOCIO

### 1. **Tasa de Recompra**
- **Por qué importa:** Es la métrica reina de fidelización. Si los clientes vuelven, las campañas funcionan.
- **Acción:** Si la tasa es baja (<30%), activar campañas de retención.

### 2. **Ventas Identificadas vs Generales**
- **Por qué importa:** Mide si el personal está capturando correctamente los datos del cliente en el punto de venta.
- **Acción:** Si >50% son ventas generales, capacitar al equipo en registro de clientes.

### 3. **LTV (Customer Lifetime Value)**
- **Por qué importa:** Identifica los clientes más valiosos para crear campañas VIP personalizadas.
- **Acción:** Diseñar recompensas especiales para el top 10% de clientes.

### 4. **Conversión de Cupón**
- **Por qué importa:** Mide la efectividad de las campañas promocionales.
- **Acción:** Si la conversión es baja, revisar atractivo de la oferta o canales de distribución.

### 5. **Análisis de Personalización**
- **Por qué importa:** Revela preferencias reales de los clientes ("sin cebolla", "extra queso").
- **Acción:** Crear menús personalizados o sugerencias automáticas basadas en historial.

### 6. **ROI por Campaña**
- **Por qué importa:** Justifica la inversión en marketing y campañas.
- **Acción:** Replicar campañas con ROI >200%, descontinuar las de ROI <50%.

---

## 🚀 PRÓXIMOS PASOS

1. **Ejecutar migración Flyway:** El script V13 se ejecutará automáticamente al iniciar la aplicación.
2. **Probar endpoints:** Usar Swagger UI en `/swagger-ui.html`
3. **Poblar campo estimatedCost:** Actualizar campañas existentes con su costo estimado.
4. **Integrar con Frontend:** Consumir estos endpoints desde el dashboard.
5. **Configurar alertas:** Notificar cuando métricas críticas (ej. tasa recompra) caigan.

---

## 📚 QUERIES IMPLEMENTADAS

### Tasa de Recompra:
```java
@Query("SELECT COUNT(DISTINCT o.customer.id) FROM ClientOrder o ...")
```

### Ventas Identificadas vs Generales:
```java
@Query("SELECT COUNT(o), SUM(o.total), AVG(o.total) WHERE o.customer IS NOT NULL ...")
@Query("SELECT COUNT(o), SUM(o.total), AVG(o.total) WHERE o.customer IS NULL ...")
```

### LTV:
```java
@Query("SELECT o.customer.id, SUM(o.total), COUNT(o.id), AVG(o.total), MIN(o.fecha), MAX(o.fecha) ...")
```

### Conversión Cupón:
```java
@Query("SELECT c.campaign.id, COUNT(c.id), COUNT(o.id), SUM(o.total) FROM Coupon c LEFT JOIN ClientOrder o ...")
```

### Personalización:
```java
@Query("SELECT i.comentarios FROM ClientOrderItem i WHERE i.comentarios IS NOT NULL ...")
```

---

**Desarrollado con ❤️ para Lealtix - SaaS de Fidelización**
