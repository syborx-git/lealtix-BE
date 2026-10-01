package com.lealtixservice.config;

import com.lealtixservice.entity.TenantUser;
import com.lealtixservice.repository.TenantUserRepository;
import com.lealtixservice.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
@ConditionalOnBean(TokenService.class)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final TenantUserRepository tenantUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            
            try {
                Claims claims = tokenService.validateToken(token).getBody();
                
                Object tid = claims.get("tenantId");
                Long tenantId = null;
                if (tid instanceof Number) {
                    tenantId = ((Number) tid).longValue();
                } else if (tid != null) {
                    try {
                        tenantId = Long.parseLong(tid.toString());
                    } catch (NumberFormatException ignored) {}
                }

                String email = claims.get("email", String.class);
                if (email == null) {
                    email = claims.getSubject();
                }

                String role = claims.get("role", String.class);

                // Si el token es antiguo o no trae tenantId/rol, lo resolvemos desde la base de datos
                if ((tenantId == null || role == null) && email != null) {
                    Optional<TenantUser> userOpt = tenantUserRepository.findByEmail(email);
                    if (userOpt.isPresent()) {
                        TenantUser u = userOpt.get();
                        if (tenantId == null && u.getTenant() != null) {
                            tenantId = u.getTenant().getId();
                        }
                        if (role == null && u.getRol() != null) {
                            role = u.getRol().name();
                        }
                    }
                }
                
                if (tenantId != null && email != null) {
                    TenantUserPrincipal principal = new TenantUserPrincipal(tenantId, email);
                    
                    String roleName = (role != null) ? role.toUpperCase() : "ADMIN";
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal, 
                            null, 
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName))
                    );
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException e) {
                logger.warn("Invalid JWT token: " + e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/v3/api-docs") || 
               path.startsWith("/swagger-ui") || 
               path.equals("/stripe/webhook") ||
               path.equals("/error");
    }
}
