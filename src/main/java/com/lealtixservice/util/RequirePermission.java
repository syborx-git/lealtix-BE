package com.lealtixservice.util;

import java.lang.annotation.*;

/**
 * Anotación para validar permisos en endpoints
 * Puede usarse en combinación con @PreAuthorize
 * 
 * Ejemplo:
 * @RequirePermission("create_order")
 * public ResponseEntity<GenericResponse> createOrder(...) { }
 * 
 * @RequirePermission(value = "view_reports", alternative = {"manage_all"})
 * public ResponseEntity<GenericResponse> getReports(...) { }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    
    /**
     * Código de permiso principal requerido
     */
    String value();
    
    /**
     * Permisos alternativos (si se cumple cualquiera de estos, se permite)
     */
    String[] alternative() default {};
}
