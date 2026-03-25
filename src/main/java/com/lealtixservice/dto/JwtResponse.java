package com.lealtixservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtResponse {
    private String accessToken;
    private String userEmail;
    private Long userId;
    private List<String> permissions;

    // Constructores adicionales para backwards compatibility
    public JwtResponse(String accessToken, String userEmail, Long userId) {
        this.accessToken = accessToken;
        this.userEmail = userEmail;
        this.userId = userId;
        this.permissions = List.of();
    }
}

