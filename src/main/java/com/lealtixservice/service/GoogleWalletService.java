package com.lealtixservice.service;

import com.lealtixservice.entity.CustomerLoyaltyCard;
import com.lealtixservice.entity.TenantLoyaltyCardConfig;

public interface GoogleWalletService {

    /**
     * Genera la URL de guardado directo (JWT Save to Google Wallet)
     * para que el comensal agregue su pase a su teléfono con 1 solo toque.
     */
    String generateSavePassUrl(CustomerLoyaltyCard card, TenantLoyaltyCardConfig config);

    /**
     * Actualiza en tiempo real el saldo de sellos y estado de la tarjeta en Google Wallet,
     * disparando la notificación push nativa en el dispositivo del cliente.
     */
    void updatePassStamps(CustomerLoyaltyCard card, TenantLoyaltyCardConfig config);

    /**
     * Verifica si el emisor y las credenciales de servicio están activas y configuradas.
     */
    boolean isConfigured();
}

