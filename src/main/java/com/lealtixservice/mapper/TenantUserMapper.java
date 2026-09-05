package com.lealtixservice.mapper;

import com.lealtixservice.dto.UserDTO;
import com.lealtixservice.dto.CreateUserRequest;
import com.lealtixservice.dto.UpdateUserRequest;
import com.lealtixservice.entity.TenantUser;
import com.lealtixservice.enums.RoleEnum;

import java.util.List;

public class TenantUserMapper {

    public static UserDTO toDTO(TenantUser entity, List<String> permissions) {
        if (entity == null) return null;
        return UserDTO.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .email(entity.getEmail())
                .rol(entity.getRol().name())
                .permissions(permissions)
                .activo(entity.getActivo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .sueldoMensual(entity.getSueldoMensual())
                .build();
    }

    public static TenantUser toEntity(CreateUserRequest request, String passwordHash) {
        if (request == null) return null;
        return TenantUser.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .rol(RoleEnum.valueOf(request.getRol().toUpperCase()))
                .activo(true)
                .sueldoMensual(request.getSueldoMensual() != null ? request.getSueldoMensual() : 100.0)
                .build();
    }

    public static void updateEntity(UpdateUserRequest request, TenantUser entity) {
        if (request == null || entity == null) return;
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            entity.setNombre(request.getNombre());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            entity.setEmail(request.getEmail());
        }
        if (request.getRol() != null && !request.getRol().isBlank()) {
            entity.setRol(RoleEnum.valueOf(request.getRol().toUpperCase()));
        }
        if (request.getSueldoMensual() != null) {
            entity.setSueldoMensual(Math.max(0, request.getSueldoMensual()));
        }
    }
}
