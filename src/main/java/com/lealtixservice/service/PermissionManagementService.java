package com.lealtixservice.service;

import com.lealtixservice.dto.RolePermissionDTO;

import java.util.List;

public interface PermissionManagementService {
    
    /**
     * Obtener todos los permisos asignados a un rol
     */
    List<RolePermissionDTO> getPermissionsByRole(String role);
    
    /**
     * Obtener códigos de permisos asignados a un rol
     */
    List<String> getPermissionCodesByRole(String role);
    
    /**
     * Asignar un permiso a un rol
     */
    RolePermissionDTO assignPermissionToRole(String role, Long permissionId);
    
    /**
     * Asignar múltiples permisos a un rol
     */
    List<RolePermissionDTO> assignPermissionsToRole(String role, List<Long> permissionIds);
    
    /**
     * Reemplazar todos los permisos de un rol (eliminar actuales e insertar nuevos)
     */
    List<RolePermissionDTO> replaceRolePermissions(String role, List<Long> permissionIds);
    
    /**
     * Revocar permiso de un rol
     */
    void revokePermissionFromRole(String role, Long permissionId);
    
    /**
     * Revocar múltiples permisos de un rol
     */
    void revokePermissionsFromRole(String role, List<Long> permissionIds);
    
    /**
     * Verificar si un rol tiene un permiso específico
     */
    boolean roleHasPermission(String role, String permissionCode);
    
    /**
     * Listar todos los roles disponibles
     */
    List<String> getAllRoles();
}
