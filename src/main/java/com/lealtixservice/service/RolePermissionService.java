package com.lealtixservice.service;

import com.lealtixservice.enums.RoleEnum;
import com.lealtixservice.repository.RolePermissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RolePermissionService {

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    /**
     * Obtiene los permisos de un rol desde la base de datos
     * Intenta desde BD primero, fallback a enum si no existen
     */
    @Cacheable(value = "rolePermissions", key = "#roleName")
    public List<String> getPermissionsByRole(String roleName) {
        try {
            String upperRole = roleName.toUpperCase();
            
            // Intentar obtener de BD
            var rolePermissions = rolePermissionRepository.findByRole(upperRole);
            
            if (!rolePermissions.isEmpty()) {
                return rolePermissions.stream()
                        .map(rp -> rp.getPermission().getCode())
                        .collect(Collectors.toList());
            }
            
            // Fallback a enum para roles existentes (backwards compatibility)
            RoleEnum role = RoleEnum.valueOf(upperRole);
            return role.getPermissions();
        } catch (IllegalArgumentException e) {
            log.warn("Rol no encontrado: {}", roleName);
            return List.of();
        }
    }

    /**
     * Obtiene los permisos desde el enum (usado durante inicialización)
     */
    public List<String> getPermissionsByRole(RoleEnum role) {
        return role.getPermissions();
    }

    /**
     * Valida si un rol tiene un permiso específico
     */
    public boolean hasPermission(String roleName, String permission) {
        List<String> permissions = getPermissionsByRole(roleName);
        return permissions.contains(permission);
    }

    /**
     * Valida si un rol tiene alguno de los permisos especificados
     */
    public boolean hasAnyPermission(String roleName, String... permissions) {
        List<String> rolePermissions = getPermissionsByRole(roleName);
        for (String permission : permissions) {
            if (rolePermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Valida si un rol tiene todos los permisos especificados
     */
    public boolean hasAllPermissions(String roleName, String... permissions) {
        List<String> rolePermissions = getPermissionsByRole(roleName);
        for (String permission : permissions) {
            if (!rolePermissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }
}
