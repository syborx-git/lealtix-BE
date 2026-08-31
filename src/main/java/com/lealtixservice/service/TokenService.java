package com.lealtixservice.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio utilitario para generación y validación de tokens JWT.
 */
@Component
public class TokenService {

    @Value("${jwt.secret}")
    private String secretKey;
    // Expiración de 3 meses en milisegundos
    private static final long EXPIRATION_TIME = 1000L * 60 * 60 * 24 * 30 * 3; // 3 meses
    // Expiración de 24 horas en milisegundos
    private static final long LOGIN_EXPIRATION_TIME = 1000L * 60 * 60 * 24; // 24 horas

    private Key getKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Genera un token JWT firmado con los claims tenantId y email.
     * @param tenantId identificador del tenant
     * @param email email del usuario
     * @return token JWT
     */
    public String generateToken(Long tenantId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", tenantId);
        claims.put("email", email);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Genera un token JWT de login con duración de 24 horas.
     * @param tenantId identificador del tenant (puede ser null para app_user)
     * @param email email del usuario
     * @return token JWT
     */
    public String generateLoginToken(Long tenantId, String email) {
        Map<String, Object> claims = new HashMap<>();
        if (tenantId != null) {
            claims.put("tenantId", tenantId);
        }
        claims.put("email", email);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + LOGIN_EXPIRATION_TIME))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valida el token JWT y retorna los claims si es válido.
     * @param token JWT
     * @return claims decodificados
     * @throws JwtException si el token no es válido
     */
    public Jws<Claims> validateToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token);
    }

    /**
     * Obtiene el tenantId del token JWT.
     * @param token JWT
     * @return tenantId
     */
    public String getTenantId(String token) {
        return validateToken(token).getBody().get("tenantId", String.class);
    }

    /**
     * Obtiene el email del token JWT.
     * @param token JWT
     * @return email
     */
    public String getEmail(String token) {
        return validateToken(token).getBody().get("email", String.class);
    }
}
