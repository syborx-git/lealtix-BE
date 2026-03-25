package com.lealtixservice.controller;

import com.lealtixservice.dto.AssignPermissionsRequest;
import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.RolePermissionDTO;
import com.lealtixservice.service.PermissionManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "Role Permission Management", description = "Operaciones para asignar permisos a roles")
@RestController
@RequestMapping("/api/admin/roles")
public class PermissionManagementController {

    @Autowired
    private PermissionManagementService permissionManagementService;

    @Operation(summary = "Listar todos los roles disponibles")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> listAllRoles() {
        try {
            log.info("Listing all available roles");
            List<String> roles = permissionManagementService.getAllRoles();
            GenericResponse response = new GenericResponse(200, "Roles obtenidos exitosamente", roles);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error listing roles", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Obtener permisos asignados a un rol")
    @GetMapping("/{role}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> getPermissionsByRole(@PathVariable String role) {
        try {
            log.info("Getting permissions for role: {}", role);
            List<RolePermissionDTO> permissions = permissionManagementService.getPermissionsByRole(role);
            GenericResponse response = new GenericResponse(200, "Permisos del rol obtenidos", permissions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting permissions by role", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Obtener códigos de permisos asignados a un rol")
    @GetMapping("/{role}/permission-codes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> getPermissionCodesByRole(@PathVariable String role) {
        try {
            log.info("Getting permission codes for role: {}", role);
            List<String> permissionCodes = permissionManagementService.getPermissionCodesByRole(role);
            GenericResponse response = new GenericResponse(200, "Códigos de permisos obtenidos", permissionCodes);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting permission codes by role", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Asignar permisos a un rol (agregar nuevos)")
    @PostMapping("/{role}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> assignPermissionsToRole(
            @PathVariable String role,
            @Valid @RequestBody AssignPermissionsRequest request) {
        try {
            log.info("Assigning permissions to role: {}", role);
            List<RolePermissionDTO> assigned = permissionManagementService.assignPermissionsToRole(
                    role,
                    request.getPermissionIds()
            );
            GenericResponse response = new GenericResponse(201, "Permisos asignados exitosamente", assigned);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error assigning permissions to role", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Reemplazar todos los permisos de un rol")
    @PutMapping("/{role}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> replaceRolePermissions(
            @PathVariable String role,
            @Valid @RequestBody AssignPermissionsRequest request) {
        try {
            log.info("Replacing all permissions for role: {}", role);
            List<RolePermissionDTO> replaced = permissionManagementService.replaceRolePermissions(
                    role,
                    request.getPermissionIds()
            );
            GenericResponse response = new GenericResponse(200, "Permisos reemplazados exitosamente", replaced);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error replacing role permissions", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Revocar un permiso de un rol")
    @DeleteMapping("/{role}/permissions/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> revokePermission(
            @PathVariable String role,
            @PathVariable Long permissionId) {
        try {
            log.info("Revoking permission {} from role {}", permissionId, role);
            permissionManagementService.revokePermissionFromRole(role, permissionId);
            GenericResponse response = new GenericResponse(200, "Permiso revocado exitosamente", null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error revoking permission", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Revocar múltiples permisos de un rol")
    @DeleteMapping("/{role}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> revokePermissions(
            @PathVariable String role,
            @RequestBody List<Long> permissionIds) {
        try {
            log.info("Revoking {} permissions from role {}", permissionIds.size(), role);
            permissionManagementService.revokePermissionsFromRole(role, permissionIds);
            GenericResponse response = new GenericResponse(200, "Permisos revocados exitosamente", null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error revoking permissions", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Verificar si un rol tiene un permiso específico")
    @GetMapping("/{role}/has-permission/{permissionCode}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse> hasPermission(
            @PathVariable String role,
            @PathVariable String permissionCode) {
        try {
            log.info("Checking if role {} has permission {}", role, permissionCode);
            boolean hasPermission = permissionManagementService.roleHasPermission(role, permissionCode);
            Map<String, Object> result = new HashMap<>();
            result.put("role", role);
            result.put("permission", permissionCode);
            result.put("hasPermission", hasPermission);
            GenericResponse response = new GenericResponse(200, "Verificación completada", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking permission", e);
            GenericResponse response = new GenericResponse(400, e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        }
    }
}
