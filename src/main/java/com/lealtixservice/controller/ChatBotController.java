package com.lealtixservice.controller;

import com.lealtixservice.dto.*;
import com.lealtixservice.service.ChatBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para el ChatBot (Mesero Virtual).
 * Proporciona endpoints para validación de clientes, registro rápido,
 * sugerencias de productos, validación de cupones y creación de órdenes.
 */
@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Tag(name = "ChatBot", description = "Endpoints para el Mesero Virtual (ChatBot)")
public class ChatBotController {

    private final ChatBotService chatBotService;

    @Operation(
        summary = "Validar cliente por teléfono o email",
        description = "Busca un cliente por teléfono o email. Si existe, retorna su información completa " +
                      "incluyendo LTV, cupones activos y 'lo de siempre' (última orden)."
    )
    @PostMapping("/validate-customer")
    public ResponseEntity<GenericResponse> validateCustomer(
            @Parameter(description = "Datos de validación") 
            @Valid @RequestBody ValidateCustomerRequest request) {
        try {
            log.info("POST /api/chatbot/validate-customer - tenantId={}, phone={}, email={}", 
                     request.getTenantId(), request.getPhone(), request.getEmail());
            
            CustomerValidationResponseDTO response = chatBotService.validateCustomer(
                    request.getTenantId(), 
                    request.getPhone(), 
                    request.getEmail()
            );
            
            String message = response.isExists() 
                    ? "Cliente encontrado" 
                    : "Cliente no encontrado";
            
            return ResponseEntity.ok(new GenericResponse(200, message, response));
            
        } catch (Exception e) {
            log.error("Error validando cliente en ChatBot", e);
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }

    @Operation(
        summary = "Registro rápido de cliente",
        description = "Crea un nuevo cliente con información básica: nombre, email, teléfono (opcional), " +
                      "género y fecha de cumpleaños. Pensado para registro durante la conversación del ChatBot."
    )
    @PostMapping("/register-customer")
    public ResponseEntity<GenericResponse> registerCustomer(
            @Parameter(description = "Datos del nuevo cliente") 
            @Valid @RequestBody QuickCustomerRegistrationDTO request) {
        try {
            log.info("POST /api/chatbot/register-customer - email={}, tenantId={}", 
                     request.getEmail(), request.getTenantId());
            
            TenantCustomerDTO customer = chatBotService.registerCustomer(request);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new GenericResponse(201, "Cliente registrado exitosamente", customer));
            
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al registrar cliente: {}", e.getMessage());
            return ResponseEntity.ok(
                new GenericResponse(400, "Error de validación: " + e.getMessage(), null)
            );
        } catch (Exception e) {
            log.error("Error registrando cliente en ChatBot", e);
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }

    @Operation(
        summary = "Obtener 'lo de siempre' del cliente",
        description = "Retorna los productos de la última orden del cliente para sugerir 'lo de siempre'."
    )
    @GetMapping("/customer/{customerId}/last-order")
    public ResponseEntity<GenericResponse> getLastOrderProducts(
            @Parameter(description = "ID del cliente") 
            @PathVariable Long customerId,
            @Parameter(description = "ID del tenant") 
            @RequestParam Long tenantId) {
        try {
            log.info("GET /api/chatbot/customer/{}/last-order - tenantId={}", customerId, tenantId);
            
            List<CustomerValidationResponseDTO.ProductSuggestionDTO> products = 
                    chatBotService.getLastOrderProducts(customerId, tenantId);
            
            String message = products.isEmpty() 
                    ? "El cliente no tiene órdenes previas" 
                    : "Productos de la última orden obtenidos";
            
            return ResponseEntity.ok(new GenericResponse(200, message, products));
            
        } catch (Exception e) {
            log.error("Error obteniendo última orden del cliente {}", customerId, e);
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }

    @Operation(
        summary = "Obtener sugerencias de venta cruzada",
        description = "Retorna productos sugeridos para complementar un producto específico."
    )
    @GetMapping("/product/{productId}/cross-sell")
    public ResponseEntity<GenericResponse> getCrossSellingSuggestions(
            @Parameter(description = "ID del producto") 
            @PathVariable Long productId,
            @Parameter(description = "ID del tenant") 
            @RequestParam Long tenantId) {
        try {
            log.info("GET /api/chatbot/product/{}/cross-sell - tenantId={}", productId, tenantId);
            
            List<CustomerValidationResponseDTO.ProductSuggestionDTO> suggestions = 
                    chatBotService.getCrossSellingSuggestions(productId, tenantId);
            
            String message = suggestions.isEmpty() 
                    ? "No hay sugerencias para este producto" 
                    : "Sugerencias de venta cruzada obtenidas";
            
            return ResponseEntity.ok(new GenericResponse(200, message, suggestions));
            
        } catch (Exception e) {
            log.error("Error obteniendo sugerencias de venta cruzada para producto {}", productId, e);
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }

    @Operation(
        summary = "Validar cupón",
        description = "Valida si un cupón está activo, no ha expirado y pertenece al tenant."
    )
    @PostMapping("/validate-coupon")
    public ResponseEntity<GenericResponse> validateCoupon(
            @Parameter(description = "Datos de validación de cupón") 
            @Valid @RequestBody ValidateCouponRequest request) {
        try {
            log.info("POST /api/chatbot/validate-coupon - couponCode={}, tenantId={}", 
                     request.getCouponCode(), request.getTenantId());
            
            CouponValidationResponse response = chatBotService.validateCoupon(
                    request.getCouponCode(), 
                    request.getTenantId()
            );
            
            return ResponseEntity.ok(new GenericResponse(200, "Cupón validado", response));
            
        } catch (Exception e) {
            log.error("Error validando cupón en ChatBot", e);
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }

    @Operation(
        summary = "Redimir cupón desde ChatBot",
        description = "Redime un cupón aplicando descuentos por porcentaje, monto fijo o 2x1. " +
                      "Retorna información detallada del descuento aplicado para que el frontend " +
                      "pueda actualizar el total de la orden."
    )
    @PostMapping("/redeem-coupon")
    public ResponseEntity<GenericResponse> redeemCoupon(
            @Parameter(description = "Datos de redención de cupón") 
            @Valid @RequestBody ChatBotRedeemCouponRequest request) {
        try {
            log.info("POST /api/chatbot/redeem-coupon - couponCode={}, tenantId={}, customerId={}", 
                     request.getCouponCode(), request.getTenantId(), request.getCustomerId());
            
            ChatBotCouponRedemptionResponse response = chatBotService.redeemCouponFromChatBot(request);
            
            return ResponseEntity.ok(
                new GenericResponse(200, "Cupón redimido exitosamente", response)
            );
            
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al redimir cupón: {}", e.getMessage());
            return ResponseEntity.ok(
                new GenericResponse(400, "Error de validación: " + e.getMessage(), null)
            );
        } catch (IllegalStateException e) {
            log.warn("Cupón no puede ser redimido: {}", e.getMessage());
            return ResponseEntity.ok(
                new GenericResponse(422, e.getMessage(), null)
            );
        } catch (Exception e) {
            log.error("Error redimiendo cupón en ChatBot", e);
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }

    @Operation(
        summary = "Crear orden desde ChatBot",
        description = "Crea una nueva orden marcada con source='CHATBOT'. Si el cliente no está identificado " +
                      "pero se proporciona información de contacto, se realiza un registro rápido automáticamente."
    )
    @PostMapping("/create-order")
    public ResponseEntity<GenericResponse> createOrder(
            @Parameter(description = "Datos de la orden") 
            @Valid @RequestBody ChatBotOrderRequestDTO request) {
        try {
            log.info("POST /api/chatbot/create-order - sessionId={}, tenantId={}", 
                     request.getSessionId(), request.getTenantId());
            
            ClientOrderDTO order = chatBotService.createOrderFromChatBot(request);
            
            // Completar la sesión automáticamente
            chatBotService.completeSession(request.getSessionId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new GenericResponse(201, "Orden creada exitosamente desde ChatBot", order));
            
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al crear orden: {}", e.getMessage());
            return ResponseEntity.ok(
                new GenericResponse(400, "Error de validación: " + e.getMessage(), null)
            );
        } catch (Exception e) {
            log.error("Error creando orden desde ChatBot", e);
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }

    @Operation(
        summary = "Obtener historial de mensajes de una sesión",
        description = "Retorna todos los mensajes de una sesión del ChatBot."
    )
    @GetMapping("/session/{sessionId}/messages")
    public ResponseEntity<GenericResponse> getSessionMessages(
            @Parameter(description = "ID de la sesión") 
            @PathVariable String sessionId) {
        try {
            log.info("GET /api/chatbot/session/{}/messages", sessionId);
            
            List<ChatBotMessageDTO> messages = chatBotService.getSessionMessages(sessionId);
            
            return ResponseEntity.ok(
                new GenericResponse(200, "Mensajes de la sesión obtenidos", messages)
            );
            
        } catch (Exception e) {
            log.error("Error obteniendo mensajes de la sesión {}", sessionId, e);
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }

    @Operation(
        summary = "Abandonar sesión",
        description = "Marca una sesión como abandonada."
    )
    @PostMapping("/session/{sessionId}/abandon")
    public ResponseEntity<GenericResponse> abandonSession(
            @Parameter(description = "ID de la sesión") 
            @PathVariable String sessionId) {
        try {
            log.info("POST /api/chatbot/session/{}/abandon", sessionId);
            
            chatBotService.abandonSession(sessionId);
            
            return ResponseEntity.ok(
                new GenericResponse(200, "Sesión abandonada", null)
            );
            
        } catch (Exception e) {
            log.error("Error abandonando sesión {}", sessionId, e);
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }

    // ==================== DTOs AUXILIARES ====================

    /**
     * DTO para validar cliente
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ValidateCustomerRequest {
        @jakarta.validation.constraints.NotNull(message = "tenantId es requerido")
        private Long tenantId;
        private String phone;
        private String email;
    }

    /**
     * DTO para validar cupón
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ValidateCouponRequest {
        @jakarta.validation.constraints.NotNull(message = "tenantId es requerido")
        private Long tenantId;
        @jakarta.validation.constraints.NotBlank(message = "couponCode es requerido")
        private String couponCode;
    }
}
