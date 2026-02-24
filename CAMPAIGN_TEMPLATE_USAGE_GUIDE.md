# Guía de Uso - Templates de Campaña con Estado de Uso

## 📋 Resumen de Funcionalidad

Se implementó la capacidad de detectar qué templates de campaña están **actualmente en uso** por un tenant específico, para que el frontend pueda deshabilitarlos y prevenir la creación de campañas duplicadas del mismo tipo.

---

## 🎯 Problema Resuelto

**Antes:** No había forma de saber si un template (ej. "Bienvenida") ya estaba siendo usado por una campaña activa del tenant.

**Ahora:** El endpoint retorna un flag `inUse` que indica si ese template tiene una campaña activa asociada en el período actual.

---

## 🔧 Cambios Implementados

### 1. **CampaignTemplateDTO** - Nuevo campo
```java
private Boolean inUse;  // true si el template está siendo usado por una campaña activa
```

### 2. **Nuevo Endpoint en CampaignTemplateController**

#### **GET** `/api/campaign-templates/tenant/{tenantId}`

**Descripción:** Lista todos los templates con información de si están en uso por el tenant especificado.

**Parámetros:**
- `tenantId` (Long): ID del tenant/negocio

**Respuesta:**
```json
{
  "status": 200,
  "message": "Templates obtenidos con estado de uso",
  "data": [
    {
      "id": 1,
      "name": "Bienvenida",
      "category": "General",
      "defaultTitle": "Bienvenido a nuestro negocio",
      "defaultSubtitle": "Disfruta de tu primera compra",
      "defaultDescription": "...",
      "defaultImageUrl": "...",
      "defaultPromoType": "DISCOUNT",
      "active": true,
      "inUse": true  // ⚠️ Este template YA está en uso activamente
    },
    {
      "id": 2,
      "name": "Cumpleaños",
      "category": "Especial",
      "defaultTitle": "¡Feliz cumpleaños!",
      "active": true,
      "inUse": false  // ✅ Este template está disponible
    }
  ],
  "total": 2
}
```

### 3. **Lógica de Detección de Uso**

Un template se marca como `inUse: true` cuando:
1. Existe una campaña con `status = ACTIVE`
2. La campaña está asociada a ese template
3. La campaña pertenece al `tenantId` especificado
4. **Y** la fecha actual está dentro del rango de la campaña:
   - `startDate` es null O hoy >= startDate
   - `endDate` es null O hoy <= endDate

---

## 📡 Ejemplos de Uso

### Caso 1: Listar templates para tenant 24
```bash
GET http://localhost:8080/api/campaign-templates/tenant/24
```

### Caso 2: Frontend deshabilitando templates en uso
```javascript
// Ejemplo React/Vue/Angular
templates.forEach(template => {
  if (template.inUse) {
    // Deshabilitar botón de "Crear campaña"
    // Mostrar badge "En uso"
    // Prevenir selección
  }
});
```

---

## 🧪 Escenarios de Prueba

### Escenario 1: Template sin campañas activas
- **Given:** Template "Bienvenida" existe pero no tiene campañas activas
- **When:** Llamo a `/api/campaign-templates/tenant/24`
- **Then:** `inUse: false`

### Escenario 2: Template con campaña activa dentro del período
- **Given:** 
  - Template "Bienvenida" con id=1
  - Campaña activa usando template 1
  - Hoy: 21 Feb 2026
  - Campaña: startDate=1 Jan 2026, endDate=31 Dec 2026
- **When:** Llamo a `/api/campaign-templates/tenant/24`
- **Then:** `inUse: true`

### Escenario 3: Template con campaña activa pero fuera del período
- **Given:**
  - Template "Navidad" con id=3
  - Campaña activa usando template 3
  - Hoy: 21 Feb 2026
  - Campaña: startDate=1 Dec 2025, endDate=31 Dec 2025 (ya pasó)
- **When:** Llamo a `/api/campaign-templates/tenant/24`
- **Then:** `inUse: false` (porque la campaña ya terminó)

### Escenario 4: Múltiples tenants
- **Given:**
  - Tenant 24 tiene campaña "Bienvenida" activa
  - Tenant 99 NO tiene campañas activas
- **When:** 
  - Llamo a `/api/campaign-templates/tenant/24` → `inUse: true`
  - Llamo a `/api/campaign-templates/tenant/99` → `inUse: false`
- **Then:** Cada tenant ve su propio estado de uso

---

## 🎨 Recomendaciones para el Frontend

### Opción 1: Deshabilitar completamente
```html
<button 
  :disabled="template.inUse" 
  @click="createCampaign(template)">
  {{ template.inUse ? 'En uso' : 'Crear campaña' }}
</button>
```

### Opción 2: Mostrar badge visual
```html
<div class="template-card">
  <h3>{{ template.name }}</h3>
  <span v-if="template.inUse" class="badge badge-warning">
    ⚠️ Actualmente en uso
  </span>
  <button v-else>Crear campaña</button>
</div>
```

### Opción 3: Tooltip informativo
```html
<Tooltip v-if="template.inUse" 
  text="Este template ya tiene una campaña activa. 
        Finaliza la campaña actual antes de crear una nueva.">
  <IconInfo />
</Tooltip>
```

---

## 🔄 Endpoint Original (sin cambios)

**GET** `/api/campaign-templates`

- Sigue funcionando igual
- **NO incluye** el flag `inUse` (siempre `null`)
- Útil para listar templates sin filtrar por tenant

---

## 🚀 Próximos Pasos Sugeridos

1. **Validación de negocio:** Prevenir creación de campañas si ya existe una activa del mismo tipo
2. **Endpoint de validación:** `POST /api/campaigns/validate-before-create` que verifique reglas de negocio
3. **Filtros adicionales:** Poder filtrar templates solo disponibles (`inUse: false`)
4. **Histórico:** Mostrar cuántas veces se ha usado cada template

---

## 📝 Notas Técnicas

- **Performance:** La query filtra en memoria después de obtener campañas activas. Para optimizar, se puede crear un query custom en el repository.
- **Caché:** Considerar cachear el resultado si se llama frecuentemente.
- **Lazy loading:** El template en Campaign es LAZY, se carga solo cuando se accede.

---

## ✅ Checklist de Integración

- [x] Backend implementado
- [ ] Frontend consume nuevo endpoint
- [ ] UI muestra badge "En uso"
- [ ] Botón deshabilitado cuando `inUse: true`
- [ ] Mensaje informativo al usuario
- [ ] Tests unitarios del frontend
- [ ] Tests de integración end-to-end

---

## 🐛 Troubleshooting

### Problema: `inUse` siempre es `false`
**Causa:** No hay campañas con `status = ACTIVE` en el rango de fechas actual.
**Solución:** Verifica que las campañas tengan:
- `status = 'ACTIVE'` (no 'DRAFT')
- `endDate` null o >= hoy
- `startDate` null o <= hoy

### Problema: `inUse` es `null`
**Causa:** Estás usando el endpoint `/api/campaign-templates` en lugar de `/api/campaign-templates/tenant/{tenantId}`.
**Solución:** Usa el endpoint con `tenantId` en la ruta.

### Problema: Tenant 24 no ve su campaña como "en uso"
**Causa:** La campaña puede tener `isDraft: true` o `status != ACTIVE`.
**Solución:** Activa la campaña con `POST /api/campaigns/{id}/activate`.

---

## 📞 Soporte

Si tienes dudas sobre esta funcionalidad, revisa:
1. Este documento
2. Código en `CampaignTemplateServiceImpl.findAllWithUsageStatus()`
3. Tests unitarios (próximamente)

---

**Última actualización:** 21 de febrero de 2026
