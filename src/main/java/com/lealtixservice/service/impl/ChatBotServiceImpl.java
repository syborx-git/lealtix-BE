package com.lealtixservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lealtixservice.dto.*;
import com.lealtixservice.entity.*;
import com.lealtixservice.enums.CouponStatus;
import com.lealtixservice.enums.RedemptionChannel;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.repository.*;
import com.lealtixservice.service.ChatBotService;
import com.lealtixservice.service.ClientOrderService;
import com.lealtixservice.service.CouponRedemptionService;
import com.lealtixservice.service.CouponValidationService;
import com.lealtixservice.service.ProductCrossSellingService;
import com.lealtixservice.service.TenantCustomerService;
import com.lealtixservice.util.TenantCustomerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio ChatBot (Mesero Virtual).
 * Orquesta la validación de clientes, sugerencias de productos, validación de cupones
 * y creación de órdenes desde el ChatBot.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ChatBotServiceImpl implements ChatBotService {

    private static final String DEMO_TENANT_SLUG = "demo";
    private static final String DEMO_TENANT_NAME = "Tenant Demo";
    private static final String DEMO_TENANT_UID = "UID-DEMO";

    private final ChatBotSessionRepository sessionRepository;
    private final ChatBotMessageRepository messageRepository;
    private final TenantCustomerRepository customerRepository;
    private final TenantRepository tenantRepository;
    private final ClientOrderRepository orderRepository;
    private final ClientOrderItemRepository orderItemRepository;
    private final CouponRepository couponRepository;
    private final TenantMenuProductRepository productRepository;
    private final CouponValidationService couponValidationService;
    private final CouponRedemptionService couponRedemptionService;
    private final ProductCrossSellingService crossSellingService;
    private final ClientOrderService clientOrderService;
    private final TenantCustomerService tenantCustomerService;
    private final ObjectMapper objectMapper;

    @Override
    public ChatBotSession getOrCreateSession(Long tenantId, String sessionId, String phone, String email) {
        log.info("Obteniendo o creando sesión ChatBot: sessionId={}, tenantId={}", sessionId, tenantId);
        Tenant tenant = resolveChatBotTenant(tenantId);
        
        return sessionRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    ChatBotSession newSession = ChatBotSession.builder()
                            .sessionId(sessionId)
                            .tenant(tenant)
                            .phone(phone)
                            .email(email)
                            .status("ACTIVE")
                            .startedAt(LocalDateTime.now())
                            .lastInteractionAt(LocalDateTime.now())
                            .createdAt(LocalDateTime.now())
                            .build();
                    
                    return sessionRepository.save(newSession);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerValidationResponseDTO validateCustomer(Long tenantId, String phone, String email) {
        log.info("Validando cliente en ChatBot: tenantId={}, phone={}, email={}", tenantId, phone, email);
        Long resolvedTenantId = resolveExistingChatBotTenant(tenantId).getId();
        
        // Buscar cliente por teléfono o email
        Optional<TenantCustomer> customerOpt = Optional.empty();
        
        if (phone != null && !phone.trim().isEmpty()) {
            customerOpt = customerRepository.findByPhoneAndTenantId(phone.trim(), resolvedTenantId);
        }
        
        if (customerOpt.isEmpty() && email != null && !email.trim().isEmpty()) {
            customerOpt = customerRepository.findByEmailAndTenantId(email.trim(), resolvedTenantId);
        }
        
        if (customerOpt.isEmpty()) {
            return CustomerValidationResponseDTO.builder()
                    .exists(false)
                    .build();
        }
        
        TenantCustomer customer = customerOpt.get();
        
        // Calcular LTV (suma de todas las órdenes del cliente)
        BigDecimal ltv = calculateCustomerLTV(customer.getId());
        
        // Contar órdenes
        Integer orderCount = orderRepository.findByCustomerIdOrderByFechaDesc(customer.getId()).size();
        
        // Obtener cupones activos
        List<CouponResponseDTO> activeCoupons = getActiveCoupons(customer.getId());
        
        // Obtener "lo de siempre" (última orden)
        List<CustomerValidationResponseDTO.ProductSuggestionDTO> lastOrderProducts = 
                getLastOrderProducts(customer.getId(), resolvedTenantId);
        
        return CustomerValidationResponseDTO.builder()
                .exists(true)
                .customer(TenantCustomerMapper.toDTO(customer))
                .ltv(ltv)
                .orderCount(orderCount)
                .activeCoupons(activeCoupons)
                .lastOrderProducts(lastOrderProducts)
                .frequentProducts(Collections.emptyList()) // Puede extenderse en el futuro
                .build();
    }

    @Override
    public TenantCustomerDTO registerCustomer(QuickCustomerRegistrationDTO request) {
        log.info("Registro rápido de cliente desde ChatBot: email={}, tenantId={}", 
                 request.getEmail(), request.getTenantId());
        
        Tenant tenant = resolveChatBotTenant(request.getTenantId());
        
        // Crear el cliente
        TenantCustomer customer = TenantCustomer.builder()
                .tenant(tenant)
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .gender(request.getGender())
                .birthDate(request.getBirthDate())
                .acceptedPromotions(request.getAcceptedPromotions())
                .acceptedAt(request.getAcceptedPromotions() ? LocalDate.now() : null)
                .active(true)
                .build();
        
        TenantCustomer savedCustomer = tenantCustomerService.save(customer);
        
        log.info("Cliente registrado exitosamente desde ChatBot con validación de promoción: customerId={}", 
                 savedCustomer.getId());
        
        return TenantCustomerMapper.toDTO(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerValidationResponseDTO.ProductSuggestionDTO> getLastOrderProducts(Long customerId, Long tenantId) {
        log.debug("Obteniendo productos de la última orden: customerId={}, tenantId={}", customerId, tenantId);
        Long resolvedTenantId = resolveExistingChatBotTenant(tenantId).getId();
        
        Optional<ClientOrder> lastOrderOpt = orderRepository.findFirstByCustomerIdAndTenantIdOrderByFechaDesc(customerId, resolvedTenantId);
        
        if (lastOrderOpt.isEmpty()) {
            return Collections.emptyList();
        }
        
        ClientOrder lastOrder = lastOrderOpt.get();
        
        return lastOrder.getItems().stream()
                .map(item -> CustomerValidationResponseDTO.ProductSuggestionDTO.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getNombre())
                        .description(item.getProduct().getDescripcion())
                        .price(item.getPrecioUnitario())
                        .imageUrl(item.getProduct().getImgUrl())
                        .quantity(item.getCantidad())
                        .comments(item.getComentarios())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerValidationResponseDTO.ProductSuggestionDTO> getCrossSellingSuggestions(Long productId, Long tenantId) {
        log.debug("Obteniendo sugerencias de venta cruzada: productId={}, tenantId={}", productId, tenantId);
        Long resolvedTenantId = resolveExistingChatBotTenant(tenantId).getId();
        
        List<CrossSellingDTO> suggestions = crossSellingService.getSuggestionsByProduct(productId, resolvedTenantId);
        
        return suggestions.stream()
                .map(suggestion -> CustomerValidationResponseDTO.ProductSuggestionDTO.builder()
                        .productId(suggestion.getId())
                        .productName(suggestion.getName())
                        .description(suggestion.getDescription())
                        .price(suggestion.getPrice())
                        .imageUrl(suggestion.getImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String couponCode, Long tenantId) {
        log.info("Validando cupón en ChatBot: couponCode={}, tenantId={}", couponCode, tenantId);
        Long resolvedTenantId = resolveExistingChatBotTenant(tenantId).getId();
        return couponValidationService.validateCouponByCode(couponCode, resolvedTenantId);
    }

    @Override
    @Transactional
    public ChatBotCouponRedemptionResponse redeemCouponFromChatBot(ChatBotRedeemCouponRequest request) {
        log.info("Redimiendo cupón desde ChatBot: couponCode={}, customerId={}, tenantId={}", 
                 request.getCouponCode(), request.getCustomerId(), request.getTenantId());
        
        try {
            Long resolvedTenantId = resolveChatBotTenant(request.getTenantId()).getId();

            // Obtener el cliente
            TenantCustomer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + request.getCustomerId()));
            
            // Preparar el request de redención con los parámetros necesarios
            String redeemedBy = customer.getEmail();
            RedemptionChannel channel = RedemptionChannel.CHATBOT;
            BigDecimal originalAmount = request.getOrderTotal();
            
            String metadata = String.format("ChatBot redención - SessionId: %s, OrderTotal: %s", 
                                           request.getSessionId(), originalAmount);
            
            RedeemCouponRequest redeemRequest = RedeemCouponRequest.builder()
                    .redeemedBy(redeemedBy)
                    .channel(channel)
                    .originalAmount(originalAmount)
                    .metadata(metadata)
                    .build();
            
            // Usar el servicio de redención de cupones con la misma lógica que ClientOrderService
            RedemptionResponse redemptionResponse = couponRedemptionService.redeemCouponByCode(
                    request.getCouponCode(), 
                    redeemRequest, 
                    resolvedTenantId
            );
            
            // Convertir RedemptionResponse a ChatBotCouponRedemptionResponse
            if (redemptionResponse == null || !redemptionResponse.isSuccess()) {
                String message = redemptionResponse != null && redemptionResponse.getMessage() != null
                        ? redemptionResponse.getMessage()
                        : "No se pudo redimir el cupón";
                log.warn("Error al redimir cupón: {}", message);
                return ChatBotCouponRedemptionResponse.failure(message);
            }
            
            // Construir la respuesta ChatBot con la información de la redención
            ChatBotCouponRedemptionResponse response = ChatBotCouponRedemptionResponse.builder()
                    .success(true)
                    .message("Cupón redimido exitosamente")
                    .couponCode(redemptionResponse.getCouponCode())
                    .couponId(redemptionResponse.getCouponId())
                    .campaignTitle(redemptionResponse.getCampaignTitle())
                    .redeemedAt(redemptionResponse.getRedeemedAt())
                    .discountType(redemptionResponse.getCouponType())
                    .discountDescription(redemptionResponse.getBenefit())
                    .originalAmount(redemptionResponse.getOriginalAmount())
                    .discountAmount(redemptionResponse.getDiscountAmount())
                    .finalAmount(redemptionResponse.getFinalAmount())
                    .discountValue(redemptionResponse.getCouponValue())
                    .build();
            
            log.info("Cupón redimido exitosamente desde ChatBot: code={}, discount={}, finalAmount={}", 
                     request.getCouponCode(), response.getDiscountAmount(), response.getFinalAmount());
            
            return response;
            
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al redimir cupón: {}", e.getMessage());
            return ChatBotCouponRedemptionResponse.failure(e.getMessage());
        } catch (Exception e) {
            log.error("Error al redimir cupón desde ChatBot", e);
            return ChatBotCouponRedemptionResponse.failure(e.getMessage());
        }
    }

    @Override
    public ClientOrderDTO createOrderFromChatBot(ChatBotOrderRequestDTO request) {
        log.info("Creando orden desde ChatBot: sessionId={}, tenantId={}", 
                 request.getSessionId(), request.getTenantId());
        Long resolvedTenantId = resolveChatBotTenant(request.getTenantId()).getId();
        
        // Si no hay customerId pero hay información de cliente, registrar rápidamente
        Long customerId = request.getCustomerId();
        
        if (customerId == null && request.getCustomerPhone() != null && request.getCustomerEmail() != null) {
            log.info("Cliente no identificado, realizando registro rápido");
            QuickCustomerRegistrationDTO quickReg = QuickCustomerRegistrationDTO.builder()
                    .tenantId(resolvedTenantId)
                    .name(request.getCustomerName() != null ? request.getCustomerName() : "Cliente ChatBot")
                    .email(request.getCustomerEmail())
                    .phone(request.getCustomerPhone())
                    .acceptedPromotions(true)
                    .build();
            
            TenantCustomerDTO newCustomer = registerCustomer(quickReg);
            customerId = newCustomer.getId();
        }
        
        // Resolver couponId desde couponCode (si aplica)
        Long couponId = null;
        String couponCode = request.getCouponCode();
        if (couponCode != null && !couponCode.isBlank()) {
            Coupon coupon = couponRepository.findByCodeWithRelations(couponCode)
                    .orElseThrow(() -> new IllegalArgumentException("Cupón no encontrado: " + couponCode));
            if (coupon.getCampaign() != null && !coupon.getCampaign().getBusinessId().equals(resolvedTenantId)) {
                throw new IllegalArgumentException("El cupón no pertenece al tenant especificado");
            }
            couponId = coupon.getId();
        }

        // Crear el request para el servicio de órdenes
        CreateClientOrderRequest orderRequest = CreateClientOrderRequest.builder()
                .customerId(customerId)
                .tenantId(resolvedTenantId)
                .items(request.getItems().stream()
                        .map(item -> CreateClientOrderRequest.OrderItemRequest.builder()
                                .productId(item.getProductId())
                                .cantidad(item.getCantidad())
                                .precioUnitario(item.getPrecioUnitario())
                                .comentarios(item.getComentarios())
                                .excludedIngredientIds(item.getExcludedIngredientIds())
                                .additionalIngredientIds(item.getAdditionalIngredientIds())
                                .build())
                        .collect(Collectors.toList()))
                .descuento(request.getDescuento())
                .subtotal(request.getSubtotal())
                .totalFinal(request.getTotalFinal())
                .couponId(couponId)
                .source("CHATBOT")  // ¡IMPORTANTE! Marca el origen
                .build();

        ClientOrderDTO order = clientOrderService.createOrder(orderRequest);
        
        // Actualizar la sesión con el cliente identificado si aplica
        if (customerId != null) {
            Long finalCustomerId = customerId;
            sessionRepository.findBySessionId(request.getSessionId())
                    .ifPresent(session -> {
                        customerRepository.findById(finalCustomerId)
                                .ifPresent(session::setCustomer);
                        sessionRepository.save(session);
                    });
        }
        
        // Registrar mensaje de confirmación
        logMessage(request.getSessionId(), "ORDER_CONFIRMATION", "SYSTEM", 
                   "Orden creada exitosamente: " + order.getId(), 
                   toJson(Map.of("orderId", order.getId())));
        
        log.info("Orden creada exitosamente desde ChatBot: orderId={}", order.getId());
        return order;
    }

    @Override
    public void logMessage(String sessionId, String messageType, String sender, String content, String metadata) {
        sessionRepository.findBySessionId(sessionId)
                .ifPresent(session -> {
                    ChatBotMessage message = ChatBotMessage.builder()
                            .session(session)
                            .messageType(messageType)
                            .sender(sender)
                            .content(content)
                            .metadata(metadata)
                            .timestamp(LocalDateTime.now())
                            .build();
                    
                    messageRepository.save(message);
                    log.debug("Mensaje registrado: sessionId={}, type={}, sender={}", sessionId, messageType, sender);
                });
    }

    @Override
    public void completeSession(String sessionId) {
        sessionRepository.findBySessionId(sessionId)
                .ifPresent(session -> {
                    session.complete();
                    sessionRepository.save(session);
                    log.info("Sesión completada: sessionId={}", sessionId);
                });
    }

    @Override
    public void abandonSession(String sessionId) {
        sessionRepository.findBySessionId(sessionId)
                .ifPresent(session -> {
                    session.abandon();
                    sessionRepository.save(session);
                    log.info("Sesión abandonada: sessionId={}", sessionId);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatBotMessageDTO> getSessionMessages(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
                .map(session -> messageRepository.findBySessionIdOrderByTimestampAsc(session.getId()).stream()
                        .map(message -> ChatBotMessageDTO.builder()
                                .id(message.getId())
                                .messageType(message.getMessageType())
                                .sender(message.getSender())
                                .content(message.getContent())
                                .metadata(message.getMetadata())
                                .timestamp(message.getTimestamp())
                                .build())
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private BigDecimal calculateCustomerLTV(Long customerId) {
        List<ClientOrder> orders = orderRepository.findByCustomerIdOrderByFechaDesc(customerId);
        return orders.stream()
                .map(ClientOrder::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<CouponResponseDTO> getActiveCoupons(Long customerId) {
        return couponRepository.findByCustomerId(customerId).stream()
                .filter(coupon -> CouponStatus.ACTIVE.equals(coupon.getStatus()))
                .filter(coupon -> coupon.getExpiresAt() == null || coupon.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(coupon -> CouponResponseDTO.builder()
                        .id(coupon.getId())
                        .code(coupon.getCode())
                        .status(coupon.getStatus())
                        .expiresAt(coupon.getExpiresAt())
                        .qrUrl(coupon.getQrUrl())
                        .campaignTitle(coupon.getCampaign().getTitle())
                        .rewardDescription(coupon.getCampaign().getPromotionReward().getDescription())
                        .customerName(coupon.getCustomer().getName())
                        .rewardType(coupon.getCampaign().getPromotionReward().getRewardType())
                        .minPurchaseAmount(coupon.getCampaign().getPromotionReward().getMinPurchaseAmount())
                        .usageLimit(coupon.getCampaign().getPromotionReward().getUsageLimit())
                        .usageCount(coupon.getCampaign().getPromotionReward().getUsageCount())
                        .numericValue(coupon.getCampaign().getPromotionReward().getNumericValue())
                        .productId(coupon.getCampaign().getPromotionReward().getProductId())
                        .buyQuantity(coupon.getCampaign().getPromotionReward().getBuyQuantity())
                        .freeQuantity(coupon.getCampaign().getPromotionReward().getFreeQuantity())
                        .build())
                .collect(Collectors.toList());
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Error al convertir objeto a JSON", e);
            return "{}";
        }
    }

    private Tenant resolveChatBotTenant(Long tenantId) {
        if (tenantId != null && tenantId > 0) {
            return tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado: " + tenantId));
        }

        return tenantRepository.getBySlug(DEMO_TENANT_SLUG)
                .orElseGet(this::createDemoTenant);
    }

    private Tenant resolveExistingChatBotTenant(Long tenantId) {
        if (tenantId != null && tenantId > 0) {
            return tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado: " + tenantId));
        }

        return tenantRepository.getBySlug(DEMO_TENANT_SLUG)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant demo no encontrado"));
    }

    private Tenant createDemoTenant() {
        Tenant demoTenant = Tenant.builder()
                .nombreNegocio(DEMO_TENANT_NAME)
                .direccion("Demo")
                .telefono("0000000000")
                .tipoNegocio("Demo")
                .slug(DEMO_TENANT_SLUG)
                .UIDTenant(DEMO_TENANT_UID)
                .schedules("Demo")
                .logoUrl("")
                .slogan("Tenant de demostracion")
                .kitchenModuleEnabled(false)
                .isActive(true)
                .build();

        Tenant savedTenant = tenantRepository.save(demoTenant);
        log.info("Tenant demo creado con id={}", savedTenant.getId());
        return savedTenant;
    }
}