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
    ABIERTA("Comanda abierta en consumo"),
    POR_COBRAR("Ticket impreso en mesa para cobro"),
    PAGADA("Pagada"),
    CANCELADA("Cancelada");


    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getValue() {
        return name();
    }

    @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING)
    public static OrderStatus fromString(String value) {
        if (value == null) return null;
        return switch (value.toUpperCase().trim()) {
            case "PENDING", "PENDIENTE" -> PENDIENTE;
            case "CONFIRMED", "CONFIRMADA", "CONFIRMADO" -> CONFIRMADA;
            case "IN_PREPARATION", "EN_PREPARACION", "PREPARING" -> EN_PREPARACION;
            case "READY", "LISTO", "COMPLETED" -> LISTO;
            case "OPEN", "ABIERTA" -> ABIERTA;
            case "POR_COBRAR", "PRE_CUENTA" -> POR_COBRAR;
            case "PAID", "PAGADA" -> PAGADA;
            case "CANCELLED", "CANCELED", "CANCELADA", "RECHAZADO" -> CANCELADA;
            default -> OrderStatus.valueOf(value.toUpperCase().trim());
        };
    }
}
