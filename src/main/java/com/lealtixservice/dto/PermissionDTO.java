package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para Permission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String resource;
    private String action;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
