package com.lealtixservice.service.impl;

import com.lealtixservice.dto.PermissionDTO;
import com.lealtixservice.dto.RolePermissionDTO;
import com.lealtixservice.entity.Permission;
import com.lealtixservice.entity.RolePermission;
import com.lealtixservice.enums.RoleEnum;
import com.lealtixservice.exception.BusinessRuleException;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.repository.PermissionRepository;
import com.lealtixservice.repository.RolePermissionRepository;
import com.lealtixservice.service.PermissionManagementService;
import com.lealtixservice.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class PermissionManagementServiceImpl implements PermissionManagementService {

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PermissionService permissionService;

    @Override
    @Cacheable(value = "rolePermissionsDTO", key = "#role")
    public List<RolePermissionDTO> getPermissionsByRole(String role) {
        log.info("Getting permissions for role: {}", role);
        validateRole(role);

        return rolePermissionRepository.findByRole(role).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "rolePermissionCodes", key = "#role")
    public List<String> getPermissionCodesByRole(String role) {
        log.info("Getting permission codes for role: {}", role);
        validateRole(role);

        return rolePermissionRepository.findByRole(role).stream()
                .map(rp -> rp.getPermission().getCode())
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = {"rolePermissionsDTO", "rolePermissionCodes", "rolePermissions"}, key = "#role", allEntries = false)
    public RolePermissionDTO assignPermissionToRole(String role, Long permissionId) {
        log.info("Assigning permission {} to role {}", permissionId, role);
        validateRole(role);

        // Validar que el permiso existe
        Permission permission = permissionService.getPermissionEntity(permissionId);

        // Validar que no esté ya asignado
        if (rolePermissionRepository.existsByRoleAndPermissionId(role, permissionId)) {
            log.warn("Permission {} already assigned to role {}", permissionId, role);
            return rolePermissionRepository.findByRoleAndPermissionId(role, permissionId)
                    .map(this::toDTO)
                    .orElseThrow();
        }

        RolePermission rolePermission = RolePermission.builder()
                .role(role)
                .permission(permission)
                .grantedAt(LocalDateTime.now())
                .build();

        RolePermission saved = rolePermissionRepository.save(rolePermission);
        log.info("Permission {} assigned to role {} successfully", permissionId, role);

        return toDTO(saved);
    }

    @Override
    @CacheEvict(value = {"rolePermissionsDTO", "rolePermissionCodes", "rolePermissions"}, key = "#role", allEntries = false)
    public List<RolePermissionDTO> assignPermissionsToRole(String role, List<Long> permissionIds) {
        log.info("Assigning {} permissions to role {}", permissionIds.size(), role);
        validateRole(role);

        List<RolePermissionDTO> assigned = permissionIds.stream()
                .map(permissionId -> assignPermissionToRole(role, permissionId))
                .collect(Collectors.toList());

        log.info("Permissions assigned to role {} successfully", role);
        return assigned;
    }

    @Override
    @CacheEvict(value = {"rolePermissionsDTO", "rolePermissionCodes", "rolePermissions"}, key = "#role", allEntries = false)
    public List<RolePermissionDTO> replaceRolePermissions(String role, List<Long> permissionIds) {
        log.info("Replacing all permissions for role: {}", role);
        validateRole(role);

        // Obtener permisos actuales
        List<RolePermission> currentPermissions = rolePermissionRepository.findByRole(role);

        // Eliminar todos los actuales
        if (!currentPermissions.isEmpty()) {
            currentPermissions.forEach(rolePermissionRepository::delete);
            log.info("Removed {} current permissions from role {}", currentPermissions.size(), role);
        }

        // Asignar nuevos
        List<RolePermissionDTO> newPermissions = assignPermissionsToRole(role, permissionIds);
        log.info("Replaced permissions for role {} successfully", role);

        return newPermissions;
    }

    @Override
    @CacheEvict(value = {"rolePermissionsDTO", "rolePermissionCodes", "rolePermissions"}, key = "#role", allEntries = false)
    public void revokePermissionFromRole(String role, Long permissionId) {
        log.info("Revoking permission {} from role {}", permissionId, role);
        validateRole(role);

        RolePermission rolePermission = rolePermissionRepository.findByRoleAndPermissionId(role, permissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Permiso " + permissionId + " no está asignado al rol " + role));

        rolePermissionRepository.delete(rolePermission);
        log.info("Permission {} revoked from role {} successfully", permissionId, role);
    }

    @Override
    @CacheEvict(value = {"rolePermissionsDTO", "rolePermissionCodes", "rolePermissions"}, key = "#role", allEntries = false)
    public void revokePermissionsFromRole(String role, List<Long> permissionIds) {
        log.info("Revoking {} permissions from role {}", permissionIds.size(), role);
        validateRole(role);

        permissionIds.forEach(permissionId -> {
            try {
                revokePermissionFromRole(role, permissionId);
            } catch (ResourceNotFoundException e) {
                log.warn("Permission {} was not assigned to role {}", permissionId, role);
            }
        });

        log.info("Permissions revoked from role {} successfully", role);
    }

    @Override
    public boolean roleHasPermission(String role, String permissionCode) {
        return getPermissionCodesByRole(role).contains(permissionCode);
    }

    @Override
    public List<String> getAllRoles() {
        return Arrays.stream(RoleEnum.values())
                .map(RoleEnum::name)
                .collect(Collectors.toList());
    }

    private void validateRole(String role) {
        try {
            RoleEnum.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Rol inválido: " + role);
        }
    }

    private RolePermissionDTO toDTO(RolePermission rolePermission) {
        PermissionDTO permissionDTO = permissionService.getById(rolePermission.getPermission().getId());

        return RolePermissionDTO.builder()
                .id(rolePermission.getId())
                .role(rolePermission.getRole())
                .permission(permissionDTO)
                .grantedAt(rolePermission.getGrantedAt())
                .build();
    }
}
