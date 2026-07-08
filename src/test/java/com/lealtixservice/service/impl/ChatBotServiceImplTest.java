package com.lealtixservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lealtixservice.dto.QuickCustomerRegistrationDTO;
import com.lealtixservice.dto.TenantCustomerDTO;
import com.lealtixservice.entity.*;
import com.lealtixservice.repository.*;
import com.lealtixservice.service.ClientOrderService;
import com.lealtixservice.service.CouponRedemptionService;
import com.lealtixservice.service.CouponValidationService;
import com.lealtixservice.service.ProductCrossSellingService;
import com.lealtixservice.service.TenantCustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatBotServiceImplTest {

    @Mock private ChatBotSessionRepository sessionRepository;
    @Mock private ChatBotMessageRepository messageRepository;
    @Mock private TenantCustomerRepository customerRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private ClientOrderRepository orderRepository;
    @Mock private ClientOrderItemRepository orderItemRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private TenantMenuProductRepository productRepository;
    @Mock private CouponValidationService couponValidationService;
    @Mock private CouponRedemptionService couponRedemptionService;
    @Mock private ProductCrossSellingService crossSellingService;
    @Mock private ClientOrderService clientOrderService;
    @Mock private TenantCustomerService tenantCustomerService;

    @InjectMocks
    private ChatBotServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ChatBotServiceImpl(
                sessionRepository,
                messageRepository,
                customerRepository,
                tenantRepository,
                orderRepository,
                orderItemRepository,
                couponRepository,
                productRepository,
                couponValidationService,
                couponRedemptionService,
                crossSellingService,
                clientOrderService,
                tenantCustomerService,
                new ObjectMapper()
        );
    }

    @Test
    void registerCustomer_usesDemoTenantWhenTenantIdIsZero() {
        Tenant demoTenant = Tenant.builder()
                .id(15L)
                .nombreNegocio("Tenant Demo")
                .slug("demo")
                .UIDTenant("UID-DEMO")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(tenantRepository.getBySlug("demo")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenReturn(demoTenant);

        ArgumentCaptor<TenantCustomer> customerCaptor = ArgumentCaptor.forClass(TenantCustomer.class);
        when(tenantCustomerService.save(any(TenantCustomer.class))).thenAnswer(invocation -> {
            TenantCustomer customer = invocation.getArgument(0);
            customer.setId(99L);
            return customer;
        });

        QuickCustomerRegistrationDTO request = QuickCustomerRegistrationDTO.builder()
                .tenantId(0L)
                .name("Cliente Demo")
                .email("demo@correo.com")
                .phone("555123123")
                .acceptedPromotions(true)
                .build();

        TenantCustomerDTO result = service.registerCustomer(request);

        verify(tenantRepository).getBySlug("demo");
        verify(tenantRepository).save(any(Tenant.class));
        verify(tenantCustomerService).save(customerCaptor.capture());

        TenantCustomer savedCustomer = customerCaptor.getValue();
        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertNotNull(savedCustomer.getTenant());
        assertEquals(15L, savedCustomer.getTenant().getId());
        assertEquals("Cliente Demo", savedCustomer.getName());
        assertEquals("demo@correo.com", savedCustomer.getEmail());
    }
}
