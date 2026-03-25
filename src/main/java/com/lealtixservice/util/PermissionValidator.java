package com.lealtixservice.util;

import com.lealtixservice.service.RolePermissionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Utilidad para validación de permisos en tiempo de ejecución
 */
@Slf4j
@Component
@AllArgsConstructor
public class PermissionValidator {

    private final RolePermissionService rolePermissionService;

    /**
     * Valida que un rol tiene un permiso específico
     */
    public boolean hasPermission(String role, String permission) {
        return rolePermissionService.hasPermission(role, permission);
    }

    /**
     * Valida que un rol tiene al menos uno de los permisos especificados
     */
    public boolean hasAnyPermission(String role, String... permissions) {
        return rolePermissionService.hasAnyPermission(role, permissions);
    }

    /**
     * Valida que un rol tiene TODOS los permisos especificados
     */
    public boolean hasAllPermissions(String role, String... permissions) {
        return rolePermissionService.hasAllPermissions(role, permissions);
    }
}
