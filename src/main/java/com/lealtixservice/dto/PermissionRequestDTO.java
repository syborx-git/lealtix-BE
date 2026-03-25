package com.lealtixservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request para crear/actualizar permiso
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionRequestDTO {
    
    @NotBlank(message = "Code es requerido")
    private String code;
    
    @NotBlank(message = "Name es requerido")
    private String name;
    
    private String description;
    
    private String resource;
    
    private String action;
    
    private String category;
}
