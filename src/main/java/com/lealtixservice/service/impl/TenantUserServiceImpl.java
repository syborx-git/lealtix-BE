package com.lealtixservice.service.impl;

import com.lealtixservice.dto.*;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantUser;
import com.lealtixservice.enums.RoleEnum;
import com.lealtixservice.exception.BusinessRuleException;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.mapper.TenantUserMapper;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.repository.TenantUserRepository;
import com.lealtixservice.service.RolePermissionService;
import com.lealtixservice.service.TenantUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class TenantUserServiceImpl implements TenantUserService {

    @Autowired
    private TenantUserRepository tenantUserRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Override
    public UserDTO createUser(CreateUserRequest request, String createdBy) {
        log.info("Creating user: {} for tenant: {}", request.getEmail(), request.getTenantId());

        // Validar que el tenant existe
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado"));

        // Validar que el email no existe para este tenant
        if (tenantUserRepository.existsByEmailAndTenantId(request.getEmail(), request.getTenantId())) {
            throw new BusinessRuleException("El email ya está registrado en este tenant");
        }

        // Validar que el rol existe
        try {
            RoleEnum.valueOf(request.getRol().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Rol inválido: " + request.getRol());
        }

        // Crear el usuario
        String passwordHash = passwordEncoder.encode(request.getContrasena());
        TenantUser user = TenantUserMapper.toEntity(request, passwordHash);
        user.setTenant(tenant);
        user.setCreatedBy(createdBy);
        user.setUpdatedBy(createdBy);

        TenantUser savedUser = tenantUserRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        // Obtener permisos del rol
        List<String> permissions = rolePermissionService.getPermissionsByRole(request.getRol());

        return TenantUserMapper.toDTO(savedUser, permissions);
    }

    @Override
    public UserDTO updateUser(Long id, Long tenantId, UpdateUserRequest request, String updatedBy) {
        log.info("Updating user: {} for tenant: {}", id, tenantId);

        TenantUser user = tenantUserRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Si se actualiza el email, validar que no exista otro con ese email en el tenant
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (tenantUserRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
                throw new BusinessRuleException("El email ya está registrado en este tenant");
            }
        }

        // Aplicar cambios
        TenantUserMapper.updateEntity(request, user);

        // Si se actualiza la contraseña
        if (request.getContrasena() != null && !request.getContrasena().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getContrasena()));
        }

        user.setUpdatedBy(updatedBy);
        TenantUser updatedUser = tenantUserRepository.save(user);
        log.info("User updated successfully");

        List<String> permissions = rolePermissionService.getPermissionsByRole(updatedUser.getRol().name());

        return TenantUserMapper.toDTO(updatedUser, permissions);
    }

    @Override
    public void deleteUser(Long id, Long tenantId, String deletedBy) {
        log.info("Deleting user: {} for tenant: {}", id, tenantId);

        TenantUser user = tenantUserRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        user.setActivo(false);
        user.setUpdatedBy(deletedBy);
        tenantUserRepository.save(user);
        log.info("User soft deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id, Long tenantId) {
        log.info("Fetching user: {} for tenant: {}", id, tenantId);

        TenantUser user = tenantUserRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<String> permissions = rolePermissionService.getPermissionsByRole(user.getRol().name());

        return TenantUserMapper.toDTO(user, permissions);
    }

    @Override
    @Transactional(readOnly = true)
    public UserListResponse listUsers(Long tenantId, int page, int pageSize, String search) {
        log.info("Listing users for tenant: {} with page: {}, pageSize: {}, search: {}", tenantId, page, pageSize, search);

        Pageable pageable = PageRequest.of(page, pageSize);
        Page<TenantUser> users;

        if (search != null && !search.isBlank()) {
            users = tenantUserRepository.findByTenantIdAndSearch(tenantId, search, pageable);
        } else {
            users = tenantUserRepository.findByTenantId(tenantId, pageable);
        }

        List<UserDTO> userDTOs = users.getContent().stream()
                .map(user -> {
                    List<String> permissions = rolePermissionService.getPermissionsByRole(user.getRol().name());
                    return TenantUserMapper.toDTO(user, permissions);
                })
                .collect(Collectors.toList());

        return UserListResponse.builder()
                .total((int) users.getTotalElements())
                .usuarios(userDTOs)
                .build();
    }
}
