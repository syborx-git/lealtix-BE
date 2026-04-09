package com.lealtixservice.service.impl;

import com.lealtixservice.dto.WhatsAppMessageRequest;
import com.lealtixservice.dto.WhatsAppMessageResponse;
import com.lealtixservice.exception.WhatsAppException;
import com.lealtixservice.service.IWhatsAppService;
import com.lealtixservice.service.WhatsAppClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Pattern;

@Service
public class WhatsAppServiceImpl implements IWhatsAppService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppServiceImpl.class);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");

    @Autowired
    private WhatsAppClient whatsAppClient;

    @Override
    public WhatsAppMessageResponse sendTemplateMessage(WhatsAppMessageRequest request) {
        validateRequest(request);

        logger.info("Processing WhatsApp template message request for phone: {}", request.getPhoneNumber());

        try {
            // Normalizar teléfono: remover + si existe (Meta lo acepta sin +)
            String normalizedPhone = normalizePhoneNumber(request.getPhoneNumber());
            
            Map<String, Object> response = whatsAppClient.sendTemplateMessage(
                    normalizedPhone,
                    request.getTemplateName(),
                    request.getLanguageCode(),
                    request.getTemplateParameters()
            );

            return buildMessageResponse(response, request.getPhoneNumber());

        } catch (WhatsAppException e) {
            logger.error("WhatsApp exception: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error sending WhatsApp message", e);
            throw new WhatsAppException("Error inesperado al enviar mensaje WhatsApp", "UNEXPECTED_ERROR", 500, e);
        }
    }

    private void validateRequest(WhatsAppMessageRequest request) {
        if (request == null) {
            throw new WhatsAppException("Solicitud nula", "INVALID_REQUEST", 400);
        }

        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new WhatsAppException("Número de teléfono requerido", "INVALID_PHONE", 400);
        }

        if (!isValidPhoneNumber(request.getPhoneNumber())) {
            throw new WhatsAppException(
                    "Número de teléfono inválido. Formato requerido: +525571866433 o 525571866433",
                    "INVALID_PHONE_FORMAT",
                    400
            );
        }

        if (request.getTemplateName() == null || request.getTemplateName().trim().isEmpty()) {
            throw new WhatsAppException("Nombre de plantilla requerido", "INVALID_TEMPLATE", 400);
        }
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return PHONE_PATTERN.matcher(phoneNumber).matches();
    }

    private String normalizePhoneNumber(String phoneNumber) {
        // Quitar + si lo tiene (Meta lo acepta sin +)
        if (phoneNumber.startsWith("+")) {
            return phoneNumber.substring(1);
        }
        return phoneNumber;
    }

    private WhatsAppMessageResponse buildMessageResponse(Map<String, Object> apiResponse, String phoneNumber) {
        WhatsAppMessageResponse response = new WhatsAppMessageResponse();
        response.setRecipient(phoneNumber);
        response.setStatus((String) apiResponse.getOrDefault("status", "sent"));
        response.setMessageId((String) apiResponse.getOrDefault("messageId", null));
        response.setTimestamp((Long) apiResponse.getOrDefault("timestamp", System.currentTimeMillis()));

        return response;
    }
}
