package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para RolePermission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionDTO {
    private Long id;
    private String role;
    private PermissionDTO permission;
    private LocalDateTime grantedAt;
}
