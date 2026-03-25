package com.lealtixservice.service.impl;

import com.lealtixservice.dto.CreateUserRequest;
import com.lealtixservice.dto.UpdateUserRequest;
import com.lealtixservice.dto.UserDTO;
import com.lealtixservice.dto.UserListResponse;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantUser;
import com.lealtixservice.enums.RoleEnum;
import com.lealtixservice.exception.BusinessRuleException;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.repository.TenantUserRepository;
import com.lealtixservice.service.RolePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantUserServiceImplTest {

    @Mock
    private TenantUserRepository tenantUserRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RolePermissionService rolePermissionService;

    @InjectMocks
    private TenantUserServiceImpl tenantUserService;

    private Tenant tenant;
    private TenantUser tenantUser;
    private CreateUserRequest createUserRequest;
    private UpdateUserRequest updateUserRequest;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder()
                .id(1L)
                .nombreNegocio("Mi Negocio")
                .isActive(true)
                .build();

        tenantUser = TenantUser.builder()
                .id(1L)
                .tenant(tenant)
                .nombre("Juan Pérez")
                .email("juan@example.com")
                .passwordHash("hashedPassword")
                .rol(RoleEnum.MESERO)
                .activo(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createUserRequest = CreateUserRequest.builder()
                .nombre("Juan Pérez")
                .email("juan@example.com")
                .contrasena("password123")
                .rol("MESERO")
                .tenantId(1L)
                .build();

        updateUserRequest = UpdateUserRequest.builder()
                .nombre("Juan Carlos Pérez")
                .build();
    }

    @Test
    void testCreateUserSuccess() {
        // Arrange
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenantUserRepository.existsByEmailAndTenantId("juan@example.com", 1L)).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(tenantUserRepository.save(any(TenantUser.class))).thenReturn(tenantUser);
        when(rolePermissionService.getPermissionsByRole("MESERO"))
                .thenReturn(Arrays.asList("view_comanda", "create_order", "edit_own_order"));

        // Act
        UserDTO result = tenantUserService.createUser(createUserRequest, "SYSTEM");

        // Assert
        assertNotNull(result);
        assertEquals("Juan Pérez", result.getNombre());
        assertEquals("juan@example.com", result.getEmail());
        assertEquals("MESERO", result.getRol());
        assertTrue(result.getActivo());
        assertEquals(3, result.getPermissions().size());

        verify(tenantRepository, times(1)).findById(1L);
        verify(tenantUserRepository, times(1)).existsByEmailAndTenantId("juan@example.com", 1L);
        verify(passwordEncoder, times(1)).encode("password123");
        verify(tenantUserRepository, times(1)).save(any(TenantUser.class));
    }

    @Test
    void testCreateUserTenantNotFound() {
        // Arrange
        when(tenantRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            tenantUserService.createUser(createUserRequest, "SYSTEM");
        });

        verify(tenantRepository, times(1)).findById(1L);
        verify(tenantUserRepository, never()).save(any(TenantUser.class));
    }

    @Test
    void testCreateUserEmailAlreadyExists() {
        // Arrange
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenantUserRepository.existsByEmailAndTenantId("juan@example.com", 1L)).thenReturn(true);

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> {
            tenantUserService.createUser(createUserRequest, "SYSTEM");
        });

        verify(tenantRepository, times(1)).findById(1L);
        verify(tenantUserRepository, times(1)).existsByEmailAndTenantId("juan@example.com", 1L);
        verify(tenantUserRepository, never()).save(any(TenantUser.class));
    }

    @Test
    void testCreateUserInvalidRole() {
        // Arrange
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .nombre("Juan Pérez")
                .email("juan@example.com")
                .contrasena("password123")
                .rol("INVALID_ROLE")
                .tenantId(1L)
                .build();

        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenantUserRepository.existsByEmailAndTenantId("juan@example.com", 1L)).thenReturn(false);

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> {
            tenantUserService.createUser(invalidRequest, "SYSTEM");
        });
    }

    @Test
    void testUpdateUserSuccess() {
        // Arrange
        when(tenantUserRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(tenantUser));
        when(tenantUserRepository.save(any(TenantUser.class))).thenReturn(tenantUser);
        when(rolePermissionService.getPermissionsByRole("MESERO"))
                .thenReturn(Arrays.asList("view_comanda", "create_order", "edit_own_order"));

        // Act
        UserDTO result = tenantUserService.updateUser(1L, 1L, updateUserRequest, "SYSTEM");

        // Assert
        assertNotNull(result);
        verify(tenantUserRepository, times(1)).findByIdAndTenantId(1L, 1L);
        verify(tenantUserRepository, times(1)).save(any(TenantUser.class));
    }

    @Test
    void testUpdateUserNotFound() {
        // Arrange
        when(tenantUserRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            tenantUserService.updateUser(1L, 1L, updateUserRequest, "SYSTEM");
        });

        verify(tenantUserRepository, times(1)).findByIdAndTenantId(1L, 1L);
        verify(tenantUserRepository, never()).save(any(TenantUser.class));
    }

    @Test
    void testDeleteUserSuccess() {
        // Arrange
        when(tenantUserRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(tenantUser));
        when(tenantUserRepository.save(any(TenantUser.class))).thenReturn(tenantUser);

        // Act
        tenantUserService.deleteUser(1L, 1L, "SYSTEM");

        // Assert
        verify(tenantUserRepository, times(1)).findByIdAndTenantId(1L, 1L);
        verify(tenantUserRepository, times(1)).save(any(TenantUser.class));
    }

    @Test
    void testDeleteUserNotFound() {
        // Arrange
        when(tenantUserRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            tenantUserService.deleteUser(1L, 1L, "SYSTEM");
        });

        verify(tenantUserRepository, times(1)).findByIdAndTenantId(1L, 1L);
        verify(tenantUserRepository, never()).save(any(TenantUser.class));
    }

    @Test
    void testGetUserByIdSuccess() {
        // Arrange
        when(tenantUserRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(tenantUser));
        when(rolePermissionService.getPermissionsByRole("MESERO"))
                .thenReturn(Arrays.asList("view_comanda", "create_order", "edit_own_order"));

        // Act
        UserDTO result = tenantUserService.getUserById(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("Juan Pérez", result.getNombre());
        verify(tenantUserRepository, times(1)).findByIdAndTenantId(1L, 1L);
    }

    @Test
    void testListUsersSuccess() {
        // Arrange
        List<TenantUser> users = Arrays.asList(tenantUser);
        Page<TenantUser> page = new PageImpl<>(users, PageRequest.of(0, 10), 1);

        when(tenantUserRepository.findByTenantId(1L, PageRequest.of(0, 10))).thenReturn(page);
        when(rolePermissionService.getPermissionsByRole("MESERO"))
                .thenReturn(Arrays.asList("view_comanda", "create_order", "edit_own_order"));

        // Act
        UserListResponse result = tenantUserService.listUsers(1L, 0, 10, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getUsuarios().size());
        verify(tenantUserRepository, times(1)).findByTenantId(1L, PageRequest.of(0, 10));
    }

    @Test
    void testListUsersWithSearch() {
        // Arrange
        List<TenantUser> users = Arrays.asList(tenantUser);
        Page<TenantUser> page = new PageImpl<>(users, PageRequest.of(0, 10), 1);

        when(tenantUserRepository.findByTenantIdAndSearch(eq(1L), eq("juan"), any(Pageable.class))).thenReturn(page);
        when(rolePermissionService.getPermissionsByRole("MESERO"))
                .thenReturn(Arrays.asList("view_comanda", "create_order", "edit_own_order"));

        // Act
        UserListResponse result = tenantUserService.listUsers(1L, 0, 10, "juan");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getUsuarios().size());
        verify(tenantUserRepository, times(1)).findByTenantIdAndSearch(eq(1L), eq("juan"), any(Pageable.class));
    }
}
