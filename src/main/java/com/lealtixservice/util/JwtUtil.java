package com.lealtixservice.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final long EXPIRATION_MS = 8 * 60 * 60 * 1000; // 8 horas
    // Sesión larga para tablets (POS): 10 años, para que la estación no cierre sesión.
    private static final long LONG_EXPIRATION_MS = 3650L * 24 * 60 * 60 * 1000;

    public String generateToken(String email) {
        return generateToken(email, null, null, false);
    }

    public String generateToken(String email, Long tenantId, String role) {
        return generateToken(email, tenantId, role, false);
    }

    /**
     * @param longSession true => token de larga duración (tablet/POS). false => 8h.
     */
    public String generateToken(String email, Long tenantId, String role, boolean longSession) {
        Date now = new Date();
        long ttl = longSession ? LONG_EXPIRATION_MS : EXPIRATION_MS;
        Date expiryDate = new Date(now.getTime() + ttl);
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        if (tenantId != null) {
            claims.put("tenantId", tenantId);
        }
        claims.put("email", email);
        if (role != null) {
            claims.put("role", role);
        }
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }
}

