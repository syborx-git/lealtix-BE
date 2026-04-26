package com.lealtixservice.service.impl;

import com.lealtixservice.dto.ClientOrderDTO;
import com.lealtixservice.dto.RecordPaymentRequest;
import com.lealtixservice.entity.AppUser;
import com.lealtixservice.entity.ClientOrder;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.entity.TenantUser;
import com.lealtixservice.enums.OrderStatus;
import com.lealtixservice.enums.PaymentMethod;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.mapper.ClientOrderMapper;
import com.lealtixservice.repository.AppUserRepository;
import com.lealtixservice.repository.ClientOrderItemRepository;
import com.lealtixservice.repository.ClientOrderRepository;
import com.lealtixservice.repository.CouponRepository;
import com.lealtixservice.repository.TenantCustomerRepository;
import com.lealtixservice.repository.TenantMenuProductRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.repository.TenantUserRepository;
import com.lealtixservice.service.CouponRedemptionService;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ClientOrderServiceImplRecordPaymentTest {

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

    @InjectMocks
    private ClientOrderServiceImpl clientOrderService;

    private UUID orderId;
    private ClientOrder testOrder;
    private AppUser testUser;
    private TenantUser testTenantUser;
    private Tenant testTenant;

    @BeforeEach
    public void setUp() {
        orderId = UUID.randomUUID();
        testTenant = new Tenant();
        testTenant.setId(1L);

        testOrder = new ClientOrder();
        testOrder.setId(orderId);
        testOrder.setEstado(OrderStatus.LISTO);
        testOrder.setTotal(new BigDecimal("100.00"));
        testOrder.setTenant(testTenant);

        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");

        testTenantUser = new TenantUser();
        testTenantUser.setId(1L);
        testTenantUser.setNombre("Test User");
        testTenantUser.setEmail("test@example.com");
        testTenantUser.setActivo(true);

        // Mock TenantUserRepository
        when(tenantUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testTenantUser));

        // Mock AppUserRepository
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(testUser);
    }

    @Test
    public void testRecordPaymentWithCashSuccess() {
        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setMethod(PaymentMethod.CASH);
        request.setReference(null);
        request.setUserEmail("test@example.com");

        when(clientOrderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(clientOrderRepository.save(any(ClientOrder.class))).thenReturn(testOrder);
        doNothing().when(orderSseService).publishOrderStatusChanged(any());

        ClientOrderDTO result = clientOrderService.recordPayment(orderId, request);

        assertNotNull(result);
        assertEquals(OrderStatus.PAGADA, result.getEstado());
        assertEquals(PaymentMethod.CASH, result.getPaidMethod());
        assertNull(result.getPaymentReference());

        verify(clientOrderRepository).save(any(ClientOrder.class));
    }

    @Test
    public void testRecordPaymentWithCardSuccess() {
        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setMethod(PaymentMethod.CARD);
        request.setReference("AUTH-12345");
        request.setUserEmail("test@example.com");

        when(clientOrderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(clientOrderRepository.save(any(ClientOrder.class))).thenReturn(testOrder);
        doNothing().when(orderSseService).publishOrderStatusChanged(any());

        ClientOrderDTO result = clientOrderService.recordPayment(orderId, request);

        assertNotNull(result);
        assertEquals(OrderStatus.PAGADA, result.getEstado());
        assertEquals(PaymentMethod.CARD, result.getPaidMethod());
        assertEquals("AUTH-12345", result.getPaymentReference());

        verify(clientOrderRepository).save(any(ClientOrder.class));
    }

    @Test
    public void testRecordPaymentWithCardMissingReference() {
        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setMethod(PaymentMethod.CARD);
        request.setReference("");  // INVALID
        request.setUserEmail("test@example.com");

        when(clientOrderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        assertThrows(IllegalArgumentException.class,
                () -> clientOrderService.recordPayment(orderId, request));

        verify(clientOrderRepository, never()).save(any());
    }

    @Test
    public void testRecordPaymentOrderNotInReadyState() {
        testOrder.setEstado(OrderStatus.EN_PREPARACION);

        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setMethod(PaymentMethod.CASH);
        request.setUserEmail("test@example.com");

        when(clientOrderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        assertThrows(IllegalArgumentException.class,
                () -> clientOrderService.recordPayment(orderId, request));

        verify(clientOrderRepository, never()).save(any());
    }

    @Test
    public void testRecordPaymentOrderNotFound() {
        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setMethod(PaymentMethod.CASH);
        request.setUserEmail("test@example.com");

        when(clientOrderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> clientOrderService.recordPayment(orderId, request));

        verify(clientOrderRepository, never()).save(any());
    }

    @Test
    public void testRecordPaymentAlreadyPaid() {
        testOrder.setPaidAt(LocalDateTime.now());

        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setMethod(PaymentMethod.CASH);
        request.setUserEmail("test@example.com");

        when(clientOrderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        assertThrows(IllegalArgumentException.class,
                () -> clientOrderService.recordPayment(orderId, request));

        verify(clientOrderRepository, never()).save(any());
    }

    @Test
    public void testRecordPaymentWithTransferSuccess() {
        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setMethod(PaymentMethod.TRANSFER);
        request.setReference("TRF-UUID-2026-04-25-12345");
        request.setUserEmail("test@example.com");

        when(clientOrderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(clientOrderRepository.save(any(ClientOrder.class))).thenReturn(testOrder);
        doNothing().when(orderSseService).publishOrderStatusChanged(any());

        ClientOrderDTO result = clientOrderService.recordPayment(orderId, request);

        assertNotNull(result);
        assertEquals(OrderStatus.PAGADA, result.getEstado());
        assertEquals(PaymentMethod.TRANSFER, result.getPaidMethod());
        assertEquals("TRF-UUID-2026-04-25-12345", result.getPaymentReference());

        verify(clientOrderRepository).save(any(ClientOrder.class));
    }

    @Test
    public void testRecordPaymentWithMixedPaymentSuccess() {
        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setMethod(PaymentMethod.MIXED);
        request.setReference("MIXED-50-CASH-50-TRANSFER-REF-ABC");
        request.setUserEmail("test@example.com");

        when(clientOrderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(clientOrderRepository.save(any(ClientOrder.class))).thenReturn(testOrder);
        doNothing().when(orderSseService).publishOrderStatusChanged(any());

        ClientOrderDTO result = clientOrderService.recordPayment(orderId, request);

        assertNotNull(result);
        assertEquals(OrderStatus.PAGADA, result.getEstado());
        assertEquals(PaymentMethod.MIXED, result.getPaidMethod());

        verify(clientOrderRepository).save(any(ClientOrder.class));
    }

    @Test
    public void testRecordPaymentMissingUserEmail() {
        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setMethod(PaymentMethod.CASH);
        request.setUserEmail(null);  // INVALID

        when(clientOrderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        assertThrows(IllegalArgumentException.class,
                () -> clientOrderService.recordPayment(orderId, request));

        verify(clientOrderRepository, never()).save(any());
    }

    @Test
    public void testRecordPaymentUserNotFound() {
        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setMethod(PaymentMethod.CASH);
        request.setUserEmail("nonexistent@example.com");

        when(clientOrderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));
        when(tenantUserRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> clientOrderService.recordPayment(orderId, request));

        verify(clientOrderRepository, never()).save(any());
    }
}
