package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.PermissionDTO;
import com.lealtixservice.dto.PermissionRequestDTO;
import com.lealtixservice.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Permission Management", description = "Operaciones de gestión de permisos del sistema")
@RestController
@RequestMapping("/api/admin/permissions")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @Operation(summary = "Listar todos los permisos")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        try {
            log.info("Listing permissions - page: {}, pageSize: {}", page, pageSize);
            Pageable pageable = PageRequest.of(page, pageSize);
            Page<PermissionDTO> permissions = permissionService.listPaginated(pageable);
            GenericResponse response = new GenericResponse(200, "Permisos obtenidos exitosamente", permissions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error listing permissions", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Obtener permiso por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> getById(@PathVariable Long id) {
        try {
            log.info("Getting permission by ID: {}", id);
            PermissionDTO permission = permissionService.getById(id);
            GenericResponse response = new GenericResponse(200, "Permiso obtenido", permission);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting permission", e);
            GenericResponse response = new GenericResponse(404, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @Operation(summary = "Obtener permiso por código")
    @GetMapping("/code/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> getByCode(@PathVariable String code) {
        try {
            log.info("Getting permission by code: {}", code);
            PermissionDTO permission = permissionService.getByCode(code)
                    .orElseThrow(() -> new RuntimeException("Permiso no encontrado: " + code));
            GenericResponse response = new GenericResponse(200, "Permiso obtenido", permission);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting permission by code", e);
            GenericResponse response = new GenericResponse(404, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @Operation(summary = "Obtener permisos por recurso")
    @GetMapping("/by-resource/{resource}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> getByResource(@PathVariable String resource) {
        try {
            log.info("Getting permissions by resource: {}", resource);
            List<PermissionDTO> permissions = permissionService.getByResource(resource);
            GenericResponse response = new GenericResponse(200, "Permisos obtenidos", permissions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting permissions by resource", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Obtener permisos por categoría")
    @GetMapping("/by-category/{category}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> getByCategory(@PathVariable String category) {
        try {
            log.info("Getting permissions by category: {}", category);
            List<PermissionDTO> permissions = permissionService.getByCategory(category);
            GenericResponse response = new GenericResponse(200, "Permisos obtenidos", permissions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting permissions by category", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Crear nuevo permiso")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> create(@Valid @RequestBody PermissionRequestDTO requestDTO) {
        try {
            log.info("Creating permission: {}", requestDTO.getCode());
            PermissionDTO permissionDTO = PermissionDTO.builder()
                    .code(requestDTO.getCode())
                    .name(requestDTO.getName())
                    .description(requestDTO.getDescription())
                    .resource(requestDTO.getResource())
                    .action(requestDTO.getAction())
                    .category(requestDTO.getCategory())
                    .build();

            PermissionDTO created = permissionService.create(permissionDTO);
            GenericResponse response = new GenericResponse(201, "Permiso creado exitosamente", created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating permission", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Actualizar permiso")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PermissionRequestDTO requestDTO) {
        try {
            log.info("Updating permission: {}", id);
            PermissionDTO permissionDTO = PermissionDTO.builder()
                    .code(requestDTO.getCode())
                    .name(requestDTO.getName())
                    .description(requestDTO.getDescription())
                    .resource(requestDTO.getResource())
                    .action(requestDTO.getAction())
                    .category(requestDTO.getCategory())
                    .build();

            PermissionDTO updated = permissionService.update(id, permissionDTO);
            GenericResponse response = new GenericResponse(200, "Permiso actualizado exitosamente", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating permission", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Eliminar permiso")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> delete(@PathVariable Long id) {
        try {
            log.info("Deleting permission: {}", id);
            permissionService.delete(id);
            GenericResponse response = new GenericResponse(200, "Permiso eliminado exitosamente", null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting permission", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }
}
