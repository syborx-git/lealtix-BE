package com.lealtixservice.controller;

import com.lealtixservice.dto.ClientOrderDTO;
import com.lealtixservice.dto.CreateClientOrderRequest;
import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.RecordPaymentRequest;
import com.lealtixservice.dto.UpdateOrderStatusRequest;
import com.lealtixservice.enums.OrderStatus;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.service.ClientOrderService;
import com.lealtixservice.util.RequireKitchenModule;
import com.lealtixservice.util.TenantOwnership;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Tag(name = "Client Orders", description = "Operaciones relacionadas con órdenes/comandas de clientes")
@RestController
@RequestMapping("/api/tenant-client-orders")
@RequiredArgsConstructor
public class ClientOrderController {

    private final ClientOrderService clientOrderService;

    @Operation(summary = "Crear una nueva orden")
    @PostMapping
    public ResponseEntity<GenericResponse> createOrder(@Valid @RequestBody CreateClientOrderRequest request) {
        try {
            log.info("Creando nueva orden para cliente {} en tenant {}", request.getCustomerId(), request.getTenantId());
            ClientOrderDTO order = clientOrderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new GenericResponse(201, "Orden creada exitosamente", order));
        } catch (ResourceNotFoundException ex) {
            log.warn("Recurso no encontrado al crear orden: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse(404, ex.getMessage(), null));
        } catch (IllegalArgumentException ex) {
            log.warn("Error de validación al crear orden: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error creando orden", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Obtener una orden por su ID")
    @GetMapping("/{orderId}")
    public ResponseEntity<GenericResponse> getOrderById(@PathVariable UUID orderId) {
        try {
            log.debug("Obteniendo orden con ID: {}", orderId);
            return clientOrderService.getOrderById(orderId)
                    .map(order -> ResponseEntity.ok(new GenericResponse(200, "Orden encontrada", order)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new GenericResponse(404, "Orden no encontrada", null)));
        } catch (Exception e) {
            log.error("Error obteniendo orden {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Obtener órdenes de un cliente (paginadas)")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<GenericResponse> getOrdersByCustomer(
            @PathVariable Long customerId,
            @Parameter(description = "Número de página (comenzando en 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página")
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.debug("Obteniendo órdenes del cliente: {} (página: {}, tamaño: {})", customerId, page, size);
            Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
            Page<ClientOrderDTO> orders = clientOrderService.getOrdersByCustomer(customerId, pageable);
            return ResponseEntity.ok(new GenericResponse(200, "Órdenes encontradas", orders));
        } catch (Exception e) {
            log.error("Error obteniendo órdenes del cliente {}", customerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Obtener órdenes de un tenant (paginadas)")
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<GenericResponse> getOrdersByTenant(
            @PathVariable Long tenantId,
            @Parameter(description = "Número de página (comenzando en 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página")
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.debug("Obteniendo órdenes del tenant: {} (página: {}, tamaño: {})", tenantId, page, size);
            Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
            Page<ClientOrderDTO> orders = clientOrderService.getOrdersByTenant(tenantId, pageable);
            return ResponseEntity.ok(new GenericResponse(200, "Órdenes encontradas", orders));
        } catch (Exception e) {
            log.error("Error obteniendo órdenes del tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Obtener órdenes de un tenant filtradas por estado")
    @GetMapping("/tenant/{tenantId}/status/{estado}")
    public ResponseEntity<GenericResponse> getOrdersByTenantAndStatus(
            @PathVariable Long tenantId,
            @PathVariable OrderStatus estado,
            @Parameter(description = "Número de página (comenzando en 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página")
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.debug("Obteniendo órdenes del tenant {} con estado {} (página: {}, tamaño: {})", tenantId, estado, page, size);
            Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
            Page<ClientOrderDTO> orders = clientOrderService.getOrdersByTenantAndStatus(tenantId, estado, pageable);
            return ResponseEntity.ok(new GenericResponse(200, "Órdenes encontradas", orders));
        } catch (Exception e) {
            log.error("Error obteniendo órdenes del tenant {} con estado {}", tenantId, estado, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Obtener órdenes de un tenant filtradas por estado (query params)")
    @GetMapping
    @TenantOwnership(tenantIdParam = "tenantId")
    public ResponseEntity<GenericResponse> getOrdersByTenantAndStatusQuery(
            @RequestParam Long tenantId,
            @RequestParam String status,
            @Parameter(description = "Número de página (comenzando en 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página")
            @RequestParam(defaultValue = "20") int size) {
        try {
            OrderStatus orderStatus = resolveOrderStatus(status);
            if (orderStatus == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new GenericResponse(400,
                                "Estado inválido: '" + status + "'. Valores aceptados: PENDING/PENDIENTE, CONFIRMED/CONFIRMADA, EN_PREPARACION/IN_PREPARATION, LISTO/READY, PAGADA/PAID, CANCELADA/CANCELLED", null));
            }
            log.debug("Obteniendo órdenes del tenant {} con estado {} (página: {}, tamaño: {})", tenantId, orderStatus, page, size);
            Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
            Page<ClientOrderDTO> orders = clientOrderService.getOrdersByTenantAndStatus(tenantId, orderStatus, pageable);
            return ResponseEntity.ok(new GenericResponse(200, "Órdenes encontradas", orders));
        } catch (Exception e) {
            log.error("Error obteniendo órdenes del tenant {} con estado {}", tenantId, status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    /**
     * Resuelve el estado de la orden aceptando tanto inglés como español.
     * PENDING / PENDIENTE → OrderStatus.PENDIENTE
     * CONFIRMED / CONFIRMADA / CONFIRMADO → OrderStatus.CONFIRMADA
     * IN_PREPARATION / EN_PREPARACION / PREPARING → OrderStatus.EN_PREPARACION
     * READY / LISTO / COMPLETED → OrderStatus.LISTO
     * PAID / PAGADA → OrderStatus.PAGADA
     * CANCELLED / CANCELED / CANCELADA / RECHAZADO → OrderStatus.CANCELADA
     */
    private OrderStatus resolveOrderStatus(String status) {
        if (status == null) return null;
        return switch (status.toUpperCase().trim()) {
            case "PENDING", "PENDIENTE"                      -> OrderStatus.PENDIENTE;
            case "CONFIRMED", "CONFIRMADA", "CONFIRMADO"               -> OrderStatus.CONFIRMADA;
            case "IN_PREPARATION", "EN_PREPARACION", "PREPARING"           -> OrderStatus.EN_PREPARACION;
            case "READY", "LISTO", "COMPLETED"                             -> OrderStatus.LISTO;
            case "PAID", "PAGADA"                                           -> OrderStatus.PAGADA;
            case "CANCELLED", "CANCELED", "CANCELADA", "RECHAZADO"        -> OrderStatus.CANCELADA;
            default -> null;
        };
    }

    @Operation(summary = "Actualizar el estado de una orden")
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<GenericResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        try {
            log.info("Actualizando estado de orden {} a: {}", orderId, request.getEstado());
            ClientOrderDTO order = clientOrderService.updateOrderStatus(
                    orderId, 
                    request.getEstado(),
                    request.getUserEmail(),
                    request.getReason()
            );
            return ResponseEntity.ok(new GenericResponse(200, "Estado de orden actualizado", order));
        } catch (ResourceNotFoundException ex) {
            log.warn("Orden no encontrada: {}", orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse(404, ex.getMessage(), null));
        } catch (IllegalArgumentException ex) {
            log.warn("Error de validación al actualizar orden {}: {}", orderId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error actualizando estado de orden {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Actualizar estado de una orden (endpoint dedicado para cocina)")
    @PatchMapping("/status")
    @RequireKitchenModule
    public ResponseEntity<GenericResponse> updateOrderStatusDedicated(
            @RequestParam UUID orderId,
            @RequestParam String status) {
        try {
            OrderStatus orderStatus = resolveOrderStatus(status);
            if (orderStatus == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new GenericResponse(400,
                                "Estado inválido: '" + status + "'. Valores aceptados: PENDING/PENDIENTE, CONFIRMED/CONFIRMADA, EN_PREPARACION/IN_PREPARATION, LISTO/READY, PAGADA/PAID, CANCELADA/CANCELLED", null));
            }
            log.info("Actualizando estado de orden {} a: {} (dedicado)", orderId, orderStatus);
            ClientOrderDTO order = clientOrderService.updateOrderStatus(orderId, orderStatus);
            return ResponseEntity.ok(new GenericResponse(200, "Estado de orden actualizado", order));
        } catch (ResourceNotFoundException ex) {
            log.warn("Orden no encontrada: {}", orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse(404, ex.getMessage(), null));
        } catch (IllegalArgumentException ex) {
            log.warn("Error de validación al actualizar orden {}: {}", orderId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error actualizando estado de orden {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Cancelar una orden")
    @DeleteMapping("/{orderId}/cancel")
    public ResponseEntity<GenericResponse> cancelOrder(@PathVariable UUID orderId) {
        try {
            log.info("Cancelando orden: {}", orderId);
            ClientOrderDTO order = clientOrderService.cancelOrder(orderId);
            return ResponseEntity.ok(new GenericResponse(200, "Orden cancelada exitosamente", order));
        } catch (ResourceNotFoundException ex) {
            log.warn("Orden no encontrada: {}", orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse(404, ex.getMessage(), null));
        } catch (IllegalArgumentException ex) {
            log.warn("Error de validación al cancelar orden {}: {}", orderId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error cancelando orden {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Obtener total de ventas de un tenant en rango de fechas")
    @GetMapping("/tenant/{tenantId}/sales")
    public ResponseEntity<GenericResponse> getTotalSalesByTenant(
            @PathVariable Long tenantId,
            @Parameter(description = "Fecha inicio (ISO 8601 format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam LocalDateTime startDate,
            @Parameter(description = "Fecha fin (ISO 8601 format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam LocalDateTime endDate) {
        try {
            log.debug("Obteniendo ventas totales del tenant {} entre {} y {}", tenantId, startDate, endDate);
            Double totalSales = clientOrderService.getTotalSalesByTenant(tenantId, startDate, endDate);
            return ResponseEntity.ok(new GenericResponse(200, "Ventas totales obtenidas", totalSales));
        } catch (Exception e) {
            log.error("Error obteniendo ventas totales del tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Obtener ticket promedio de un tenant")
    @GetMapping("/tenant/{tenantId}/average-ticket")
    public ResponseEntity<GenericResponse> getAverageTicketByTenant(@PathVariable Long tenantId) {
        try {
            log.debug("Obteniendo ticket promedio del tenant: {}", tenantId);
            Double averageTicket = clientOrderService.getAverageTicketByTenant(tenantId);
            return ResponseEntity.ok(new GenericResponse(200, "Ticket promedio obtenido", averageTicket));
        } catch (Exception e) {
            log.error("Error obteniendo ticket promedio del tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Contar órdenes por estado en un tenant")
    @GetMapping("/tenant/{tenantId}/count/{estado}")
    public ResponseEntity<GenericResponse> countOrdersByStatus(
            @PathVariable Long tenantId,
            @PathVariable OrderStatus estado) {
        try {
            log.debug("Contando órdenes del tenant {} con estado: {}", tenantId, estado);
            Long count = clientOrderService.countOrdersByStatus(tenantId, estado);
            return ResponseEntity.ok(new GenericResponse(200, "Cantidad obtenida", count));
        } catch (Exception e) {
            log.error("Error contando órdenes del tenant {} con estado {}", tenantId, estado, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Registrar pago de una orden")
    @PatchMapping("/{orderId}/record-payment")
    public ResponseEntity<GenericResponse> recordPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody RecordPaymentRequest request) {
        try {
            log.info("Registrando pago para orden {} con método {}", orderId, request.getMethod());
            ClientOrderDTO order = clientOrderService.recordPayment(orderId, request);
            return ResponseEntity.ok(new GenericResponse(200, "Pago registrado exitosamente", order));
        } catch (ResourceNotFoundException ex) {
            log.warn("Orden no encontrada: {}", orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse(404, ex.getMessage(), null));
        } catch (IllegalArgumentException ex) {
            log.warn("Error de validación al registrar pago en orden {}: {}", orderId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error registrando pago para orden {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }
}