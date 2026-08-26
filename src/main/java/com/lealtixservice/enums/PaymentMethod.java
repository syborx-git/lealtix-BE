package com.lealtixservice.enums;

import lombok.Getter;

/**
 * Enum para los métodos de pago disponibles
 * Solo registra el método, sin procesar transacciones en línea
 */
@Getter
public enum PaymentMethod {
    CASH("Efectivo"),
    CARD("Tarjeta de Crédito/Débito"),
    TRANSFER("Transferencia Bancaria"),
    MIXED("Pago Mixto");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }

    public String getValue() {
        return name();
    }
}
