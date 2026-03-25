package com.lealtixservice.controller;

import com.lealtixservice.dto.*;
import com.lealtixservice.service.TenantUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "User Management", description = "Operaciones de gestión de usuarios por tenant")
@RestController
@RequestMapping("/api/admin/users")
public class TenantUserController {

    @Autowired
    private TenantUserService tenantUserService;

    @Operation(summary = "Crear nuevo usuario")
    @PostMapping
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            log.info("Creating user: {}", request.getEmail());
            UserDTO userDTO = tenantUserService.createUser(request, "SYSTEM");
            GenericResponse response = new GenericResponse(201, "Usuario creado exitosamente", userDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating user", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Obtener usuario por ID")
    @GetMapping("/{id}")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> getUserById(
            @PathVariable Long id,
            @RequestParam Long tenantId) {
        try {
            log.info("Getting user {} for tenant {}", id, tenantId);
            UserDTO userDTO = tenantUserService.getUserById(id, tenantId);
            GenericResponse response = new GenericResponse(200, "Usuario obtenido", userDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting user", e);
            GenericResponse response = new GenericResponse(404, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @Operation(summary = "Listar usuarios con paginación")
    @GetMapping
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> listUsers(
            @RequestParam Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search) {
        try {
            log.info("Listing users for tenant {} - page: {}, pageSize: {}", tenantId, page, pageSize);
            UserListResponse userListResponse = tenantUserService.listUsers(tenantId, page, pageSize, search);
            GenericResponse response = new GenericResponse(200, "Usuarios listados", userListResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error listing users", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Actualizar usuario")
    @PutMapping("/{id}")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> updateUser(
            @PathVariable Long id,
            @RequestParam Long tenantId,
            @Valid @RequestBody UpdateUserRequest request) {
        try {
            log.info("Updating user {} for tenant {}", id, tenantId);
            UserDTO userDTO = tenantUserService.updateUser(id, tenantId, request, "SYSTEM");
            GenericResponse response = new GenericResponse(200, "Usuario actualizado", userDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating user", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Eliminar usuario (soft delete)")
    @DeleteMapping("/{id}")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> deleteUser(
            @PathVariable Long id,
            @RequestParam Long tenantId) {
        try {
            log.info("Deleting user {} for tenant {}", id, tenantId);
            tenantUserService.deleteUser(id, tenantId, "SYSTEM");
            GenericResponse response = new GenericResponse(200, "Usuario eliminado", null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting user", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }
}
