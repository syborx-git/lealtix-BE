package com.lealtixservice.config;

import com.lealtixservice.config.TenantUserPrincipal;
import com.lealtixservice.util.TenantOwnership;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
@ConditionalOnClass(TenantUserPrincipal.class)
public class TenantOwnershipAspect {

    @Before("@annotation(com.lealtixservice.util.TenantOwnership)")
    public void validateTenantOwnership(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        TenantOwnership annotation = method.getAnnotation(TenantOwnership.class);
        if (annotation == null) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof TenantUserPrincipal)) {
            log.warn("Principal no es de tipo TenantUserPrincipal");
            return;
        }
        
        TenantUserPrincipal userPrincipal = (TenantUserPrincipal) principal;
        Long userTenantId = userPrincipal.getTenantId();
        
        String paramName = annotation.tenantIdParam();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();
        
        Long requestTenantId = null;
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(paramName) || 
                (parameters[i].isNamePresent() && parameters[i].getName().equals(paramName))) {
                Object arg = args[i];
                if (arg instanceof Long) {
                    requestTenantId = (Long) arg;
                    break;
                } else if (arg != null) {
                    requestTenantId = Long.valueOf(arg.toString());
                    break;
                }
            }
        }
        
        if (requestTenantId == null) {
            log.warn("No se pudo extraer tenantId del parámetro '{}'", paramName);
            return;
        }
        
        if (!userTenantId.equals(requestTenantId)) {
            log.warn("Intento de acceso denegado: usuario con tenantId {} intentó acceder a recursos de tenantId {}", 
                    userTenantId, requestTenantId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "No tiene permisos para acceder a recursos de otro tenant");
        }
    }
}
