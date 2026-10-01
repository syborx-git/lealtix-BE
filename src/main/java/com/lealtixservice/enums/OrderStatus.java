package com.lealtixservice.enums;

import lombok.Getter;

/**
 * Enum para los estados posibles de una orden (comanda)
 */
@Getter
public enum OrderStatus {
    PENDIENTE("Pendiente de confirmacion"),
    CONFIRMADA("Orden confirmada por mesero"),
    EN_PREPARACION("En preparación"),
    LISTO("Listo para servir"),
    PAGADA("Pagada"),
    CANCELADA("Cancelada");


    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getValue() {
        return name();
    }
}
