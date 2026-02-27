package com.lealtixservice.enums;

/**
 * Canal por el cual se redimió un cupón.
 * Permite diferenciar el origen de la redención para reportes y análisis.
 */
public enum RedemptionChannel {
    QR_WEB,      // Redención vía página web escaneando QR
    QR_ADMIN,    // Redención desde el admin/scanner del tenant
    MANUAL,
    COMANDIX, // Redencion desde comanda
    API, CHATBOT;         // Redención vía API externa

    public String getValue() {
        return name();
    }
}

