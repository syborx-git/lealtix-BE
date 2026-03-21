package com.lealtixservice.enums;

import java.util.Arrays;
import java.util.List;

public enum RoleEnum {
    ADMIN(Arrays.asList(
            "view_dashboard", "manage_users", "manage_campaigns", "manage_categories",
            "manage_products", "view_reports", "manage_settings"
    )),
    MESERO(Arrays.asList(
            "view_comanda", "create_order", "edit_own_order"
    )),
    COCINA(Arrays.asList(
            "view_kitchen_orders", "update_order_status", "view_pending_orders"
    )),
    CAJA(Arrays.asList(
            "view_sales", "process_payment", "manage_transactions", "view_cash_register"
    )),
    MARKETING(Arrays.asList(
            "view_campaigns", "create_campaign", "view_analytics", "manage_redemptions"
    ));

    private final List<String> permissions;

    RoleEnum(List<String> permissions) {
        this.permissions = permissions;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}
