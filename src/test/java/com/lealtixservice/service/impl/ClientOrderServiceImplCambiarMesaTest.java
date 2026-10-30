package com.lealtixservice.service.impl;

import com.lealtixservice.dto.ClientOrderDTO;
import com.lealtixservice.entity.AppUser;
import com.lealtixservice.entity.ClientOrder;
import com.lealtixservice.entity.Mesa;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.enums.MesaEstado;
import com.lealtixservice.enums.OrderStatus;
import com.lealtixservice.repository.*;
import com.lealtixservice.service.CouponRedemptionService;
import com.lealtixservice.service.InventoryService;
import com.lealtixservice.service.OrderSseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ClientOrderServiceImplCambiarMesaTest {

    @Mock
    private ClientOrderRepository clientOrderRepository;

    @Mock
    private ClientOrderItemRepository clientOrderItemRepository;

    @Mock
    private TenantCustomerRepository tenantCustomerRepository;

    @Mock
    private TenantMenuProductRepository tenantMenuProductRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private TenantUserRepository tenantUserRepository;

    @Mock
    private CouponRedemptionService couponRedemptionService;

    @Mock
    private OrderSseService orderSseService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private InsumoRepository insumoRepository;

    @InjectMocks
    private ClientOrderServiceImpl clientOrderService;

    private Tenant tenant;
    private AppUser mesero;
    private Mesa mesaAntigua;
    private Mesa mesaNueva;
    private ClientOrder order;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(1L);

        mesero = new AppUser();
        mesero.setId(10L);
        mesero.setFullName("Juan Mesero");
        mesero.setEmail("juan@test.com");

        mesaAntigua = Mesa.builder()
                .id(101L)
                .tenantId(1L)
                .nombre("Mesa 1")
                .numero(1)
                .estado(MesaEstado.OCUPADA)
                .meseroUserId(10L)
                .build();

        mesaNueva = Mesa.builder()
                .id(102L)
                .tenantId(1L)
                .nombre("Mesa 2")
                .numero(2)
                .estado(MesaEstado.LIBRE)
                .build();

        orderId = UUID.randomUUID();
        order = ClientOrder.builder()
                .id(orderId)
                .tenant(tenant)
                .mesa(mesaAntigua)
                .mesero(mesero)
                .estado(OrderStatus.CONFIRMADA)
                .subtotal(BigDecimal.valueOf(250.00))
                .descuento(BigDecimal.ZERO)
                .total(BigDecimal.valueOf(250.00))
                .fecha(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(Collections.emptyList())
                .build();

        when(clientOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(clientOrderRepository.save(any(ClientOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mesaRepository.findById(101L)).thenReturn(Optional.of(mesaAntigua));
        when(mesaRepository.findById(102L)).thenReturn(Optional.of(mesaNueva));
        when(mesaRepository.save(any(Mesa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clientOrderRepository.findByTenantId(1L)).thenReturn(List.of(order));
        doNothing().when(orderSseService).publishOrderStatusChanged(any());
    }

    @Test
    void testCambiarMesa_Exito() {
        ClientOrderDTO result = clientOrderService.cambiarMesa(orderId, 102L);

        assertNotNull(result);
        assertEquals(102L, result.getMesaId());
        assertEquals("Mesa 2", result.getMesaNombre());
        assertEquals(10L, result.getMeseroId());
        assertEquals("Juan Mesero", result.getMeseroNombre());

        // Mesa antigua debe quedar LIBRE porque no tiene otras órdenes activas
        assertEquals(MesaEstado.LIBRE, mesaAntigua.getEstado());
        assertNull(mesaAntigua.getMeseroUserId());

        // Mesa nueva debe quedar OCUPADA y con el mesero asignado
        assertEquals(MesaEstado.OCUPADA, mesaNueva.getEstado());
        assertEquals(10L, mesaNueva.getMeseroUserId());

        // La orden debe apuntar a la mesa nueva
        assertEquals(mesaNueva, order.getMesa());
        assertEquals(mesero, order.getMesero());

        // Debe notificar vía SSE
        verify(orderSseService, times(1)).publishOrderStatusChanged(any(ClientOrderDTO.class));
    }

    @Test
    void testCambiarMesa_MesaDestinoOcupada_LanzaExcepcion() {
        mesaNueva.setEstado(MesaEstado.OCUPADA);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                clientOrderService.cambiarMesa(orderId, 102L));

        assertTrue(ex.getMessage().contains("debe estar LIBRE"));
        verify(clientOrderRepository, never()).save(any());
    }

    @Test
    void testCambiarMesa_OrdenPagada_LanzaExcepcion() {
        order.setEstado(OrderStatus.PAGADA);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                clientOrderService.cambiarMesa(orderId, 102L));

        assertTrue(ex.getMessage().contains("No se puede cambiar de mesa una orden en estado"));
        verify(clientOrderRepository, never()).save(any());
    }

    @Test
    void testCambiarMesa_MismaMesa_LanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                clientOrderService.cambiarMesa(orderId, 101L));

        assertTrue(ex.getMessage().contains("ya se encuentra asignada a la mesa"));
        verify(clientOrderRepository, never()).save(any());
    }
}
