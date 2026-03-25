package com.lealtixservice.specification;

import com.lealtixservice.entity.TenantCustomer;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Especificaciones (predicados dinámicos) para filtrar TenantCustomer.
 * Permite construir queries dinámicas combinando múltiples filtros con Specification.where().and().
 * 
 * Patrón: JPA Specifications para consultas seguras y reutilizables.
 * Extensible: fácil de agregar nuevos filtros sin modificar el repositorio.
 */
public final class TenantCustomerSpecifications {
    
    private TenantCustomerSpecifications() {
        // Clase utilitaria, no se instancia
    }

    /**
     * Filtra clientes por tenant ID.
     * Crucial para multi-tenancy: garantiza que solo se devuelvan clientes del tenant correcto.
     * 
     * @param tenantId ID del tenant
     * @return Specification para tenant_id = ?
     */
    public static Specification<TenantCustomer> byTenantId(Long tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenant").get("id"), tenantId);
    }

    /**
     * Filtra clientes que aceptaron recibir promociones.
     * Requisito de negocio: respetar el consentimiento del usuario.
     * 
     * @return Specification para accepted_promotions = true
     */
    public static Specification<TenantCustomer> acceptedPromotions() {
        return (root, query, cb) -> cb.isTrue(root.get("acceptedPromotions"));
    }

    /**
     * Filtra clientes por género específico.
     * Usado para segmentación: "male", "female".
     * 
     * @param gender Valor de género (ej. "male", "female")
     * @return Specification para gender = ?
     */
    public static Specification<TenantCustomer> byGender(String gender) {
        return (root, query, cb) -> {
            if (gender == null || gender.isBlank()) {
                return cb.conjunction(); // Sin filtro si gender es nulo
            }
            return cb.equal(root.get("gender"), gender);
        };
    }

    /**
     * Filtra clientes registrados en los últimos N días.
     * Usado para segmentación: "new_30d".
     * 
     * @param days Número de días hacia atrás
     * @return Specification para created_at >= (now - N días)
     */
    public static Specification<TenantCustomer> createdWithinDays(int days) {
        return (root, query, cb) -> {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
            return cb.greaterThanOrEqualTo(root.get("createdAt"), cutoff);
        };
    }

    /**
     * Filtra clientes con cumpleaños en los próximos N días (basado en mes/día).
     * Usado para segmentación: "upcoming_birthday_7d".
     * 
     * Nota: Compara mes y día del cumpleaños con el rango actual.
     * Maneja correctamente rangos que cruzan fin de año (ej. 28 dic - 7 ene).
     * 
     * @param days Número de días hacia adelante
     * @return Specification para cumpleaños en el rango (hoy, hoy + N días)
     */
    public static Specification<TenantCustomer> birthdayInNextDays(int days) {
        return (root, query, cb) -> {
            // Obtener campo birth_date
            Path<LocalDate> birthDate = root.get("birthDate");
            
            // Solo filtrar registros con birthDate no nulo
            Predicate notNull = cb.isNotNull(birthDate);
            
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusDays(days);
            
            // Extraer mes y día (algunas BD usan EXTRACT(MONTH FROM ...), otras function("month", ...))
            // Para portabilidad, usamos function() que funciona en PostgreSQL y MySQL
            Expression<Integer> monthExpr = cb.function("month", Integer.class, birthDate);
            Expression<Integer> dayExpr = cb.function("day", Integer.class, birthDate);
            
            int startMonth = today.getMonthValue();
            int startDay = today.getDayOfMonth();
            int endMonth = endDate.getMonthValue();
            int endDay = endDate.getDayOfMonth();
            
            Predicate rangePredicate;
            
            if (startMonth == endMonth) {
                // El rango está dentro del mismo mes
                rangePredicate = cb.and(
                    cb.equal(monthExpr, startMonth),
                    cb.between(dayExpr, startDay, endDay)
                );
            } else if (startMonth < endMonth) {
                // El rango abarca dos meses consecutivos (sin cruzar fin de año)
                Predicate startMonthRange = cb.and(
                    cb.equal(monthExpr, startMonth),
                    cb.greaterThanOrEqualTo(dayExpr, startDay)
                );
                Predicate endMonthRange = cb.and(
                    cb.equal(monthExpr, endMonth),
                    cb.lessThanOrEqualTo(dayExpr, endDay)
                );
                rangePredicate = cb.or(startMonthRange, endMonthRange);
            } else {
                // El rango cruza fin de año (ej. 28 dic - 7 ene)
                // startMonth > endMonth es el caso: start en dic (12), end en ene (1)
                Predicate endYearRange = cb.and(
                    cb.equal(monthExpr, startMonth),
                    cb.greaterThanOrEqualTo(dayExpr, startDay)
                );
                Predicate nextYearRange = cb.and(
                    cb.equal(monthExpr, endMonth),
                    cb.lessThanOrEqualTo(dayExpr, endDay)
                );
                rangePredicate = cb.or(endYearRange, nextYearRange);
            }
            
            return cb.and(notNull, rangePredicate);
        };
    }

    /**
     * Filtra clientes con LTV (Lifetime Value) superior a un umbral.
     * Usado para segmentación: "high_ltv".
     * 
     * NOTA: Requiere que TenantCustomer tenga un campo 'ltv' de tipo BigDecimal/Double.
     * Actualmente no existe en la entidad, este método es un placeholder para extensión futura.
     * 
     * Ejemplo de implementación cuando se agregue el campo:
     * <pre>
     * public static Specification<TenantCustomer> ltvGreaterThan(BigDecimal threshold) {
     *     return (root, query, cb) -> cb.greaterThan(root.get("ltv"), threshold);
     * }
     * </pre>
     */

    /**
     * Filtra clientes sin compras en los últimos N días.
     * Usado para segmentación: "no_purchase_60d".
     * 
     * NOTA: Requiere que TenantCustomer tenga un campo 'lastPurchaseAt' de tipo LocalDateTime.
     * Actualmente no existe en la entidad, este método es un placeholder para extensión futura.
     * 
     * Ejemplo de implementación cuando se agregue el campo:
     * <pre>
     * public static Specification<TenantCustomer> lastPurchaseBeforeDays(int days) {
     *     return (root, query, cb) -> {
     *         LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
     *         return cb.lessThanOrEqualTo(root.get("lastPurchaseAt"), cutoff);
     *     };
     * }
     * </pre>
     */

    /**
     * Filtra clientes activos en los últimos N días.
     * Usado para segmentación: "active_30d".
     * 
     * NOTA: Requiere que TenantCustomer tenga un campo 'lastActiveAt' o similar.
     * Por ahora, se podría usar 'updatedAt' como proxy de última actividad.
     * 
     * @param days Número de días hacia atrás
     * @return Specification para last_active_at >= (now - N días)
     */
    public static Specification<TenantCustomer> activeWithinDays(int days) {
        return (root, query, cb) -> {
            // Usando updatedAt como proxy de última actividad
            // Cuando se agregue un campo lastActiveAt específico, reemplazar esta lógica
            LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
            return cb.greaterThanOrEqualTo(root.get("updatedAt"), cutoff);
        };
    }

    /**
     * Filtra clientes con estatus VIP.
     * Usado para segmentación: "vip".
     * 
     * NOTA: Requiere que TenantCustomer tenga un campo 'isVip' de tipo boolean.
     * Actualmente no existe en la entidad, este método es un placeholder para extensión futura.
     * 
     * Ejemplo de implementación cuando se agregue el campo:
     * <pre>
     * public static Specification<TenantCustomer> isVip() {
     *     return (root, query, cb) -> cb.isTrue(root.get("isVip"));
     * }
     * </pre>
     */
}
