package com.lealtixservice.service;

import com.lealtixservice.enums.RoleEnum;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolePermissionService {

    public List<String> getPermissionsByRole(String roleName) {
        try {
            RoleEnum role = RoleEnum.valueOf(roleName.toUpperCase());
            return role.getPermissions();
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    public List<String> getPermissionsByRole(RoleEnum role) {
        return role.getPermissions();
    }

    public boolean hasPermission(String roleName, String permission) {
        List<String> permissions = getPermissionsByRole(roleName);
        return permissions.contains(permission);
    }
}
