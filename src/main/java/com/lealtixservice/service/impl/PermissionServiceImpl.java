package com.lealtixservice.service.impl;

import com.lealtixservice.dto.PermissionDTO;
import com.lealtixservice.entity.Permission;
import com.lealtixservice.exception.BusinessRuleException;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.repository.PermissionRepository;
import com.lealtixservice.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public PermissionDTO create(PermissionDTO permissionDTO) {
        log.info("Creating permission: {}", permissionDTO.getCode());

        // Validar que no exista permiso con ese código
        if (permissionRepository.findByCode(permissionDTO.getCode()).isPresent()) {
            throw new BusinessRuleException("Ya existe un permiso con el código: " + permissionDTO.getCode());
        }

        Permission permission = Permission.builder()
                .code(permissionDTO.getCode())
                .name(permissionDTO.getName())
                .description(permissionDTO.getDescription())
                .resource(permissionDTO.getResource())
                .action(permissionDTO.getAction())
                .category(permissionDTO.getCategory())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Permission saved = permissionRepository.save(permission);
        log.info("Permission created successfully with ID: {}", saved.getId());

        return toDTO(saved);
    }

    @Override
    public PermissionDTO getById(Long id) {
        Permission permission = getPermissionEntity(id);
        return toDTO(permission);
    }

    @Override
    public Optional<PermissionDTO> getByCode(String code) {
        return permissionRepository.findByCode(code).map(this::toDTO);
    }

    @Override
    public List<PermissionDTO> listAll() {
        return permissionRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PermissionDTO> listPaginated(Pageable pageable) {
        return permissionRepository.findAll(pageable).map(this::toDTO);
    }

    @Override
    public List<PermissionDTO> getByResource(String resource) {
        return permissionRepository.findByResource(resource).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionDTO> getByCategory(String category) {
        return permissionRepository.findByCategory(category).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PermissionDTO update(Long id, PermissionDTO permissionDTO) {
        log.info("Updating permission: {}", id);

        Permission permission = getPermissionEntity(id);

        // Si cambia el código, validar que no exista otro con el mismo
        if (!permission.getCode().equals(permissionDTO.getCode())) {
            if (permissionRepository.findByCode(permissionDTO.getCode()).isPresent()) {
                throw new BusinessRuleException("Ya existe un permiso con el código: " + permissionDTO.getCode());
            }
        }

        permission.setCode(permissionDTO.getCode());
        permission.setName(permissionDTO.getName());
        permission.setDescription(permissionDTO.getDescription());
        permission.setResource(permissionDTO.getResource());
        permission.setAction(permissionDTO.getAction());
        permission.setCategory(permissionDTO.getCategory());
        permission.setUpdatedAt(LocalDateTime.now());

        Permission updated = permissionRepository.save(permission);
        log.info("Permission updated successfully");

        return toDTO(updated);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting permission: {}", id);
        Permission permission = getPermissionEntity(id);
        permissionRepository.delete(permission);
        log.info("Permission deleted successfully");
    }

    @Override
    public Permission getPermissionEntity(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permiso no encontrado con ID: " + id));
    }

    private PermissionDTO toDTO(Permission permission) {
        return PermissionDTO.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .name(permission.getName())
                .description(permission.getDescription())
                .resource(permission.getResource())
                .action(permission.getAction())
                .category(permission.getCategory())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .build();
    }
}
