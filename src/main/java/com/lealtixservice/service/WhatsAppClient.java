package com.lealtixservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lealtixservice.config.WhatsAppConfig;
import com.lealtixservice.dto.WhatsAppErrorResponse;
import com.lealtixservice.exception.WhatsAppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class WhatsAppClient {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final WhatsAppConfig whatsAppConfig;
    private final HttpClient httpClient;

    public WhatsAppClient(WhatsAppConfig whatsAppConfig, HttpClient whatsAppHttpClient) {
        this.whatsAppConfig = whatsAppConfig;
        this.httpClient = whatsAppHttpClient;
    }

    public Map<String, Object> sendTemplateMessage(String phoneNumber, String templateName, String languageCode, String[] templateParameters) {
        try {
            String requestBody = buildTemplateRequestBody(phoneNumber, templateName, languageCode, templateParameters);
            logger.debug("Sending WhatsApp template message to: {}", phoneNumber);
            
            String endpoint = whatsAppConfig.getMessageEndpoint();
            logger.info("WhatsApp API Endpoint: {}", endpoint);
            logger.info("Phone Number ID: {}", whatsAppConfig.getPhoneNumberId());
            logger.info("API Version: {}", whatsAppConfig.getApiVersion());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(endpoint))
                    .header("Authorization", "Bearer " + whatsAppConfig.getAccessToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return handleResponse(response);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("WhatsApp API request interrupted", e);
            throw new WhatsAppException("Solicitud interrumpida al enviar mensaje WhatsApp", "INTERRUPTED", 500, e);
        } catch (Exception e) {
            logger.error("Error communicating with WhatsApp API", e);
            throw new WhatsAppException("Error al comunicarse con WhatsApp API: " + e.getMessage(), "API_ERROR", 500, e);
        }
    }

    private String buildTemplateRequestBody(String phoneNumber, String templateName, String languageCode, String[] templateParameters) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", phoneNumber);
        payload.put("type", "template");

        Map<String, Object> template = new HashMap<>();
        template.put("name", templateName);

        Map<String, Object> language = new HashMap<>();
        language.put("code", languageCode != null ? languageCode : "es_MX");
        template.put("language", language);

        if (templateParameters != null && templateParameters.length > 0) {
            Map<String, Object> components = new HashMap<>();
            Map<String, Object> body = new HashMap<>();

            Map<String, Object>[] parameters = new Map[templateParameters.length];
            for (int i = 0; i < templateParameters.length; i++) {
                Map<String, Object> param = new HashMap<>();
                param.put("type", "text");
                param.put("text", templateParameters[i]);
                parameters[i] = param;
            }

            body.put("parameters", parameters);
            components.put("type", "body");
            components.put("parameters", parameters);

            template.put("components", new Object[]{components});
        }

        payload.put("template", template);

        return objectMapper.writeValueAsString(payload);
    }

    private Map<String, Object> handleResponse(HttpResponse<String> response) throws Exception {
        int statusCode = response.statusCode();
        String responseBody = response.body();

        logger.debug("WhatsApp API response status: {}", statusCode);

        if (statusCode >= 200 && statusCode < 300) {
            return parseSuccessResponse(responseBody);
        } else {
            handleErrorResponse(statusCode, responseBody);
            return null;
        }
    }

    private Map<String, Object> parseSuccessResponse(String responseBody) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);

        if (response.containsKey("messages")) {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> messages = (java.util.List<Map<String, Object>>) response.get("messages");
            if (!messages.isEmpty()) {
                result.put("messageId", messages.get(0).get("id"));
            }
        }

        result.put("status", "sent");
        result.put("timestamp", System.currentTimeMillis());

        logger.info("WhatsApp message sent successfully. Message ID: {}", result.get("messageId"));

        return result;
    }

    private void handleErrorResponse(int statusCode, String responseBody) throws Exception {
        logger.error("WhatsApp API error. Status: {}. Body: {}", statusCode, responseBody);

        try {
            WhatsAppErrorResponse errorResponse = objectMapper.readValue(responseBody, WhatsAppErrorResponse.class);
            WhatsAppErrorResponse.ErrorDetail error = errorResponse.getError();

            if (error != null) {
                String errorMessage = error.getMessage() != null ? error.getMessage() : "Error desconocido";
                String errorCode = error.getCode() != null ? error.getCode().toString() : "UNKNOWN";

                throw new WhatsAppException(
                        String.format("WhatsApp API Error (%s): %s", error.getCode(), errorMessage),
                        errorCode,
                        statusCode
                );
            }
        } catch (WhatsAppException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Could not parse WhatsApp error response", e);
        }

        throw new WhatsAppException(
                String.format("WhatsApp API retornó error HTTP %d", statusCode),
                "HTTP_ERROR",
                statusCode
        );
    }
}
