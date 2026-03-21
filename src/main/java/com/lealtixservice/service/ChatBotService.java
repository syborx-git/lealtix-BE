package com.lealtixservice.service;

import com.lealtixservice.dto.*;
import com.lealtixservice.entity.ChatBotSession;

import java.util.List;

/**
 * Servicio para gestionar las interacciones del ChatBot (Mesero Virtual).
 * Orquesta la validación de clientes, sugerencias de productos, validación de cupones
 * y creación de órdenes desde el ChatBot.
 */
public interface ChatBotService {

    /**
     * Crea o recupera una sesión del ChatBot
     */
    ChatBotSession getOrCreateSession(Long tenantId, String sessionId, String phone, String email);

    /**
     * Valida si un cliente existe por teléfono o email y retorna información completa
     * incluyendo LTV, cupones activos y "lo de siempre" (última orden)
     */
    CustomerValidationResponseDTO validateCustomer(Long tenantId, String phone, String email);

    /**
     * Registro rápido de cliente desde el ChatBot
     */
    TenantCustomerDTO registerCustomer(QuickCustomerRegistrationDTO request);

    /**
     * Obtiene la última orden del cliente para sugerir "lo de siempre"
     */
    List<CustomerValidationResponseDTO.ProductSuggestionDTO> getLastOrderProducts(Long customerId, Long tenantId);

    /**
     * Obtiene sugerencias de venta cruzada para un producto
     */
    List<CustomerValidationResponseDTO.ProductSuggestionDTO> getCrossSellingSuggestions(Long productId, Long tenantId);

    /**
     * Valida un cupón para el ChatBot
     */
    CouponValidationResponse validateCoupon(String couponCode, Long tenantId);

    /**
     * Redime un cupón desde el ChatBot aplicando la lógica de descuento
     * (porcentaje, monto fijo, 2x1, etc.)
     */
    ChatBotCouponRedemptionResponse redeemCouponFromChatBot(ChatBotRedeemCouponRequest request);

    /**
     * Crea una orden desde el ChatBot
     */
    ClientOrderDTO createOrderFromChatBot(ChatBotOrderRequestDTO request);

    /**
     * Registra un mensaje en la sesión del ChatBot
     */
    void logMessage(String sessionId, String messageType, String sender, String content, String metadata);

    /**
     * Completa una sesión del ChatBot
     */
    void completeSession(String sessionId);

    /**
     * Abandona una sesión del ChatBot
     */
    void abandonSession(String sessionId);

    /**
     * Obtiene el historial de mensajes de una sesión
     */
    List<ChatBotMessageDTO> getSessionMessages(String sessionId);
}
