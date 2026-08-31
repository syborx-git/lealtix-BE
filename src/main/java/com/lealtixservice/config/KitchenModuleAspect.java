package com.lealtixservice.config;

import com.lealtixservice.entity.TenantConfig;
import com.lealtixservice.repository.TenantConfigRepository;
import com.lealtixservice.util.RequireKitchenModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Slf4j
@Aspect
@Component
@ConditionalOnBean(TenantConfigRepository.class)
@RequiredArgsConstructor
public class KitchenModuleAspect {

    private final TenantConfigRepository tenantConfigRepository;

    @Before("@annotation(com.lealtixservice.util.RequireKitchenModule)")
    public void validateKitchenModuleEnabled(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        RequireKitchenModule annotation = method.getAnnotation(RequireKitchenModule.class);
        if (annotation == null) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        
        Object principal = authentication.getPrincipal();
        Long tenantId = null;
        
        if (principal instanceof TenantUserPrincipal) {
            tenantId = ((TenantUserPrincipal) principal).getTenantId();
        }
        
        if (tenantId == null) {
            Parameter[] parameters = method.getParameters();
            Object[] args = joinPoint.getArgs();
            
            for (int i = 0; i < parameters.length; i++) {
                String paramName = parameters[i].getName();
                if (paramName.equals("tenantId")) {
                    Object arg = args[i];
                    if (arg instanceof Long) {
                        tenantId = (Long) arg;
                        break;
                    } else if (arg != null) {
                        tenantId = Long.valueOf(arg.toString());
                        break;
                    }
                }
            }
        }
        
        if (tenantId == null) {
            log.warn("No se pudo determinar tenantId para validar módulo de cocina");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TenantId no proporcionado");
        }
        
        TenantConfig tenantConfig = tenantConfigRepository.findByTenantId(tenantId);
        
        if (tenantConfig == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuración de tenant no encontrada");
        }
        
        if (tenantConfig.getKitchenModuleEnabled() == null || !tenantConfig.getKitchenModuleEnabled()) {
            log.warn("Intento de acceso a módulo de cocina deshabilitado para tenant {}", tenantId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "El módulo de cocina no está habilitado para este tenant");
        }
    }
}
