package com.lealtixservice.service;

import com.lealtixservice.dto.PermissionDTO;
import com.lealtixservice.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PermissionService {
    
    /**
     * Crear nuevo permiso
     */
    PermissionDTO create(PermissionDTO permissionDTO);
    
    /**
     * Obtener permiso por ID
     */
    PermissionDTO getById(Long id);
    
    /**
     * Obtener permiso por código
     */
    Optional<PermissionDTO> getByCode(String code);
    
    /**
     * Listar todos los permisos
     */
    List<PermissionDTO> listAll();
    
    /**
     * Listar permisos con paginación
     */
    Page<PermissionDTO> listPaginated(Pageable pageable);
    
    /**
     * Obtener permisos por recurso
     */
    List<PermissionDTO> getByResource(String resource);
    
    /**
     * Obtener permisos por categoría
     */
    List<PermissionDTO> getByCategory(String category);
    
    /**
     * Actualizar permiso
     */
    PermissionDTO update(Long id, PermissionDTO permissionDTO);
    
    /**
     * Eliminar permiso
     */
    void delete(Long id);
    
    /**
     * Obtener entidad Permission (uso interno)
     */
    Permission getPermissionEntity(Long id);
}
