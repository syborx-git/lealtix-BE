package com.lealtixservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request para asignar permisos a un rol
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignPermissionsRequest {
    
    @NotNull(message = "Role es requerido")
    private String role;
    
    @NotEmpty(message = "PermissionIds no puede estar vacío")
    private List<Long> permissionIds;
    
    /**
     * Si es true, reemplaza todos los permisos
     * Si es false, solo agrega los nuevos
     */
    @Builder.Default
    private Boolean replace = false;
}
