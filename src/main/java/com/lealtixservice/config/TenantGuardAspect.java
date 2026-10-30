package com.lealtixservice.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Guard global de aislamiento multi-tenant.
 *
 * Para cualquier endpoint de controller, si la petición está AUTENTICADA con un
 * {@link TenantUserPrincipal}, valida que el `tenantId` que venga por parámetro
 * (path o query) o dentro del @RequestBody coincida con el tenant del token.
 *
 * - Si el usuario intenta operar sobre otro tenant -> 403.
 * - Si la petición NO está autenticada (endpoints públicos: landing, chatbot,
 *   redención, registro, pagos) -> no interfiere.
 */
@Slf4j
@Aspect
@Component
public class TenantGuardAspect {

    @Before("execution(* com.lealtixservice.controller..*(..))")
    public void guardTenantScope(JoinPoint joinPoint) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof TenantUserPrincipal)) {
            return;
        }
        Long userTenantId = ((TenantUserPrincipal) principal).getTenantId();
        if (userTenantId == null) {
            return;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length && i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                continue;
            }
            Parameter parameter = parameters[i];

            // 1) Parámetro directo llamado tenantId (path/query)
            if ("tenantId".equals(parameter.getName())) {
                Long requestTenantId = toLong(arg);
                if (requestTenantId != null && !userTenantId.equals(requestTenantId)) {
                    deny(userTenantId, requestTenantId, method);
                }
            }

            // 2) tenantId dentro del @RequestBody (DTO)
            if (hasRequestBody(parameter)) {
                Long requestTenantId = extractTenantIdFromBody(arg);
                if (requestTenantId != null && !userTenantId.equals(requestTenantId)) {
                    deny(userTenantId, requestTenantId, method);
                }
            }
        }
    }

    private void deny(Long userTenantId, Long requestTenantId, Method method) {
        log.warn("[TenantGuard] Acceso denegado: tenant {} intentó acceder a recursos del tenant {} en {}",
                userTenantId, requestTenantId, method.getName());
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "No tiene permisos para acceder a recursos de otro tenant");
    }

    private boolean hasRequestBody(Parameter parameter) {
        for (java.lang.annotation.Annotation annotation : parameter.getAnnotations()) {
            if (annotation instanceof RequestBody) {
                return true;
            }
        }
        return false;
    }

    private Long extractTenantIdFromBody(Object body) {
        try {
            Method getter = body.getClass().getMethod("getTenantId");
            Object value = getter.invoke(body);
            return toLong(value);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            log.debug("[TenantGuard] No se pudo leer getTenantId() del body: {}", e.getMessage());
            return null;
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
