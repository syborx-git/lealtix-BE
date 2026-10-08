package com.lealtixservice.service.impl;

import com.lealtixservice.dto.ClientOrderDTO;
import com.lealtixservice.dto.CreateClientOrderRequest;
import com.lealtixservice.dto.RecordPaymentRequest;
import com.lealtixservice.dto.RedeemCouponRequest;
import com.lealtixservice.dto.RedemptionResponse;
import com.lealtixservice.dto.SalesReportRowDTO;
import com.lealtixservice.dto.SplitOrderRequest;
import com.lealtixservice.dto.SplitOrderResponse;
import com.lealtixservice.entity.AppUser;
import com.lealtixservice.entity.ClientOrder;
import com.lealtixservice.entity.ClientOrderItem;
import com.lealtixservice.entity.Coupon;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.entity.TenantUser;
import com.lealtixservice.enums.CouponStatus;
import com.lealtixservice.enums.OrderStatus;
import com.lealtixservice.enums.PaymentMethod;
import com.lealtixservice.enums.RedemptionChannel;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.entity.Mesa;
import com.lealtixservice.entity.Pago;
import com.lealtixservice.entity.Turno;
import com.lealtixservice.enums.MesaEstado;
import com.lealtixservice.mapper.ClientOrderItemMapper;
import com.lealtixservice.mapper.ClientOrderMapper;
import com.lealtixservice.repository.MesaRepository;
import com.lealtixservice.repository.PagoRepository;
import com.lealtixservice.repository.TurnoRepository;
import com.lealtixservice.repository.AppUserRepository;
import com.lealtixservice.repository.ClientOrderItemRepository;
import com.lealtixservice.repository.ClientOrderRepository;
import com.lealtixservice.repository.CouponRepository;
import com.lealtixservice.repository.TenantCustomerRepository;
import com.lealtixservice.repository.TenantMenuProductRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.repository.TenantUserRepository;
import com.lealtixservice.service.ClientOrderService;
import com.lealtixservice.service.CouponRedemptionService;
import com.lealtixservice.service.InventoryService;
import com.lealtixservice.service.OrderSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.lealtixservice.entity.Insumo;
import com.lealtixservice.repository.InsumoRepository;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ClientOrderServiceImpl implements ClientOrderService {

    /** Prórroga de edición de comandas enviadas (en minutos) */
    public static final int EDIT_WINDOW_MINUTES = 3;
    private static final long EDIT_WINDOW_SECONDS = EDIT_WINDOW_MINUTES * 60L;

    private final ClientOrderRepository clientOrderRepository;
    private final ClientOrderItemRepository clientOrderItemRepository;
    private final TenantCustomerRepository tenantCustomerRepository;
    private final TenantMenuProductRepository tenantMenuProductRepository;
    private final TenantRepository tenantRepository;
    private final CouponRepository couponRepository;
    private final AppUserRepository appUserRepository;
    private final TenantUserRepository tenantUserRepository;
    private final CouponRedemptionService couponRedemptionService;
    private final OrderSseService orderSseService;
    private final InventoryService inventoryService;
    private final TurnoRepository turnoRepository;
    private final PagoRepository pagoRepository;
    private final MesaRepository mesaRepository;
    private final InsumoRepository insumoRepository;

    private Set<Long> getBeverageProductIds(Long tenantId) {
        if (tenantId == null) return java.util.Collections.emptySet();
        try {
            return insumoRepository.findByTenantIdAndIsActiveTrueOrderByNombreAsc(tenantId).stream()
                    .filter(Insumo::isEsBebida)
                    .map(Insumo::getProductoId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Error resolviendo catálogo de bebidas para tenant {}: {}", tenantId, e.getMessage());
            return java.util.Collections.emptySet();
        }
    }

    @Override
    public ClientOrderDTO createOrder(CreateClientOrderRequest request) {
        log.info("Creando nueva orden para cliente {} en tenant {}", request.getCustomerId(), request.getTenantId());

        // Validar que el cliente existe si se proporciona un customerId
        TenantCustomer customer = null;
        if (request.getCustomerId() != null) {
            customer = tenantCustomerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + request.getCustomerId()));
        }

        // Validar que el tenant existe
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado con ID: " + request.getTenantId()));

        // Validar que el cliente pertenece al tenant (solo si existe cliente)
        if (customer != null && !customer.getTenant().getId().equals(tenant.getId())) {
            throw new IllegalArgumentException("El cliente no pertenece al tenant especificado");
        }

        // Validar que hay items
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("La orden debe contener al menos un item");
        }

        // Pre-cargar productos en lote para validar y mapear sin N+1
        java.util.Set<Long> productIds = request.getItems().stream()
                .map(CreateClientOrderRequest.OrderItemRequest::getProductId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, TenantMenuProduct> productMap = tenantMenuProductRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(TenantMenuProduct::getId, p -> p));

        // Validar stock disponible antes de crear la orden
        for (CreateClientOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            TenantMenuProduct prod = productMap.get(itemRequest.getProductId());
            if (prod == null) {
                throw new ResourceNotFoundException("Producto no encontrado con ID: " + itemRequest.getProductId());
            }
            if (!prod.isActive()) {
                throw new IllegalArgumentException("El producto '" + prod.getNombre() + "' no está disponible actualmente");
            }
            double qty = itemRequest.getCantidad() != null ? itemRequest.getCantidad().doubleValue() : 1.0;
            if (!inventoryService.hasStock(itemRequest.getProductId(), qty)) {
                throw new IllegalArgumentException("El producto '" + prod.getNombre() + "' está agotado o no hay stock suficiente");
            }
        }

// Crear la orden inicial
        final ClientOrder initialOrder = ClientOrderMapper.toEntity(request, customer, tenant);

        // Asociar Mesa si viene en el request
        if (request.getMesaId() != null) {
            mesaRepository.findById(request.getMesaId()).ifPresent(mesa -> {
                initialOrder.setMesa(mesa);
                mesa.setEstado(MesaEstado.OCUPADA);
                mesaRepository.save(mesa);
            });
        }

        // Asociar Mesero si viene en el request
        if (request.getMeseroId() != null) {
            appUserRepository.findById(request.getMeseroId()).ifPresent(initialOrder::setMesero);
            if (initialOrder.getMesero() == null) {
                tenantUserRepository.findById(request.getMeseroId()).ifPresent(tu -> {
                    AppUser au = appUserRepository.findByEmail(tu.getEmail());
                    if (au != null) {
                        initialOrder.setMesero(au);
                    }
                });
            }
        } else if (request.getMeseroEmail() != null && !request.getMeseroEmail().isBlank()) {
            AppUser au = appUserRepository.findByEmail(request.getMeseroEmail());
            if (au != null) {
                initialOrder.setMesero(au);
            }
        }

        // Guardar la orden primero para obtener el ID
        ClientOrder savedOrder = clientOrderRepository.save(initialOrder);
        
        // Variable final para usar en el lambda
        final ClientOrder finalOrder = savedOrder;

        // Crear y guardar los items usando el productMap ya precargado en memoria
        List<ClientOrderItem> items = request.getItems().stream()
                .map(itemRequest -> {
                    TenantMenuProduct product = productMap.get(itemRequest.getProductId());
                    if (product == null) {
                        throw new ResourceNotFoundException("Producto no encontrado con ID: " + itemRequest.getProductId());
                    }
                    return ClientOrderItemMapper.toEntity(itemRequest, finalOrder, product);
                })
                .collect(Collectors.toList());

        items = clientOrderItemRepository.saveAll(items);
        savedOrder.setItems(items);

        Set<Long> beverageProductIds = getBeverageProductIds(tenant.getId());
        boolean hasBarra = items.stream().anyMatch(i -> i.getProduct() != null && beverageProductIds.contains(i.getProduct().getId()));
        boolean hasCocina = items.stream().anyMatch(i -> i.getProduct() != null && !beverageProductIds.contains(i.getProduct().getId()));

        savedOrder.setBarraEstado(hasBarra ? savedOrder.getEstado() : null);
        savedOrder.setCocinaEstado(hasCocina ? savedOrder.getEstado() : null);

        // Descontar stock del inventario conforme se confirma la comanda (sincronizando disponibilidad solo una vez)
        try {
            for (CreateClientOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
                Double qty = itemRequest.getCantidad() != null ? itemRequest.getCantidad().doubleValue() : 1.0;
                inventoryService.deductForOrder(
                        itemRequest.getProductId(),
                        qty,
                        itemRequest.getExcludedIngredientIds(),
                        itemRequest.getAdditionalIngredientIds(),
                        false);
            }
            inventoryService.syncProductAvailabilityByTenant(tenant.getId());
        } catch (Exception e) {
            log.error("Error descontando inventario para la orden {}: {}", savedOrder.getId(), e.getMessage(), e);
        }

        // Calcular montos
        BigDecimal subtotal = ClientOrderMapper.calculateSubtotal(items);
        BigDecimal descuento = request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO;
        BigDecimal total = ClientOrderMapper.calculateTotal(subtotal, descuento);

        // Actualizar la orden con los montos calculados
        savedOrder.setSubtotal(subtotal);
        savedOrder.setDescuento(descuento);
        savedOrder.setTotal(total);
        savedOrder = clientOrderRepository.save(savedOrder);

        // Redimir cupón y obtener información completa si está presente
        String couponCode = request.getCouponCode();
        BigDecimal couponDiscount = BigDecimal.ZERO;
        
        // Solo redimir coupon si hay un cliente asociado
        if (customer != null) {
            couponDiscount = redeemCouponIfPresent(request, customer, tenant, savedOrder, subtotal);
        }

        log.info("Orden creada exitosamente con ID: {}", savedOrder.getId());
        ClientOrderDTO orderDTO = ClientOrderMapper.toDTO(savedOrder, couponCode, couponDiscount, beverageProductIds);
        
        // Publicar evento SSE si la orden es de CHATBOT
        if ("CHATBOT".equalsIgnoreCase(savedOrder.getSource())) {
            try {
                orderSseService.publishNewChatbotOrder(orderDTO);
                log.info("Evento SSE publicado para orden {} del tenant {}", savedOrder.getId(), savedOrder.getTenant().getId());
            } catch (Exception e) {
                log.error("Error al publicar evento SSE para orden {}: {}", savedOrder.getId(), e.getMessage(), e);
            }
        }
        
        // Notificar a cocina en tiempo real para cualquier orden creada activa (CONFIRMADA o PENDIENTE)
        if (savedOrder.getEstado() == OrderStatus.CONFIRMADA || savedOrder.getEstado() == OrderStatus.PENDIENTE) {
            try {
                orderSseService.publishOrderStatusChanged(orderDTO);
                log.info("Evento SSE (cocina) publicado para orden {} del tenant {}", savedOrder.getId(), savedOrder.getTenant().getId());
            } catch (Exception e) {
                log.error("Error al publicar evento SSE de cocina para orden {}: {}", savedOrder.getId(), e.getMessage(), e);
            }
        }
        
        return orderDTO;
    }

    @Override
    public ClientOrderDTO updateOrder(UUID orderId, CreateClientOrderRequest request) {
        log.info("Actualizando orden {} en tenant {}", orderId, request.getTenantId());

        ClientOrder order = clientOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));

        // Validar que la orden pertenece al tenant
        if (request.getTenantId() == null) {
            throw new IllegalArgumentException("tenantId es requerido");
        }
        if (!order.getTenant().getId().equals(request.getTenantId())) {
            throw new IllegalArgumentException("La orden no pertenece al tenant especificado");
        }

        // Validar la prórroga de edición (3 minutos desde el envío)
        LocalDateTime sentAt = order.getFecha() != null ? order.getFecha() : order.getCreatedAt();
        if (sentAt != null) {
            long elapsedSeconds = Duration.between(sentAt, LocalDateTime.now()).getSeconds();
            if (elapsedSeconds > EDIT_WINDOW_SECONDS) {
                throw new IllegalArgumentException(
                        "El tiempo de prórroga de " + EDIT_WINDOW_MINUTES + " minutos para editar la comanda ha expirado");
            }
        }

        // Validar que el estado permite edición (aún no tomada por cocina / pagada / cancelada)
        if (order.getEstado() != OrderStatus.PENDIENTE && order.getEstado() != OrderStatus.CONFIRMADA) {
            throw new IllegalArgumentException(
                    "No se puede editar una comanda en estado " + order.getEstado()
                            + ". Solo pueden editarse comandas pendientes o confirmadas");
        }

        // Validar cliente (opcional) y que pertenezca al tenant
        TenantCustomer customer = null;
        if (request.getCustomerId() != null) {
            customer = tenantCustomerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + request.getCustomerId()));
            if (!customer.getTenant().getId().equals(order.getTenant().getId())) {
                throw new IllegalArgumentException("El cliente no pertenece al tenant especificado");
            }
        }

        // Validar items
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("La orden debe contener al menos un item");
        }

        // Pre-cargar productos en lote para validar y mapear sin N+1
        java.util.Set<Long> productIds = request.getItems().stream()
                .map(CreateClientOrderRequest.OrderItemRequest::getProductId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, TenantMenuProduct> productMap = tenantMenuProductRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(TenantMenuProduct::getId, p -> p));

        // Validar stock y disponibilidad antes de reemplazar los items
        for (CreateClientOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            TenantMenuProduct prod = productMap.get(itemRequest.getProductId());
            if (prod == null) {
                throw new ResourceNotFoundException("Producto no encontrado con ID: " + itemRequest.getProductId());
            }
            if (!prod.isActive()) {
                throw new IllegalArgumentException("El producto '" + prod.getNombre() + "' no está disponible actualmente");
            }
            double qty = itemRequest.getCantidad() != null ? itemRequest.getCantidad().doubleValue() : 1.0;
            if (!inventoryService.hasStock(itemRequest.getProductId(), qty)) {
                throw new IllegalArgumentException("El producto '" + prod.getNombre() + "' está agotado o no hay stock suficiente");
            }
        }

        // Restaurar el stock de la versión anterior de la comanda
        restoreStockForOrder(order);

        // Reemplazar los items (orphanRemoval elimina los anteriores).
        // IMPORTANTE: mantener la misma referencia de colección (no usar setItems)
        // para no romper el cascade="all-delete-orphan" de Hibernate.
        if (order.getItems() == null) {
            order.setItems(new java.util.ArrayList<>());
        }
        order.getItems().clear();

        final ClientOrder finalOrder = order;
        List<ClientOrderItem> items = request.getItems().stream()
                .map(itemRequest -> {
                    TenantMenuProduct product = productMap.get(itemRequest.getProductId());
                    if (product == null) {
                        throw new ResourceNotFoundException("Producto no encontrado con ID: " + itemRequest.getProductId());
                    }
                    return ClientOrderItemMapper.toEntity(itemRequest, finalOrder, product);
                })
                .collect(Collectors.toList());

        order.getItems().addAll(items);
        order.setCustomer(customer);

        // Descontar el stock de la nueva versión (sincronizando disponibilidad solo una vez)
        try {
            for (CreateClientOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
                Double qty = itemRequest.getCantidad() != null ? itemRequest.getCantidad().doubleValue() : 1.0;
                inventoryService.deductForOrder(
                        itemRequest.getProductId(),
                        qty,
                        itemRequest.getExcludedIngredientIds(),
                        itemRequest.getAdditionalIngredientIds(),
                        false);
            }
            inventoryService.syncProductAvailabilityByTenant(order.getTenant().getId());
        } catch (Exception e) {
            log.error("Error descontando inventario al actualizar la orden {}: {}", order.getId(), e.getMessage(), e);
        }

        // Recalcular montos (el cupón no se re-redime al editar)
        BigDecimal subtotal = ClientOrderMapper.calculateSubtotal(items);
        BigDecimal descuento = request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO;
        BigDecimal total = ClientOrderMapper.calculateTotal(subtotal, descuento);

        order.setSubtotal(subtotal);
        order.setDescuento(descuento);
        order.setTotal(total);
        order = clientOrderRepository.save(order);

        log.info("Orden {} actualizada exitosamente. Nuevo total: {}", orderId, total);
        ClientOrderDTO orderDTO = ClientOrderMapper.toDTO(order, request.getCouponCode(), descuento);
        try {
            orderSseService.publishOrderStatusChanged(orderDTO);
            log.info("Evento SSE (cocina) publicado al actualizar orden {} del tenant {}", order.getId(), order.getTenant().getId());
        } catch (Exception e) {
            log.error("Error al publicar evento SSE de cocina al actualizar orden {}: {}", order.getId(), e.getMessage(), e);
        }
        return orderDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClientOrderDTO> getOrderById(UUID orderId) {
        return clientOrderRepository.findById(orderId)
                .map(ClientOrderMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientOrderDTO> getOrdersByCustomer(Long customerId, Pageable pageable) {
        log.debug("Obteniendo órdenes del cliente: {}", customerId);
        return clientOrderRepository.findByCustomerIdOrderByFechaDesc(customerId)
                .stream()
                .map(ClientOrderMapper::toDTO)
                .collect(Collectors.toList())
                .stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> new org.springframework.data.domain.PageImpl<>(list, pageable, list.size())
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientOrderDTO> getOrdersByTenant(Long tenantId, Pageable pageable) {
        log.debug("Obteniendo órdenes del tenant: {}", tenantId);
        Set<Long> bevIds = getBeverageProductIds(tenantId);
        return clientOrderRepository.findByTenantId(tenantId, pageable)
                .map(o -> ClientOrderMapper.toDTO(o, bevIds));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientOrderDTO> getOrdersByTenantAndStatus(Long tenantId, OrderStatus estado, Pageable pageable) {
        log.debug("Obteniendo órdenes del tenant {} con estado: {}", tenantId, estado);
        Set<Long> bevIds = getBeverageProductIds(tenantId);
        return clientOrderRepository.findByTenantIdAndEstado(tenantId, estado, pageable)
                .map(o -> ClientOrderMapper.toDTO(o, bevIds));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientOrderDTO> getOrdersByTenantAndStatuses(Long tenantId, List<OrderStatus> estados, Pageable pageable) {
        log.debug("Obteniendo órdenes del tenant {} con estados: {}", tenantId, estados);
        Set<Long> bevIds = getBeverageProductIds(tenantId);
        return clientOrderRepository.findByTenantIdAndEstadoIn(tenantId, estados, pageable)
                .map(o -> ClientOrderMapper.toDTO(o, bevIds));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientOrderDTO> getOrdersByDateRange(Long tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Obteniendo órdenes pagadas del tenant {} entre {} y {}", tenantId, startDate, endDate);
        return clientOrderRepository.findByTenantIdAndEstadoAndFechaBetween(tenantId, OrderStatus.PAGADA, startDate, endDate)
                .stream()
                .map(ClientOrderMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ClientOrderDTO updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        return updateOrderStatus(orderId, newStatus, null, null, null);
    }

    @Override
    public ClientOrderDTO updateOrderStatus(UUID orderId, OrderStatus newStatus, String userEmail, String reason) {
        return updateOrderStatus(orderId, newStatus, userEmail, reason, null);
    }

    @Override
    public ClientOrderDTO updateOrderStatus(UUID orderId, OrderStatus newStatus, String userEmail, String reason, String area) {
        log.info("Actualizando estado de orden {} a: {} (área: {})", orderId, newStatus, area);

        ClientOrder order = clientOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));

        Set<Long> beverageProductIds = getBeverageProductIds(order.getTenant().getId());

        // Asegurar que barraEstado y cocinaEstado estén inicializados si la orden tiene items
        if (order.getBarraEstado() == null && order.getCocinaEstado() == null && order.getItems() != null && !order.getItems().isEmpty()) {
            boolean hasBarra = order.getItems().stream().anyMatch(i -> i.getProduct() != null && beverageProductIds.contains(i.getProduct().getId()));
            boolean hasCocina = order.getItems().stream().anyMatch(i -> i.getProduct() != null && !beverageProductIds.contains(i.getProduct().getId()));
            order.setBarraEstado(hasBarra ? order.getEstado() : null);
            order.setCocinaEstado(hasCocina ? order.getEstado() : null);
        }

        if (area != null && !area.isBlank()) {
            String normArea = area.trim().toUpperCase();
            if ("BARRA".equals(normArea)) {
                order.setBarraEstado(newStatus);
                if (newStatus == OrderStatus.LISTO && order.getBarraReadyAt() == null) {
                    order.setBarraReadyAt(LocalDateTime.now());
                } else if (newStatus == OrderStatus.EN_PREPARACION && order.getAcceptedAt() == null) {
                    order.setAcceptedAt(LocalDateTime.now());
                }

                // Evaluar estado general de la comanda
                if (order.getCocinaEstado() == null) {
                    // Solo tenía productos de barra
                    order.setEstado(newStatus);
                    if (newStatus == OrderStatus.LISTO && order.getReadyAt() == null) {
                        order.setReadyAt(LocalDateTime.now());
                    }
                } else {
                    // Tiene productos de barra y cocina
                    if (order.getCocinaEstado() == OrderStatus.LISTO && newStatus == OrderStatus.LISTO) {
                        order.setEstado(OrderStatus.LISTO);
                        if (order.getReadyAt() == null) {
                            order.setReadyAt(LocalDateTime.now());
                        }
                    } else if (newStatus == OrderStatus.LISTO) {
                        // Barra lista pero cocina aún en preparación / confirmada
                        order.setEstado(OrderStatus.EN_PREPARACION);
                    } else if (newStatus == OrderStatus.EN_PREPARACION) {
                        order.setEstado(OrderStatus.EN_PREPARACION);
                    }
                }
            } else if ("COCINA".equals(normArea)) {
                order.setCocinaEstado(newStatus);
                if (newStatus == OrderStatus.LISTO && order.getCocinaReadyAt() == null) {
                    order.setCocinaReadyAt(LocalDateTime.now());
                } else if (newStatus == OrderStatus.EN_PREPARACION && order.getAcceptedAt() == null) {
                    order.setAcceptedAt(LocalDateTime.now());
                }

                // Evaluar estado general de la comanda
                if (order.getBarraEstado() == null) {
                    // Solo tenía productos de cocina
                    order.setEstado(newStatus);
                    if (newStatus == OrderStatus.LISTO && order.getReadyAt() == null) {
                        order.setReadyAt(LocalDateTime.now());
                    }
                } else {
                    // Tiene productos de cocina y barra
                    if (order.getBarraEstado() == OrderStatus.LISTO && newStatus == OrderStatus.LISTO) {
                        order.setEstado(OrderStatus.LISTO);
                        if (order.getReadyAt() == null) {
                            order.setReadyAt(LocalDateTime.now());
                        }
                    } else if (newStatus == OrderStatus.LISTO) {
                        // Cocina lista pero barra aún en preparación / confirmada
                        order.setEstado(OrderStatus.EN_PREPARACION);
                    } else if (newStatus == OrderStatus.EN_PREPARACION) {
                        order.setEstado(OrderStatus.EN_PREPARACION);
                    }
                }
            }
        } else {
            // Actualización global (sin área)
            validateStatusTransition(order.getEstado(), newStatus);

            if (newStatus == OrderStatus.EN_PREPARACION && order.getAcceptedAt() == null) {
                order.setAcceptedAt(LocalDateTime.now());
            } else if (newStatus == OrderStatus.LISTO && order.getReadyAt() == null) {
                order.setReadyAt(LocalDateTime.now());
            }

            if (newStatus == OrderStatus.CONFIRMADA) {
                if (order.getBarraEstado() == OrderStatus.PENDIENTE) order.setBarraEstado(OrderStatus.CONFIRMADA);
                if (order.getCocinaEstado() == OrderStatus.PENDIENTE) order.setCocinaEstado(OrderStatus.CONFIRMADA);
            } else if (newStatus == OrderStatus.LISTO) {
                if (order.getBarraEstado() != null) order.setBarraEstado(OrderStatus.LISTO);
                if (order.getCocinaEstado() != null) order.setCocinaEstado(OrderStatus.LISTO);
            } else if (newStatus == OrderStatus.PAGADA || newStatus == OrderStatus.CANCELADA) {
                if (order.getBarraEstado() != null) order.setBarraEstado(newStatus);
                if (order.getCocinaEstado() != null) order.setCocinaEstado(newStatus);
            }

            if (newStatus == OrderStatus.PAGADA && order.getCouponId() != null && order.getCustomer() != null) {
                redeemCouponOnOrderConfirmation(order);
            }

            if (newStatus == OrderStatus.CANCELADA) {
                order.setCancelledBy(userEmail);
                order.setCancelledAt(LocalDateTime.now());
                order.setCancellationReason(reason);
                if (order.getEstado() != OrderStatus.CANCELADA) {
                    restoreStockForOrder(order);
                }
                if (order.getMesa() != null) {
                    Mesa m = order.getMesa();
                    boolean tieneOtrasActivas = clientOrderRepository.findByTenantId(order.getTenant().getId()).stream()
                            .anyMatch(o -> !o.getId().equals(order.getId())
                                    && o.getMesa() != null
                                    && o.getMesa().getId().equals(m.getId())
                                    && o.getEstado() != OrderStatus.PAGADA
                                    && o.getEstado() != OrderStatus.CANCELADA);
                    if (!tieneOtrasActivas) {
                        m.setEstado(MesaEstado.LIBRE);
                        mesaRepository.save(m);
                    }
                }
                log.info("Orden {} cancelada por {}. Razón: {}", orderId, userEmail, reason);
            }

            order.setEstado(newStatus);
        }

        order = clientOrderRepository.save(order);

        log.info("Estado de orden {} actualizado a: {} (barra: {}, cocina: {})", 
                orderId, order.getEstado(), order.getBarraEstado(), order.getCocinaEstado());

        String couponCode = null;
        BigDecimal couponDiscount = order.getDescuento() != null ? order.getDescuento() : BigDecimal.ZERO;

        if (order.getCouponId() != null) {
            Coupon coupon = couponRepository.findById(order.getCouponId()).orElse(null);
            if (coupon != null) {
                couponCode = coupon.getCode();
            }
        }

        ClientOrderDTO orderDTO = ClientOrderMapper.toDTO(order, couponCode, couponDiscount, beverageProductIds);

        // Publicar evento SSE para cambios de estado
        if (newStatus == OrderStatus.CONFIRMADA
                || newStatus == OrderStatus.EN_PREPARACION
                || newStatus == OrderStatus.LISTO
                || newStatus == OrderStatus.PAGADA
                || newStatus == OrderStatus.CANCELADA) {
            try {
                orderSseService.publishOrderStatusChanged(orderDTO);
                log.info("Evento SSE de cambio de estado publicado para orden {} del tenant {}", 
                        orderId, order.getTenant().getId());
            } catch (Exception e) {
                log.error("Error al publicar evento SSE de cambio de estado para orden {}: {}", 
                        orderId, e.getMessage(), e);
            }
        }

        return orderDTO;
    }

    @Override
    @Transactional
    public ClientOrderDTO marcharSegundoTiempo(UUID orderId) {
        log.info("Marchando segundos tiempos para orden {}", orderId);

        ClientOrder order = clientOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));

        if (order.getEstado() == OrderStatus.CANCELADA) {
            throw new IllegalArgumentException("No se puede marchar el segundo tiempo de una orden CANCELADA");
        }
        if (order.getEstado() == OrderStatus.PAGADA) {
            throw new IllegalArgumentException("No se puede marchar el segundo tiempo de una orden PAGADA");
        }

        // Actualizar comentarios de los ítems de 2do tiempo para indicar que están marchados
        List<ClientOrderItem> items = clientOrderItemRepository.findByOrderId(orderId);
        for (ClientOrderItem item : items) {
            String comment = item.getComentarios();
            if (comment != null && (comment.contains("2DO TIEMPO") || comment.contains("SEGUNDO TIEMPO"))) {
                String updatedComment = comment
                        .replace("[2DO TIEMPO ⏱️ - EN ESPERA]", "[2DO TIEMPO - MARCHADO]")
                        .replace("[2DO TIEMPO - EN ESPERA]", "[2DO TIEMPO - MARCHADO]")
                        .replace("[2DO TIEMPO]", "[2DO TIEMPO - MARCHADO]")
                        .replace("[SEGUNDO TIEMPO]", "[2DO TIEMPO - MARCHADO]");
                if (!updatedComment.contains("MARCHADO")) {
                    updatedComment = "[2DO TIEMPO - MARCHADO] " + updatedComment;
                }
                item.setComentarios(updatedComment);
                clientOrderItemRepository.save(item);
            }
        }

        // Pasar orden a CONFIRMADA para que reingrese al riel de Confirmada de Cocina
        order.setEstado(OrderStatus.CONFIRMADA);
        order = clientOrderRepository.save(order);

        log.info("Orden {} pasada a CONFIRMADA para preparación de 2do tiempo", orderId);

        String couponCode = null;
        BigDecimal couponDiscount = order.getDescuento() != null ? order.getDescuento() : BigDecimal.ZERO;
        if (order.getCouponId() != null) {
            Coupon coupon = couponRepository.findById(order.getCouponId()).orElse(null);
            if (coupon != null) {
                couponCode = coupon.getCode();
            }
        }

        ClientOrderDTO orderDTO = ClientOrderMapper.toDTO(order, couponCode, couponDiscount);

        // Notificar en tiempo real a Cocina vía SSE
        try {
            orderSseService.publishOrderStatusChanged(orderDTO);
            log.info("Evento SSE de orden {} (2do tiempo marchado) publicado para cocina", orderId);
        } catch (Exception e) {
            log.error("Error al publicar evento SSE para orden {}: {}", orderId, e.getMessage(), e);
        }

        return orderDTO;
    }

    @Override
    @Transactional
    public ClientOrderDTO marcharTercerTiempo(UUID orderId) {
        log.info("Marchando terceros tiempos para orden {}", orderId);

        ClientOrder order = clientOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));

        if (order.getEstado() == OrderStatus.CANCELADA) {
            throw new IllegalArgumentException("No se puede marchar el tercer tiempo de una orden CANCELADA");
        }
        if (order.getEstado() == OrderStatus.PAGADA) {
            throw new IllegalArgumentException("No se puede marchar el tercer tiempo de una orden PAGADA");
        }

        // Actualizar comentarios de los ítems de 3er tiempo para indicar que están marchados
        List<ClientOrderItem> items = clientOrderItemRepository.findByOrderId(orderId);
        for (ClientOrderItem item : items) {
            String comment = item.getComentarios();
            if (comment != null && (comment.contains("3ER TIEMPO") || comment.contains("TERCER TIEMPO"))) {
                String updatedComment = comment
                        .replace("[3ER TIEMPO ⏱️ - EN ESPERA]", "[3ER TIEMPO - MARCHADO]")
                        .replace("[3ER TIEMPO - EN ESPERA]", "[3ER TIEMPO - MARCHADO]")
                        .replace("[3ER TIEMPO]", "[3ER TIEMPO - MARCHADO]")
                        .replace("[TERCER TIEMPO]", "[3ER TIEMPO - MARCHADO]");
                if (!updatedComment.contains("MARCHADO")) {
                    updatedComment = "[3ER TIEMPO - MARCHADO] " + updatedComment;
                }
                item.setComentarios(updatedComment);
                clientOrderItemRepository.save(item);
            }
        }

        // Pasar orden a CONFIRMADA para que reingrese al riel de Confirmada de Cocina
        order.setEstado(OrderStatus.CONFIRMADA);
        order = clientOrderRepository.save(order);

        log.info("Orden {} pasada a CONFIRMADA para preparación de 3er tiempo", orderId);

        String couponCode = null;
        BigDecimal couponDiscount = order.getDescuento() != null ? order.getDescuento() : BigDecimal.ZERO;
        if (order.getCouponId() != null) {
            Coupon coupon = couponRepository.findById(order.getCouponId()).orElse(null);
            if (coupon != null) {
                couponCode = coupon.getCode();
            }
        }

        ClientOrderDTO orderDTO = ClientOrderMapper.toDTO(order, couponCode, couponDiscount);

        // Notificar en tiempo real a Cocina vía SSE
        try {
            orderSseService.publishOrderStatusChanged(orderDTO);
            log.info("Evento SSE de orden {} (3er tiempo marchado) publicado para cocina", orderId);
        } catch (Exception e) {
            log.error("Error al publicar evento SSE para orden {}: {}", orderId, e.getMessage(), e);
        }

        return orderDTO;
    }

    @Override
    @Transactional
    public ClientOrderDTO marcharTodosLosTiempos(UUID orderId) {
        log.info("Marchando todos los tiempos para orden {}", orderId);

        ClientOrder order = clientOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));

        if (order.getEstado() == OrderStatus.CANCELADA) {
            throw new IllegalArgumentException("No se pueden marchar tiempos de una orden CANCELADA");
        }
        if (order.getEstado() == OrderStatus.PAGADA) {
            throw new IllegalArgumentException("No se pueden marchar tiempos de una orden PAGADA");
        }

        List<ClientOrderItem> items = clientOrderItemRepository.findByOrderId(orderId);
        for (ClientOrderItem item : items) {
            String comment = item.getComentarios();
            if (comment != null) {
                boolean changed = false;
                if (comment.contains("2DO TIEMPO") || comment.contains("SEGUNDO TIEMPO")) {
                    String updated = comment
                            .replace("[2DO TIEMPO ⏱️ - EN ESPERA]", "[2DO TIEMPO - MARCHADO]")
                            .replace("[2DO TIEMPO - EN ESPERA]", "[2DO TIEMPO - MARCHADO]")
                            .replace("[2DO TIEMPO]", "[2DO TIEMPO - MARCHADO]")
                            .replace("[SEGUNDO TIEMPO]", "[2DO TIEMPO - MARCHADO]");
                    if (!updated.contains("MARCHADO")) {
                        updated = "[2DO TIEMPO - MARCHADO] " + updated;
                    }
                    comment = updated;
                    changed = true;
                }
                if (comment.contains("3ER TIEMPO") || comment.contains("TERCER TIEMPO")) {
                    String updated = comment
                            .replace("[3ER TIEMPO ⏱️ - EN ESPERA]", "[3ER TIEMPO - MARCHADO]")
                            .replace("[3ER TIEMPO - EN ESPERA]", "[3ER TIEMPO - MARCHADO]")
                            .replace("[3ER TIEMPO]", "[3ER TIEMPO - MARCHADO]")
                            .replace("[TERCER TIEMPO]", "[3ER TIEMPO - MARCHADO]");
                    if (!updated.contains("MARCHADO")) {
                        updated = "[3ER TIEMPO - MARCHADO] " + updated;
                    }
                    comment = updated;
                    changed = true;
                }
                if (changed) {
                    item.setComentarios(comment);
                    clientOrderItemRepository.save(item);
                }
            }
        }

        order.setEstado(OrderStatus.CONFIRMADA);
        order = clientOrderRepository.save(order);

        log.info("Orden {} pasada a CONFIRMADA para preparación de todos los tiempos", orderId);

        String couponCode = null;
        BigDecimal couponDiscount = order.getDescuento() != null ? order.getDescuento() : BigDecimal.ZERO;
        if (order.getCouponId() != null) {
            Coupon coupon = couponRepository.findById(order.getCouponId()).orElse(null);
            if (coupon != null) {
                couponCode = coupon.getCode();
            }
        }

        ClientOrderDTO orderDTO = ClientOrderMapper.toDTO(order, couponCode, couponDiscount);

        try {
            orderSseService.publishOrderStatusChanged(orderDTO);
            log.info("Evento SSE de orden {} (todos los tiempos marchados) publicado para cocina", orderId);
        } catch (Exception e) {
            log.error("Error al publicar evento SSE para orden {}: {}", orderId, e.getMessage(), e);
        }

        return orderDTO;
    }

    @Override
    public ClientOrderDTO cancelOrder(UUID orderId) {
        log.info("Cancelando orden: {}", orderId);
        return updateOrderStatus(orderId, OrderStatus.CANCELADA);
    }

    /**
     * Restaura el inventario descontado de cada ítem al cancelar la comanda.
     */
    private void restoreStockForOrder(ClientOrder order) {
        try {
            List<ClientOrderItem> items = clientOrderItemRepository.findByOrderId(order.getId());
            for (ClientOrderItem item : items) {
                Integer qty = item.getCantidad() != null ? item.getCantidad() : 1;
                inventoryService.restoreForOrder(
                        item.getProduct().getId(),
                        qty.doubleValue(),
                        item.getExcludedIngredientIds(),
                        item.getAdditionalIngredientIds());
            }
        } catch (Exception e) {
            log.error("Error restaurando inventario al cancelar orden {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    @Override
    public void deleteOrder(UUID orderId) {
        log.info("Eliminando orden: {}", orderId);

        ClientOrder order = clientOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));

        if (!order.getEstado().equals(OrderStatus.CANCELADA)) {
            throw new IllegalArgumentException("Solo se pueden eliminar órdenes en estado CANCELADA");
        }

        clientOrderItemRepository.deleteByOrderId(orderId);
        clientOrderRepository.delete(order);

        log.info("Orden {} eliminada exitosamente", orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalSalesByTenant(Long tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Obteniendo ventas totales del tenant {} entre {} y {}", tenantId, startDate, endDate);
        return clientOrderRepository.findByTenantIdAndEstadoAndFechaBetween(tenantId, OrderStatus.PAGADA, startDate, endDate)
                .stream()
                .map(ClientOrder::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageTicketByTenant(Long tenantId) {
        log.debug("Obteniendo ticket promedio del tenant: {}", tenantId);
        List<ClientOrder> orders = clientOrderRepository.findByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        
        if (orders.isEmpty()) {
            return 0.0;
        }

        BigDecimal totalSum = orders.stream()
                .map(ClientOrder::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalSum.divide(new BigDecimal(orders.size()), 2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    @Transactional(readOnly = true)
    public Long countOrdersByStatus(Long tenantId, OrderStatus estado) {
        return clientOrderRepository.countByTenantIdAndEstado(tenantId, estado);
    }

    /**
     * Valida las transiciones de estado permitidas
     */
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }
        // PENDIENTE puede ir a CONFIRMADA, PAGADA, CANCELADA o EN_PREPARACION
        if (currentStatus == OrderStatus.PENDIENTE) {
            if (newStatus != OrderStatus.CONFIRMADA &&
                newStatus != OrderStatus.PAGADA && 
                newStatus != OrderStatus.CANCELADA && 
                newStatus != OrderStatus.EN_PREPARACION) {
                throw new IllegalArgumentException("No se puede cambiar de PENDIENTE a " + newStatus);
            }
        }
        // EN_PREPARACION puede ir a LISTO, CANCELADA o CONFIRMADA (cuando hay segundos tiempos / ciclo parcial)
        else if (currentStatus == OrderStatus.EN_PREPARACION) {
            if (newStatus != OrderStatus.LISTO && newStatus != OrderStatus.CANCELADA && newStatus != OrderStatus.CONFIRMADA) {
                throw new IllegalArgumentException("No se puede cambiar de EN_PREPARACION a " + newStatus);
            }
        }
        // LISTO puede ir a CANCELADA o CONFIRMADA (para nueva ronda de preparación por segundos tiempos)
        else if (currentStatus == OrderStatus.LISTO) {
            if (newStatus != OrderStatus.CANCELADA && newStatus != OrderStatus.CONFIRMADA) {
                throw new IllegalArgumentException("No se puede cambiar de LISTO a " + newStatus);
            }
        }
        // PAGADA solo puede ir a CANCELADA
        else if (currentStatus == OrderStatus.PAGADA) {
            if (newStatus != OrderStatus.CANCELADA) {
                throw new IllegalArgumentException("No se puede cambiar de PAGADA a " + newStatus);
            }
        }
        // CANCELADA no puede cambiar
        else if (currentStatus == OrderStatus.CANCELADA) {
            throw new IllegalArgumentException("No se puede cambiar el estado de una orden CANCELADA");
        }
    }

    private BigDecimal redeemCouponIfPresent(CreateClientOrderRequest request,
                                       TenantCustomer customer,
                                       Tenant tenant,
                                       ClientOrder order,
                                       BigDecimal originalAmount) {
        String couponCode = request.getCouponCode();
        if (couponCode == null || couponCode.isBlank()) {
            return BigDecimal.ZERO;
        }

        // Si no hay cliente, no se puede redimir cupon (el email es obligatorio)
        if (customer == null) {
            throw new IllegalArgumentException("No se puede redimir un cupon sin un cliente asociado");
        }

        String redeemedBy = request.getRedeemedBy() != null ? request.getRedeemedBy() : customer.getEmail();
        RedemptionChannel channel = request.getRedemptionChannel() != null ? request.getRedemptionChannel() : RedemptionChannel.API;

        RedeemCouponRequest redeemRequest = RedeemCouponRequest.builder()
                .redeemedBy(redeemedBy)
                .channel(channel)
                .originalAmount(originalAmount)
                .metadata("{\"orderId\":\"" + order.getId() + "\"}")
                .build();

        RedemptionResponse response = couponRedemptionService.redeemCouponByCode(couponCode, redeemRequest, tenant.getId());
        if (response == null || !response.isSuccess()) {
            String message = response != null && response.getMessage() != null
                    ? response.getMessage()
                    : "No se pudo redimir el cupon";
            throw new IllegalArgumentException(message);
        }
        
        // Retornar el descuento del cupón desde la respuesta de redención
        return response.getDiscountAmount() != null ? response.getDiscountAmount() : BigDecimal.ZERO;
    }

    /**
     * Redime el cupón asociado a una orden cuando se confirma/paga.
     * Solo se redime si:
     * - La orden tiene un cupón asociado (couponId no null)
     * - La orden tiene un cliente asociado (no es venta general)
     * - El cupón existe y no ha sido redimido previamente
     */
    private void redeemCouponOnOrderConfirmation(ClientOrder order) {
        log.info("Intentando redimir cupón {} para orden {}", order.getCouponId(), order.getId());
        
        // Obtener el cupón por ID
        Coupon coupon = couponRepository.findById(order.getCouponId())
                .orElseThrow(() -> new ResourceNotFoundException("Cupón no encontrado con ID: " + order.getCouponId()));
        
        // Validar que el cupón no haya sido redimido previamente
        // Verificar por status = REDEEMED o por fecha de redención no vacía
        if (coupon.getStatus() == CouponStatus.REDEEMED || coupon.getRedeemedAt() != null) {
            log.warn("El cupón {} ya fue redimido previamente (status={}, redeemedAt={}). Orden: {}", 
                    coupon.getCode(), coupon.getStatus(), coupon.getRedeemedAt(), order.getId());
            return; // No lanzar error, solo advertir y continuar
        }
        
        TenantCustomer customer = order.getCustomer();
        Tenant tenant = order.getTenant();
        
        // Construir request de redención
        RedeemCouponRequest redeemRequest = RedeemCouponRequest.builder()
                .redeemedBy(customer.getEmail())
                .channel(RedemptionChannel.ORDER_CONFIRMATION)
                .originalAmount(order.getSubtotal())
                .metadata("{\"orderId\":\"" + order.getId() + "\",\"source\":\"order_confirmation\"}")
                .build();
        
        try {
            RedemptionResponse response = couponRedemptionService.redeemCouponByCode(
                    coupon.getCode(), 
                    redeemRequest, 
                    tenant.getId()
            );
            
            if (response != null && response.isSuccess()) {
                log.info("Cupón {} redimido exitosamente para orden {}. Descuento: {}", 
                        coupon.getCode(), order.getId(), response.getDiscountAmount());
            } else {
                String message = response != null ? response.getMessage() : "Error desconocido";
                log.error("Error redimiendo cupón {} para orden {}: {}", coupon.getCode(), order.getId(), message);
            }
        } catch (Exception e) {
            // No fallar el cambio de estado si la redención del cupón falla
            log.error("Error al redimir cupón {} para orden {}: {}", coupon.getCode(), order.getId(), e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public ClientOrderDTO recordPayment(UUID orderId, RecordPaymentRequest request) {
        log.info("Registrando pago para orden {} con método {}", orderId, request.getMethod());

        // ===== FASE 1: VALIDAR ORDEN =====
        ClientOrder order = clientOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));

        if (order.getEstado() == OrderStatus.PAGADA) {
            throw new IllegalArgumentException("La orden ya se encuentra PAGADA.");
        }
        if (order.getEstado() == OrderStatus.CANCELADA) {
            throw new IllegalArgumentException("No se puede registrar pago para una orden CANCELADA.");
        }

        // Validar que referencia está presente para métodos que la requieren
        if ((request.getMethod() == PaymentMethod.CARD ||
             request.getMethod() == PaymentMethod.TRANSFER ||
             request.getMethod() == PaymentMethod.MIXED) &&
            (request.getReference() == null || request.getReference().isBlank())) {
            throw new IllegalArgumentException(
                    "Referencia obligatoria para método de pago: " + request.getMethod().getDescription());
        }

        if (order.getPaidAt() != null) {
            throw new IllegalArgumentException(
                    "La orden ya fue pagada el " + order.getPaidAt());
        }

        // ===== FASE 2: VALIDAR USUARIO =====
        String email = (request.getUserEmail() != null && !request.getUserEmail().isBlank()) 
                ? request.getUserEmail().trim() 
                : "cajero@lealtix.com";

        final Long tenantId = order.getTenant().getId();
        TenantUser tenantUser = tenantUserRepository.findByEmail(email)
                .orElseGet(() -> tenantUserRepository.findAllByTenantId(tenantId).stream().findFirst().orElse(null));

        AppUser paidByUser = appUserRepository.findByEmail(email);
        if (paidByUser == null) {
            paidByUser = appUserRepository.findAll().stream().findFirst().orElse(null);
            if (paidByUser == null) {
                paidByUser = AppUser.builder()
                        .email(email)
                        .fullName(tenantUser != null ? tenantUser.getNombre() : "Usuario Sistema")
                        .isActive(true)
                        .build();
                paidByUser = appUserRepository.save(paidByUser);
            }
        }

        // ===== FASE 3: REGISTRAR PAGO (TRANSACCIÓN PRINCIPAL) =====
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank() && order.getCouponId() == null) {
            String couponCode = request.getCouponCode().trim();
            TenantCustomer customer = order.getCustomer();
            if (customer != null) {
                RedeemCouponRequest redeemRequest = RedeemCouponRequest.builder()
                        .redeemedBy(request.getUserEmail() != null ? request.getUserEmail() : customer.getEmail())
                        .channel(RedemptionChannel.COMANDIX)
                        .originalAmount(order.getSubtotal())
                        .metadata("{\"orderId\":\"" + order.getId() + "\"}")
                        .build();
                try {
                    RedemptionResponse response = couponRedemptionService.redeemCouponByCode(couponCode, redeemRequest, order.getTenant().getId());
                    if (response != null && response.isSuccess()) {
                        BigDecimal discount = response.getDiscountAmount() != null ? response.getDiscountAmount() : BigDecimal.ZERO;
                        order.setDescuento(discount);
                        BigDecimal totalConDescuento = order.getSubtotal().subtract(discount);
                        if (totalConDescuento.compareTo(BigDecimal.ZERO) < 0) {
                            totalConDescuento = BigDecimal.ZERO;
                        }
                        order.setTotal(totalConDescuento);
                        if (response.getCouponId() != null) {
                            order.setCouponId(response.getCouponId());
                        }
                        log.info("Cupón {} aplicado en cobro de orden {}. Descuento: {}", couponCode, order.getId(), discount);
                    }
                } catch (Exception ex) {
                    log.warn("No se pudo redimir cupón {} en cobro de orden {}: {}", couponCode, order.getId(), ex.getMessage());
                }
            }
        }

        if (request.getMonto() != null && request.getMonto().compareTo(BigDecimal.ZERO) > 0) {
            order.setTotal(request.getMonto());
        }

        BigDecimal propina = request.getPropina() == null ? BigDecimal.ZERO : request.getPropina();
        order.setPaidMethod(request.getMethod());
        order.setPaymentReference(request.getReference());
        order.setPaidBy(paidByUser);
        order.setPaidAt(LocalDateTime.now());
        order.setPropina(propina);
        order.setEstado(OrderStatus.PAGADA);

        if (order.getMesero() == null) {
            order.setMesero(paidByUser);
        }

        // ===== VINCULAR CON TURNO ACTIVO Y GENERAR PAGO DE CAJA =====
        Turno turno = turnoRepository.findFirstByTenantIdAndEstado(order.getTenant().getId(), "ABIERTO")
                .orElse(null);

        if (turno != null) {
            order.setTurno(turno);
            BigDecimal montoCuenta = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;
            turno.setTotalIngresos((turno.getTotalIngresos() != null ? turno.getTotalIngresos() : BigDecimal.ZERO).add(montoCuenta));
            turno.setTotalPropinas((turno.getTotalPropinas() != null ? turno.getTotalPropinas() : BigDecimal.ZERO).add(propina));
            turnoRepository.save(turno);

            TenantUser cajeroUser = tenantUser != null ? tenantUser : turno.getCajero();
            Pago pago = Pago.builder()
                    .tenant(order.getTenant())
                    .comanda(order)
                    .turno(turno)
                    .cajero(cajeroUser)
                    .metodoPago(request.getMethod() != null ? request.getMethod().name() : "CASH")
                    .montoCuenta(montoCuenta)
                    .montoPropina(propina)
                    .montoTotal(montoCuenta.add(propina))
                    .referencia(request.getReference())
                    .fecha(LocalDateTime.now())
                    .estado("APLICADO")
                    .build();
            pagoRepository.save(pago);
            log.info("Pago registrado en caja #{} para comanda {} en turno #{}", 
                    pago.getIdPago(), orderId, turno.getIdTurno());
        }

        if (order.getMesa() != null) {
            Mesa mesa = order.getMesa();
            mesa.setEstado(MesaEstado.LIBRE);
            mesaRepository.save(mesa);
        }

        order = clientOrderRepository.save(order);
        log.info("Pago registrado exitosamente para orden {} por usuario {}. Método: {}", 
                orderId, paidByUser.getEmail(), request.getMethod());

        // ===== FASE 4: REDIMIR CUPÓN (TRANSACCIÓN SEPARADA - BEST EFFORT) =====
        String couponRedemptionError = null;
        if (order.getCouponId() != null) {
            couponRedemptionError = attemptCouponRedemption(order, paidByUser);
            if (couponRedemptionError != null) {
                log.warn("Advertencia: No se pudo redimir el cupón de la orden {}. Razón: {}", orderId, couponRedemptionError);
            }
        }

        // ===== FASE 5: PUBLICAR EVENTO SSE =====
        try {
            ClientOrderDTO orderDTO = ClientOrderMapper.toDTO(order);
            orderSseService.publishOrderStatusChanged(orderDTO);
            log.info("Evento SSE publicado para orden {} en estado PAGADA del tenant {}", 
                    orderId, order.getTenant().getId());
        } catch (Exception e) {
            log.error("Error al publicar evento SSE para orden {}: {}", orderId, e.getMessage(), e);
        }

        // ===== CONSTRUIR RESPUESTA =====
        ClientOrderDTO response = ClientOrderMapper.toDTO(order);
        
        // Agregar advertencia de cupón en la respuesta si hubo error
        if (couponRedemptionError != null) {
            log.info("Orden {} pagada exitosamente, pero error al redimir cupón: {}", orderId, couponRedemptionError);
            // Nota: Si quieres agregar el error a la respuesta, puedes crear un campo en ClientOrderDTO
        }

        return response;
    }

    /**
     * Intenta redimir el cupón de una orden pagada.
     * Si falla por cualquier razón, retorna el mensaje de error pero NO falla el pago.
     *
     * @return null si se redimió exitosamente, o el mensaje de error si falló
     */
    private String attemptCouponRedemption(ClientOrder order, AppUser paidByUser) {
        try {
            // Buscar el cupón
            Coupon coupon = couponRepository.findById(order.getCouponId())
                    .orElse(null);

            if (coupon == null) {
                return "Cupón con ID " + order.getCouponId() + " no encontrado";
            }

            log.info("Intentando redimir cupón {} para orden {}", coupon.getCode(), order.getId());

            // Preparar request de redención
            // IMPORTANTE: NO pasar originalAmount aquí porque:
            // 1. El descuento YA fue calculado y aplicado por el Frontend
            // 2. Si pasamos originalAmount, el servicio recalcula el descuento (DOBLE DESCUENTO)
            // 3. Solo necesitamos marcar el cupón como "redimido" en auditoría
            // El email de redención solo mostrará que fue redimido, sin recalcular descuentos
            RedeemCouponRequest redemptionRequest = RedeemCouponRequest.builder()
                    .originalAmount(null)  // NULL: Solo marcar como redimido, sin recalcular
                    .redeemedBy(paidByUser.getEmail())
                    .channel(RedemptionChannel.COMANDIX)
                    .metadata("OrderId: " + order.getId())
                    .build();

            // Intentar redimir el cupón
            RedemptionResponse redemptionResponse = couponRedemptionService.redeemCouponByCode(
                    coupon.getCode(),
                    redemptionRequest,
                    order.getTenant().getId()
            );

            // Verificar si la redención fue exitosa
            if (redemptionResponse.isSuccess()) {
                log.info("Cupón {} redimido exitosamente para orden {}. Descuento: {}", 
                        coupon.getCode(), order.getId(), redemptionResponse.getDiscountAmount());
                return null; // Éxito
            } else {
                // Redención fallida pero no crítica
                String errorMsg = redemptionResponse.getMessage();
                log.warn("Fallo de redención para cupón {}: {}", coupon.getCode(), errorMsg);
                return errorMsg;
            }

        } catch (IllegalArgumentException ex) {
            // Cupón inválido, ya redimido, etc.
            String errorMsg = ex.getMessage();
            log.warn("Error de validación al redimir cupón: {}", errorMsg);
            return errorMsg;

        } catch (Exception ex) {
            // Error inesperado - no fallar el pago
            String errorMsg = "Error inesperado: " + ex.getMessage();
            log.error("Error inesperado al redimir cupón para orden {}: {}", order.getId(), errorMsg, ex);
            return errorMsg;
        }
    }

    @Override
    public SplitOrderResponse splitOrder(UUID orderId, SplitOrderRequest request) {
        log.info("Dividiendo orden {} en tenant {}", orderId, request.getTenantId());

        // ===== FASE 1: VALIDAR ORDEN =====
        ClientOrder order = clientOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));

        if (request.getTenantId() == null) {
            throw new IllegalArgumentException("tenantId es requerido");
        }
        if (!order.getTenant().getId().equals(request.getTenantId())) {
            throw new IllegalArgumentException("La orden no pertenece al tenant especificado");
        }

        if (order.getEstado() == OrderStatus.PAGADA || order.getEstado() == OrderStatus.CANCELADA) {
            throw new IllegalArgumentException(
                    "No se puede dividir una comanda en estado " + order.getEstado());
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar al menos un artículo para la nueva cuenta");
        }

        // ===== FASE 2: VALIDAR QUE NO SE MUEVAN MÁS UNIDADES DE LAS EXISTENTES =====
        Map<Long, Long> disponible = new HashMap<>();
        for (ClientOrderItem item : order.getItems()) {
            if (item.getCantidad() != null && item.getCantidad() > 0) {
                disponible.merge(item.getProduct().getId(), item.getCantidad().longValue(), Long::sum);
            }
        }

        for (CreateClientOrderRequest.OrderItemRequest req : request.getItems()) {
            if (req.getProductId() == null) {
                throw new IllegalArgumentException("productId es requerido en los artículos a mover");
            }
            long qty = req.getCantidad() != null ? req.getCantidad().longValue() : 0L;
            if (qty <= 0) {
                throw new IllegalArgumentException("Cantidad inválida para el producto " + req.getProductId());
            }
            long restante = disponible.getOrDefault(req.getProductId(), 0L);
            if (qty > restante) {
                throw new IllegalArgumentException(
                        "No se pueden mover más unidades de las existentes en la comanda para el producto "
                                + req.getProductId() + " (disponible: " + restante + ")");
            }
            disponible.put(req.getProductId(), restante - qty);
        }

        // ===== FASE 3: CREAR LA COMANDA NUEVA (conservando mesa, cliente, mesero y hora original) =====
        TenantCustomer targetCustomer = order.getCustomer();
        if (targetCustomer == null && request.getCustomerId() != null) {
            targetCustomer = tenantCustomerRepository.findById(request.getCustomerId()).orElse(null);
        }

        Mesa targetMesa = order.getMesa();
        if (targetMesa == null && request.getMesaId() != null) {
            targetMesa = mesaRepository.findById(request.getMesaId()).orElse(null);
        }

        AppUser targetMesero = order.getMesero();
        if (targetMesero == null && request.getMeseroId() != null) {
            targetMesero = appUserRepository.findById(request.getMeseroId()).orElse(null);
        }

        LocalDateTime mismaFecha = order.getFecha() != null ? order.getFecha() : LocalDateTime.now();
        LocalDateTime mismaHoraApertura = order.getHoraApertura() != null ? order.getHoraApertura() : mismaFecha;

        ClientOrder splitCopy = ClientOrder.builder()
                .customer(targetCustomer)
                .tenant(order.getTenant())
                .estado(order.getEstado())  // Misma etapa: hereda "lista para pagar" si la original era LISTO
                .mesa(targetMesa)           // Misma mesa asignada que la orden completa
                .mesero(targetMesero)       // Mismo mesero que la orden completa
                .clienteMesa(order.getClienteMesa() != null ? order.getClienteMesa() : targetCustomer) // Mismo cliente
                .horaApertura(mismaHoraApertura) // Misma hora de apertura
                .fecha(mismaFecha)          // Misma fecha y hora de registro que la orden original
                .turno(order.getTurno())    // Mismo turno
                .acceptedAt(order.getAcceptedAt())
                .readyAt(order.getReadyAt())
                .subtotal(BigDecimal.ZERO)
                .descuento(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .propina(BigDecimal.ZERO)
                .propinasLiquidadas(false)
                .items(new ArrayList<>())
                .source(request.getSource() != null && !request.getSource().isBlank()
                        ? request.getSource()
                        : (order.getSource() != null ? order.getSource() : "POS"))
                .build();
        splitCopy = clientOrderRepository.save(splitCopy);

        final ClientOrder finalSplit = splitCopy;
        List<ClientOrderItem> newItems = request.getItems().stream()
                .map(req -> {
                    TenantMenuProduct product = tenantMenuProductRepository.findById(req.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Producto no encontrado con ID: " + req.getProductId()));
                    return ClientOrderItemMapper.toEntity(req, finalSplit, product);
                })
                .collect(Collectors.toList());

        newItems = clientOrderItemRepository.saveAll(newItems);
        splitCopy.getItems().addAll(newItems);

        BigDecimal splitSubtotal = ClientOrderMapper.calculateSubtotal(newItems);
        splitCopy.setSubtotal(splitSubtotal);
        splitCopy.setDescuento(BigDecimal.ZERO);
        splitCopy.setTotal(ClientOrderMapper.calculateTotal(splitSubtotal, BigDecimal.ZERO));
        clientOrderRepository.save(splitCopy);

        // ===== FASE 4: QUITAR LOS ARTÍCULOS MOVIDOS DE LA COMANDa ORIGINAL =====
        // NOTA: no se restaura stock porque esos artículos pasan a la nueva comanda (mismo consumo).
        Map<Long, Long> aMover = new HashMap<>();
        for (CreateClientOrderRequest.OrderItemRequest req : request.getItems()) {
            aMover.merge(req.getProductId(), req.getCantidad().longValue(), Long::sum);
        }

        if (order.getItems() != null) {
            Iterator<ClientOrderItem> it = order.getItems().iterator();
            while (it.hasNext()) {
                ClientOrderItem item = it.next();
                Long mover = aMover.get(item.getProduct().getId());
                if (mover == null || mover <= 0) {
                    continue;
                }
                long actual = item.getCantidad() != null ? item.getCantidad().longValue() : 0L;
                if (actual <= mover) {
                    aMover.put(item.getProduct().getId(), mover - actual);
                    it.remove();  // orphanRemoval elimina el registro
                } else {
                    item.setCantidad((int) (actual - mover));
                    aMover.put(item.getProduct().getId(), 0L);
                }
            }
        }

        BigDecimal origSubtotal = ClientOrderMapper.calculateSubtotal(order.getItems());
        BigDecimal origDescuento = order.getDescuento() != null ? order.getDescuento() : BigDecimal.ZERO;
        if (origSubtotal.compareTo(origDescuento) < 0) {
            origDescuento = origSubtotal;
        }
        order.setSubtotal(origSubtotal);
        order.setDescuento(origDescuento);
        order.setTotal(ClientOrderMapper.calculateTotal(origSubtotal, origDescuento));
        clientOrderRepository.save(order);

        log.info("Orden {} dividida. Nueva comanda {} con {} artículos por {}", orderId, splitCopy.getId(),
                newItems.size(), BigDecimal.valueOf(aMover.values().stream().mapToLong(Long::longValue).sum()));

        ClientOrderDTO originalDto = ClientOrderMapper.toDTO(order, null, origDescuento);
        ClientOrderDTO newDto = ClientOrderMapper.toDTO(splitCopy, null, splitSubtotal);

        try {
            orderSseService.publishOrderStatusChanged(originalDto);
            orderSseService.publishNewChatbotOrder(newDto);
        } catch (Exception e) {
            log.warn("No se pudo notificar evento SSE para comanda dividida: {}", e.getMessage());
        }

        return new SplitOrderResponse(originalDto, newDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesReportRowDTO> getSalesReport(Long tenantId, LocalDateTime from, LocalDateTime to) {
        log.debug("Obteniendo reporte de ventas/comandas del tenant {} entre {} y {}", tenantId, from, to);
        return clientOrderRepository.findSalesReport(tenantId, from, to).stream()
                .map(row -> SalesReportRowDTO.builder()
                        .folio(row[0] != null ? row[0].toString() : null)
                        .horarioApertura(toLocalDateTime(row[1]))
                        .horarioCierre(toLocalDateTime(row[2]))
                        .mesa(row[3] != null ? row[3].toString() : null)
                        .mesero(row[4] != null ? row[4].toString() : null)
                        .totalPagado(toBigDecimal(row[5]))
                        .cliente(row[6] != null ? row[6].toString() : null)  // null = "Cliente no registrado"
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Convierte un valor de timestamp (java.sql.Timestamp o LocalDateTime)
     * devuelto por una consulta nativa a LocalDateTime.
     */
    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime();
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        return null;
    }

    /**
     * Convierte un valor numérico devuelto por una consulta nativa a BigDecimal.
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return BigDecimal.ZERO;
    }
}