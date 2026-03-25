package com.lealtixservice.service.impl;

import com.lealtixservice.entity.UserPermission;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.repository.TenantUserRepository;
import com.lealtixservice.repository.UserPermissionRepository;
import com.lealtixservice.service.RolePermissionService;
import com.lealtixservice.service.UserPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class UserPermissionServiceImpl implements UserPermissionService {

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private TenantUserRepository tenantUserRepository;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Override
    public List<String> getUserCustomPermissions(Long userId) {
        log.info("Getting custom permissions for user: {}", userId);
        // Validar que el usuario existe
        tenantUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return userPermissionRepository.findByTenantUserId(userId).stream()
                .map(UserPermission::getPermission)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllUserPermissions(Long userId) {
        log.info("Getting all permissions (role + custom) for user: {}", userId);
        
        var user = tenantUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Obtener permisos del rol
        List<String> rolePermissions = rolePermissionService.getPermissionsByRole(user.getRol().name());

        // Obtener permisos personalizados
        List<String> customPermissions = getUserCustomPermissions(userId);

        // Combinar y remover duplicados
        return rolePermissions.stream()
                .distinct()
                .collect(Collectors.toList())
                .stream()
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void grantPermissionToUser(Long userId, String permissionCode) {
        log.info("Granting permission {} to user {}", permissionCode, userId);

        // Validar que el usuario existe
        var user = tenantUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Verificar que no exista ya
        if (userPermissionRepository.existsByTenantUserIdAndPermission(userId, permissionCode)) {
            log.warn("Permission {} already granted to user {}", permissionCode, userId);
            return;
        }

        UserPermission permission = UserPermission.builder()
                .tenantUser(user)
                .permission(permissionCode)
                .createdAt(LocalDateTime.now())
                .build();

        userPermissionRepository.save(permission);
        log.info("Permission {} granted to user {} successfully", permissionCode, userId);
    }

    @Override
    public void grantPermissionsToUser(Long userId, List<String> permissionCodes) {
        log.info("Granting {} permissions to user {}", permissionCodes.size(), userId);
        permissionCodes.forEach(code -> grantPermissionToUser(userId, code));
    }

    @Override
    public void revokePermissionFromUser(Long userId, String permissionCode) {
        log.info("Revoking permission {} from user {}", permissionCode, userId);

        UserPermission permission = userPermissionRepository
                .findByTenantUserIdAndPermission(userId, permissionCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario no tiene el permiso: " + permissionCode));

        userPermissionRepository.delete(permission);
        log.info("Permission {} revoked from user {} successfully", permissionCode, userId);
    }

    @Override
    public void revokePermissionsFromUser(Long userId, List<String> permissionCodes) {
        log.info("Revoking {} permissions from user {}", permissionCodes.size(), userId);
        permissionCodes.forEach(code -> {
            try {
                revokePermissionFromUser(userId, code);
            } catch (ResourceNotFoundException e) {
                log.warn("Permission {} not found for user {}", code, userId);
            }
        });
    }

    @Override
    public boolean userHasPermission(Long userId, String permissionCode) {
        List<String> permissions = getAllUserPermissions(userId);
        return permissions.contains(permissionCode);
    }

    @Override
    public void clearUserPermissions(Long userId) {
        log.info("Clearing all custom permissions for user: {}", userId);
        // Validar que el usuario existe
        tenantUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        userPermissionRepository.deleteByTenantUserId(userId);
        log.info("Custom permissions cleared for user {} successfully", userId);
    }
}
