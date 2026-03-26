package com.lealtixservice.enums;

import lombok.Getter;

/**
 * Enum para los estados posibles de una orden (comanda)
 */
@Getter
public enum OrderStatus {
    PENDIENTE("Pendiente de pago"),
    PAGADA("Pagada"),
    CANCELADA("Cancelada"),
    EN_PREPARACION("En preparación"),
    LISTO("Listo para servir");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getValue() {
        return name();
    }
}
