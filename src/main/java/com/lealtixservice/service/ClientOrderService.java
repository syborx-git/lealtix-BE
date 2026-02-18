package com.lealtixservice.service;

import com.lealtixservice.dto.ClientOrderDTO;
import com.lealtixservice.dto.CreateClientOrderRequest;
import com.lealtixservice.entity.ClientOrder;
import com.lealtixservice.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface de servicio para gestionar órdenes de clientes
 */
public interface ClientOrderService {

    /**
     * Crea una nueva orden con sus items
     */
    ClientOrderDTO createOrder(CreateClientOrderRequest request);

    /**
     * Obtiene una orden por su ID
     */
    Optional<ClientOrderDTO> getOrderById(UUID orderId);

    /**
     * Obtiene todas las órdenes de un cliente paginadas
     */
    Page<ClientOrderDTO> getOrdersByCustomer(Long customerId, Pageable pageable);

    /**
     * Obtiene todas las órdenes de un tenant paginadas
     */
    Page<ClientOrderDTO> getOrdersByTenant(Long tenantId, Pageable pageable);

    /**
     * Obtiene órdenes de un tenant filtradas por estado
     */
    Page<ClientOrderDTO> getOrdersByTenantAndStatus(Long tenantId, OrderStatus estado, Pageable pageable);

    /**
     * Obtiene órdenes de un rango de fechas
     */
    List<ClientOrderDTO> getOrdersByDateRange(Long tenantId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Actualiza el estado de una orden
     */
    ClientOrderDTO updateOrderStatus(UUID orderId, OrderStatus newStatus);

    /**
     * Cancela una orden (cambia estado a CANCELADA)
     */
    ClientOrderDTO cancelOrder(UUID orderId);

    /**
     * Elimina una orden (solo si está en estado CANCELADA)
     */
    void deleteOrder(UUID orderId);

    /**
     * Obtiene el total de ventas de un tenant en un rango de fechas
     */
    Double getTotalSalesByTenant(Long tenantId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Obtiene el promedio de ticket de un tenant
     */
    Double getAverageTicketByTenant(Long tenantId);

    /**
     * Cuenta órdenes por estado en un tenant
     */
    Long countOrdersByStatus(Long tenantId, OrderStatus estado);
}
