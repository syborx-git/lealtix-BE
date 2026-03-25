package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.JwtResponse;
import com.lealtixservice.entity.TenantUser;
import com.lealtixservice.exception.BusinessRuleException;
import com.lealtixservice.repository.TenantUserRepository;
import com.lealtixservice.service.RolePermissionService;
import com.lealtixservice.util.EncrypUtils;
import com.lealtixservice.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Tenant Authentication", description = "Operaciones de login para usuarios de tenant")
@RestController
@RequestMapping("/api/tenant/auth")
public class TenantAuthController {

    @Autowired
    private TenantUserRepository tenantUserRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Operation(summary = "Login de usuario tenant", description = "Autentica usuario tenant por email y contraseña. El tenantId se obtiene automáticamente del backend.")
    @PostMapping("/login")
    public ResponseEntity<GenericResponse> login(@RequestBody TenantLoginRequest request) {
        try {
            log.info("Tenant login attempt - email: {}", request.getEmail());

            // Buscar usuario por email (sin requerir tenantId del cliente)
            TenantUser user = tenantUserRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BusinessRuleException("Usuario o contraseña incorrectos"));

            // Validar que el usuario está activo
            if (!user.getActivo()) {
                throw new BusinessRuleException("Usuario desactivado");
            }

            // Validar contraseña
            if (!EncrypUtils.encryptPassword(request.getPassword()).equals(user.getPasswordHash())) {
                throw new BusinessRuleException("Usuario o contraseña incorrectos");
            }

            // Generar token JWT
            String token = jwtUtil.generateToken(user.getEmail());

            // Obtener permisos del rol
            List<String> permissions = rolePermissionService.getPermissionsByRole(user.getRol().name());

            // Construir respuesta con permisos
            JwtResponse jwtResponse = JwtResponse.builder()
                    .accessToken(token)
                    .userEmail(user.getEmail())
                    .userId(user.getId())
                    .permissions(permissions)
                    .build();

            log.info("Tenant login successful - userId: {}, tenantId: {}", user.getId(), user.getTenant().getId());
            GenericResponse response = new GenericResponse(200, "Login exitoso", jwtResponse);
            return ResponseEntity.ok(response);

        } catch (BusinessRuleException e) {
            log.warn("Login failed: {}", e.getMessage());
            GenericResponse response = new GenericResponse(401, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            log.error("Error during login", e);
            GenericResponse response = new GenericResponse(500, "Error interno del servidor", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantLoginRequest {
        private String email;
        private String password;
    }
}
