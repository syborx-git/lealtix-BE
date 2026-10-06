package com.lealtixservice.config;

import com.lealtixservice.util.PermissionValidator;
import com.lealtixservice.util.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Aspect para validar permisos mediante anotación @RequirePermission
 */
@Slf4j
@Aspect
@Component
public class PermissionValidationAspect {

    @Autowired(required = false)
    private PermissionValidator permissionValidator;

    @Before("@annotation(com.lealtixservice.util.RequirePermission)")
    public void validatePermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);
        if (requirePermission == null || permissionValidator == null) {
            return;
        }

        // Obtener el rol del usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() 
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("Sesión no iniciada o token expirado");
        }

        String userRole = authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .filter(auth -> auth.startsWith("ROLE_") && !auth.equals("ROLE_ANONYMOUS"))
                .findFirst()
                .map(auth -> auth.substring(5)) // Remover "ROLE_"
                .orElse(null);

        if (userRole == null) {
            log.warn("Usuario {} no tiene rol asignado o sesión no válida", authentication.getName());
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("Usuario sin rol asignado o sesión no válida");
        }

        // El rol ADMIN o SUPER_ADMIN tiene todos los privilegios en la plataforma
        if ("ADMIN".equalsIgnoreCase(userRole) || "SUPER_ADMIN".equalsIgnoreCase(userRole)) {
            log.debug("Permiso {} concedido automáticamente a rol administrativo {}", requirePermission.value(), userRole);
            return;
        }

        // Validar permiso principal
        String requiredPermission = requirePermission.value();
        if (!permissionValidator.hasPermission(userRole, requiredPermission)) {

            // Validar permisos alternativos
            String[] alternatives = requirePermission.alternative();
            if (alternatives.length > 0) {
                boolean hasAny = permissionValidator.hasAnyPermission(userRole, alternatives);
                if (!hasAny) {
                    log.warn("Usuario {} (rol: {}) no tiene permisos requeridos. Requerido: {}. Alternativos: {}",
                            authentication.getName(), userRole, requiredPermission, Arrays.toString(alternatives));
                    throw new AccessDeniedException("No tiene permisos para esta operación");
                }
            } else {
                log.warn("Usuario {} (rol: {}) no tiene permiso: {}",
                        authentication.getName(), userRole, requiredPermission);
                throw new AccessDeniedException("No tiene permiso: " + requiredPermission);
            }
        }

        log.debug("Permiso {} validado exitosamente para usuario {} (rol: {})",
                requiredPermission, authentication.getName(), userRole);
    }
}
