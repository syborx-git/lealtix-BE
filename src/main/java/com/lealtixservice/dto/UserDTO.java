package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String nombre;
    private String email;
    private String rol;
    private List<String> permissions;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean kitchenModuleEnabled;
}
