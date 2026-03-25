package com.lealtixservice.service;

import java.util.List;

public interface UserPermissionService {
    
    /**
     * Obtener permisos personalizados de un usuario (además de los del rol)
     */
    List<String> getUserCustomPermissions(Long userId);
    
    /**
     * Obtener todos los permisos efectivos de un usuario (rol + personalizados)
     */
    List<String> getAllUserPermissions(Long userId);
    
    /**
     * Asignar un permiso personalizado a un usuario
     */
    void grantPermissionToUser(Long userId, String permissionCode);
    
    /**
     * Asignar múltiples permisos personalizados a un usuario
     */
    void grantPermissionsToUser(Long userId, List<String> permissionCodes);
    
    /**
     * Revocar un permiso personalizado de un usuario
     */
    void revokePermissionFromUser(Long userId, String permissionCode);
    
    /**
     * Revocar múltiples permisos personalizados de un usuario
     */
    void revokePermissionsFromUser(Long userId, List<String> permissionCodes);
    
    /**
     * Verificar si un usuario tiene un permiso específico (considerando rol + personalizados)
     */
    boolean userHasPermission(Long userId, String permissionCode);
    
    /**
     * Limpiar todos los permisos personalizados de un usuario
     */
    void clearUserPermissions(Long userId);
}
