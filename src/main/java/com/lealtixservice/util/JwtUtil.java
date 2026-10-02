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

    public String generateToken(String email) {
        return generateToken(email, null, null);
    }

    public String generateToken(String email, Long tenantId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_MS);
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

