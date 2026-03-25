package com.lealtixservice.service.impl;

import com.lealtixservice.dto.ClientOrderDTO;
import com.lealtixservice.dto.CreateClientOrderRequest;
import com.lealtixservice.dto.RedeemCouponRequest;
import com.lealtixservice.dto.RedemptionResponse;
import com.lealtixservice.entity.ClientOrder;
import com.lealtixservice.entity.ClientOrderItem;
import com.lealtixservice.entity.Coupon;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.enums.CouponStatus;
import com.lealtixservice.enums.OrderStatus;
import com.lealtixservice.enums.RedemptionChannel;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.mapper.ClientOrderItemMapper;
import com.lealtixservice.mapper.ClientOrderMapper;
import com.lealtixservice.repository.ClientOrderItemRepository;
import com.lealtixservice.repository.ClientOrderRepository;
import com.lealtixservice.repository.CouponRepository;
import com.lealtixservice.repository.TenantCustomerRepository;
import com.lealtixservice.repository.TenantMenuProductRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.service.ClientOrderService;
import com.lealtixservice.service.CouponRedemptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ClientOrderServiceImpl implements ClientOrderService {

    private final ClientOrderRepository clientOrderRepository;
    private final ClientOrderItemRepository clientOrderItemRepository;
    private final TenantCustomerRepository tenantCustomerRepository;
    private final TenantMenuProductRepository tenantMenuProductRepository;
    private final TenantRepository tenantRepository;
    private final CouponRepository couponRepository;
    private final CouponRedemptionService couponRedemptionService;

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

        // Crear la orden
        ClientOrder order = ClientOrderMapper.toEntity(request, customer, tenant);

        // Guardar la orden primero para obtener el ID
        order = clientOrderRepository.save(order);
        
        // Variable final para usar en el lambda
        final ClientOrder finalOrder = order;

        // Crear y guardar los items
        List<ClientOrderItem> items = request.getItems().stream()
                .map(itemRequest -> {
                    TenantMenuProduct product = tenantMenuProductRepository.findById(itemRequest.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + itemRequest.getProductId()));

                    return ClientOrderItemMapper.toEntity(itemRequest, finalOrder, product);
                })
                .collect(Collectors.toList());

        items = clientOrderItemRepository.saveAll(items);
        order.setItems(items);

        // Calcular montos
        BigDecimal subtotal = ClientOrderMapper.calculateSubtotal(items);
        BigDecimal descuento = request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO;
        BigDecimal total = ClientOrderMapper.calculateTotal(subtotal, descuento);

        // Actualizar la orden con los montos calculados
        order.setSubtotal(subtotal);
        order.setDescuento(descuento);
        order.setTotal(total);
        order = clientOrderRepository.save(order);

        // Redimir cupón y obtener información completa si está presente
        String couponCode = request.getCouponCode();
        BigDecimal couponDiscount = BigDecimal.ZERO;
        
        // Solo redimir coupon si hay un cliente asociado
        if (customer != null) {
            couponDiscount = redeemCouponIfPresent(request, customer, tenant, order, subtotal);
        }

        log.info("Orden creada exitosamente con ID: {}", order.getId());
        return ClientOrderMapper.toDTO(order, couponCode, couponDiscount);
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
        return clientOrderRepository.findByTenantId(tenantId, pageable)
                .map(ClientOrderMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientOrderDTO> getOrdersByTenantAndStatus(Long tenantId, OrderStatus estado, Pageable pageable) {
        log.debug("Obteniendo órdenes del tenant {} con estado: {}", tenantId, estado);
        return clientOrderRepository.findByTenantIdAndEstado(tenantId, estado, pageable)
                .map(ClientOrderMapper::toDTO);
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
        log.info("Actualizando estado de orden {} a: {}", orderId, newStatus);

        ClientOrder order = clientOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));

        // Validar transiciones de estado permitidas
        validateStatusTransition(order.getEstado(), newStatus);

        // Si se está confirmando/pagando la orden y hay un cupón asociado, redimirlo
        if (newStatus == OrderStatus.PAGADA && order.getCouponId() != null && order.getCustomer() != null) {
            redeemCouponOnOrderConfirmation(order);
        }

        order.setEstado(newStatus);
        order = clientOrderRepository.save(order);

        log.info("Estado de orden {} actualizado a: {}", orderId, newStatus);
        
        // Obtener el código del cupón para incluirlo en el DTO (si existe)
        String couponCode = null;
        BigDecimal couponDiscount = order.getDescuento() != null ? order.getDescuento() : BigDecimal.ZERO;
        
        if (order.getCouponId() != null) {
            Coupon coupon = couponRepository.findById(order.getCouponId()).orElse(null);
            if (coupon != null) {
                couponCode = coupon.getCode();
            }
        }
        
        return ClientOrderMapper.toDTO(order, couponCode, couponDiscount);
    }

    @Override
    public ClientOrderDTO cancelOrder(UUID orderId) {
        log.info("Cancelando orden: {}", orderId);
        return updateOrderStatus(orderId, OrderStatus.CANCELADA);
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
        // PENDIENTE puede ir a PAGADA o CANCELADA
        if (currentStatus == OrderStatus.PENDIENTE) {
            if (newStatus != OrderStatus.PAGADA && newStatus != OrderStatus.CANCELADA) {
                throw new IllegalArgumentException("No se puede cambiar de PENDIENTE a " + newStatus);
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
}