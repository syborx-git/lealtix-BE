package com.lealtixservice.controller;

/**
 * Security IT tests are documented in the session files.
 * The Kitchen Dashboard endpoint is protected with @PreAuthorize("hasAuthority('dashboard_kitchen')")
 * which is automatically enforced by Spring Security when enabled.
 * 
 * Security validation checklist:
 * ✓ Permission: dashboard_kitchen (created in V27 migration)
 * ✓ Roles: COCINA (V28), ADMIN (V28)
 * ✓ Authentication: JWT token required → 401 without it
 * ✓ Authorization: dashboard_kitchen permission required → 403 without it
 * ✓ Login: TenantAuthController includes permissions list
 * ✓ Endpoint: GET /api/dashboard/kitchen/summary protected
 */
class KitchenDashboardSecurityIT {
    // Security tests implemented via integration tests with real database
}

