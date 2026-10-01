package com.lealtixservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lealtixservice.dto.ClientOrderDTO;
import com.lealtixservice.dto.ClientOrderItemDTO;
import com.lealtixservice.dto.OrderSseEventDTO;
import com.lealtixservice.enums.OrderStatus;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSseService {

    private static final long SSE_TIMEOUT = 30L * 60L * 1000L; // 30 minutos
    private static final String PING_EVENT = "ping";
    private static final String NEW_ORDER_EVENT = "new-order";
    private static final String ORDER_STATUS_CHANGED_EVENT = "order-status-changed";

    private final ObjectMapper objectMapper;

    // Map tenantId -> lista de emitters activos
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByTenant = new ConcurrentHashMap<>();

    /**
     * Registra un nuevo SseEmitter para el tenant dado.
     * Configura callbacks de limpieza en complete/timeout/error.
     */
    public SseEmitter subscribe(Long tenantId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emittersByTenant.computeIfAbsent(tenantId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("[SSE] Cliente suscrito al tenant {}. Total emitters activos: {}", tenantId,
                emittersByTenant.get(tenantId).size());

        Runnable cleanup = () -> {
            removeEmitter(tenantId, emitter);
            log.info("[SSE] Emitter removido para tenant {}. Restantes: {}",
                    tenantId, emittersByTenant.getOrDefault(tenantId, new CopyOnWriteArrayList<>()).size());
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> {
            log.warn("[SSE] Error en emitter del tenant {}: {}", tenantId, e.getMessage());
            cleanup.run();
        });

        // Enviar evento inicial de conexión establecida
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("status", "connected", "tenantId", tenantId, "timestamp", Instant.now().toString())));
        } catch (IOException e) {
            log.warn("[SSE] No se pudo enviar evento 'connected' al tenant {}: {}", tenantId, e.getMessage());
            emitter.complete();
        }

        return emitter;
    }

    /**
     * Envía evento 'new-order' solo cuando la orden viene de CHATBOT y está en PENDIENTE.
     * Construye el payload completo con toda la información requerida por el frontend.
     */
    public void publishNewChatbotOrder(ClientOrderDTO order) {
        Long tenantId = order.getTenantId();
        List<SseEmitter> emitters = emittersByTenant.getOrDefault(tenantId, new CopyOnWriteArrayList<>());

        if (emitters.isEmpty()) {
            log.debug("[SSE] No hay suscriptores activos para tenant {}. Evento descartado.", tenantId);
            return;
        }

        try {
            // Construir el evento SSE completo con toda la información
            OrderSseEventDTO event = buildOrderSseEvent(order);
            String payload = objectMapper.writeValueAsString(event);
            
            log.info("[SSE] Enviando evento new-order para tenant {}, orden {}", tenantId, order.getId());
            sendToTenant(tenantId, NEW_ORDER_EVENT, payload);
        } catch (Exception e) {
            log.error("[SSE] Error serializando orden para SSE, tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }

    /**
     * Publica un evento de cambio de estado de orden (para cocina)
     */
    public void publishOrderStatusChanged(ClientOrderDTO order) {
        Long tenantId = order.getTenantId();
        List<SseEmitter> emitters = emittersByTenant.getOrDefault(tenantId, new CopyOnWriteArrayList<>());

        if (emitters.isEmpty()) {
            log.debug("[SSE] No hay suscriptores activos para tenant {}. Evento de cambio de estado descartado.", tenantId);
            return;
        }

        try {
            // Construir el evento SSE completo con toda la información
            OrderSseEventDTO event = buildOrderSseEvent(order);
            event.setType(ORDER_STATUS_CHANGED_EVENT);
            String payload = objectMapper.writeValueAsString(event);
            
            log.info("[SSE] Enviando evento order-status-changed para tenant {}, orden {}", tenantId, order.getId());
            sendToTenant(tenantId, ORDER_STATUS_CHANGED_EVENT, payload);
        } catch (Exception e) {
            log.error("[SSE] Error serializando cambio de estado para SSE, tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }

    /**
     * Construye el DTO del evento SSE con toda la información completa de la orden
     */
    private OrderSseEventDTO buildOrderSseEvent(ClientOrderDTO order) {
        Instant timestamp = Instant.now();
        
        // Mapear items
        List<OrderSseEventDTO.OrderItemDataDTO> items = new ArrayList<>();
        if (order.getItems() != null) {
            items = order.getItems().stream()
                    .map(this::mapOrderItem)
                    .collect(Collectors.toList());
        }

        // Mapear estado de español a inglés para el frontend
        String estadoEn = mapEstadoToEnglish(order.getEstado());

        // Construir datos de la orden
        OrderSseEventDTO.OrderDataDTO orderData = OrderSseEventDTO.OrderDataDTO.builder()
                .id(order.getId().toString())
                .tenantId(order.getTenantId())
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .estado(estadoEn)
                .source(order.getSource() != null ? order.getSource() : "CHATBOT")
                .subtotal(order.getSubtotal() != null ? order.getSubtotal() : BigDecimal.ZERO)
                .descuento(order.getDescuento() != null ? order.getDescuento() : BigDecimal.ZERO)
                .total(order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO)
                .items(items)
                .couponCode(order.getCouponCode())
                .couponId(order.getCouponId() != null ? order.getCouponId().toString() : null)
                .couponDiscount(order.getCouponDiscount() != null ? order.getCouponDiscount() : BigDecimal.ZERO)
                .fecha(order.getFecha() != null ? order.getFecha().toInstant(ZoneOffset.UTC) : timestamp)
                .build();

        // Construir metadata
        OrderSseEventDTO.MetaDTO meta = OrderSseEventDTO.MetaDTO.builder()
                .origin("CHATBOT")
                .build();

        // Construir evento completo
        return OrderSseEventDTO.builder()
                .type(NEW_ORDER_EVENT)
                .tenantId(order.getTenantId())
                .timestamp(timestamp)
                .meta(meta)
                .order(orderData)
                .build();
    }

    /**
     * Mapea un item de orden al DTO del evento SSE
     */
    private OrderSseEventDTO.OrderItemDataDTO mapOrderItem(ClientOrderItemDTO item) {
        return OrderSseEventDTO.OrderItemDataDTO.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .cantidad(item.getCantidad())
                .precioUnitario(item.getPrecioUnitario() != null ? item.getPrecioUnitario() : BigDecimal.ZERO)
                .comentarios(item.getComentarios())
                .build();
    }

    /**
     * Mapea el estado de la orden de español (BD) a inglés (frontend)
     */
    private String mapEstadoToEnglish(OrderStatus estado) {
        if (estado == null) return "PENDING";
        
        return switch (estado) {
            case PENDIENTE -> "PENDING";
            case PAGADA -> "PAID";
            case CANCELADA -> "CANCELLED";
            case CONFIRMADA -> "CONFIRMADA";
            case EN_PREPARACION -> "EN_PREPARACION";
            case LISTO -> "LISTO";
        };
    }

    /**
     * Ping cada 30 segundos a todos los tenants con suscriptores activos.
     * Evita que proxies/load balancers cierren la conexión por inactividad.
     */
    @Scheduled(fixedDelay = 30_000)
    public void sendHeartbeat() {
        emittersByTenant.forEach((tenantId, emitters) -> {
            if (!emitters.isEmpty()) {
                String pingData = "{\"timestamp\":\"" + Instant.now().toString() + "\"}";
                sendToTenant(tenantId, PING_EVENT, pingData);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private void sendToTenant(Long tenantId, String eventName, String data) {
        List<SseEmitter> emitters = emittersByTenant.getOrDefault(tenantId, new CopyOnWriteArrayList<>());
        List<SseEmitter> dead = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                log.warn("[SSE] Emitter muerto en tenant {}, removiendo. Causa: {}", tenantId, e.getMessage());
                dead.add(emitter);
            }
        }

        dead.forEach(e -> removeEmitter(tenantId, e));
    }

    private void removeEmitter(Long tenantId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emittersByTenant.get(tenantId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emittersByTenant.remove(tenantId);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("[SSE] Cerrando todos los emitters SSE activos...");
        emittersByTenant.forEach((tenantId, emitters) ->
                emitters.forEach(SseEmitter::complete));
        emittersByTenant.clear();
    }
}
