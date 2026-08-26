package com.lealtixservice.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TenantUserPrincipal {
    private Long tenantId;
    private String email;
}
