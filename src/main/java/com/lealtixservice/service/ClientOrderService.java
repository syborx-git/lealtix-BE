package com.lealtixservice.service;

import com.lealtixservice.dto.ClientOrderDTO;
import com.lealtixservice.dto.CreateClientOrderRequest;
import com.lealtixservice.dto.RecordPaymentRequest;
import com.lealtixservice.dto.SalesReportRowDTO;
import com.lealtixservice.dto.SplitOrderRequest;
import com.lealtixservice.dto.SplitOrderResponse;
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
     * Actualiza los items, montos y cliente de una orden existente.
     * Permitido en cualquier momento mientras la orden no esté PAGADA ni CANCELADA
     * (permite agregar platillos si llega otro comensal o piden más).
     */
    ClientOrderDTO updateOrder(UUID orderId, CreateClientOrderRequest request);

    /**
     * Agrega nuevos productos a una comanda abierta (Ronda adicional).
     * Los nuevos items pertenecen a la misma comanda (misma cuenta para el cobro)
     * pero se despachan como una nueva ronda a cocina y barra.
     *
     * @param orderId ID de la comanda existente
     * @param request Datos con los nuevos items a agregar
     * @return DTO de la orden actualizada con todos los items acumulados
     */
    ClientOrderDTO addItemsToOrder(UUID orderId, CreateClientOrderRequest request);

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
     * Obtiene órdenes de un tenant filtradas por múltiples estados
     */
    Page<ClientOrderDTO> getOrdersByTenantAndStatuses(Long tenantId, List<OrderStatus> estados, Pageable pageable);

    /**
     * Obtiene órdenes de un rango de fechas
     */
    List<ClientOrderDTO> getOrdersByDateRange(Long tenantId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Actualiza el estado de una orden
     */
    ClientOrderDTO updateOrderStatus(UUID orderId, OrderStatus newStatus);

    /**
     * Actualiza el estado de una orden y registra auditoría de cancelación
     */
    ClientOrderDTO updateOrderStatus(UUID orderId, OrderStatus newStatus, String userEmail, String reason);

    /**
     * Actualiza el estado de una orden permitiendo aislar el área (BARRA o COCINA)
     */
    ClientOrderDTO updateOrderStatus(UUID orderId, OrderStatus newStatus, String userEmail, String reason, String area);

    /**
     * Actualiza el estado de una orden permitiendo aislar el área y registrando categoría y comentarios de cancelación
     */
    ClientOrderDTO updateOrderStatus(UUID orderId, OrderStatus newStatus, String userEmail, String reason, String area, String category, String comment);

    /**
     * Marcha los segundos tiempos de una comanda, actualizando sus ítems y pasando la orden a CONFIRMADA para cocina.
     */
    ClientOrderDTO marcharSegundoTiempo(UUID orderId);

    /**
     * Marcha los terceros tiempos de una comanda, actualizando sus ítems y pasando la orden a CONFIRMADA para cocina.
     */
    ClientOrderDTO marcharTercerTiempo(UUID orderId);

    /**
     * Marcha todos los tiempos pendientes de una comanda (segundo y tercer tiempo), actualizando sus ítems y pasando la orden a CONFIRMADA para cocina.
     */
    ClientOrderDTO marcharTodosLosTiempos(UUID orderId);

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

    /**
     * Registra el pago de una orden (transición LISTO → PAGADA)
     */
    ClientOrderDTO recordPayment(UUID orderId, RecordPaymentRequest request);

    /**
     * Divide una cuenta: mueve los artículos seleccionados de la comanda
     * a una comanda nueva lista para pagar.
     */
    SplitOrderResponse splitOrder(UUID orderId, SplitOrderRequest request);

    /**
     * Reporte general de ventas/comandas con JOIN de mesa, mesero y cliente.
     */
    List<SalesReportRowDTO> getSalesReport(Long tenantId, LocalDateTime from, LocalDateTime to);
}
