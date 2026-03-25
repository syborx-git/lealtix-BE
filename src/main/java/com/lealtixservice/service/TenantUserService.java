package com.lealtixservice.service;

import com.lealtixservice.dto.CreateUserRequest;
import com.lealtixservice.dto.UpdateUserRequest;
import com.lealtixservice.dto.UserDTO;
import com.lealtixservice.dto.UserListResponse;
import org.springframework.data.domain.Pageable;

public interface TenantUserService {
    UserDTO createUser(CreateUserRequest request, String createdBy);
    UserDTO updateUser(Long id, Long tenantId, UpdateUserRequest request, String updatedBy);
    void deleteUser(Long id, Long tenantId, String deletedBy);
    UserDTO getUserById(Long id, Long tenantId);
    UserListResponse listUsers(Long tenantId, int page, int pageSize, String search);
}
