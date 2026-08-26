package com.lealtixservice.service.impl;

import com.lealtixservice.dto.dashboard.*;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.repository.ClientOrderRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KitchenDashboardServiceImplTest {

    @Mock
    private ClientOrderRepository clientOrderRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private DashboardServiceImpl dashboardService;

    @InjectMocks
    private KitchenDashboardServiceImpl kitchenDashboardService;

    private Long tenantId;
    private LocalDateTime from;
    private LocalDateTime to;
    private Tenant mockTenant;

    @BeforeEach
    void setUp() {
        tenantId = 24L;
        from = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        to = LocalDateTime.of(2026, 1, 31, 23, 59, 59);

        mockTenant = Tenant.builder()
                .id(tenantId)
                .nombreNegocio("Restaurant Test")
                .build();
    }

    @Test
    void testGetSummary_WithValidDates() {
        // Arrange
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(mockTenant));
        when(clientOrderRepository.getTopDishes(tenantId, from, to)).thenReturn(getTopDishesData());
        when(dashboardService.getRepeatPurchaseRate(tenantId, from, to)).thenReturn(getRepeatPurchaseRate());
        when(clientOrderRepository.countCompletedOrders(tenantId, from, to)).thenReturn(45L);
        when(clientOrderRepository.countSuccessfulDeliveries(tenantId, from, to)).thenReturn(42L);
        when(dashboardService.getCustomizationAnalysis(tenantId, from, to)).thenReturn(getCustomizationAnalysis());
        when(clientOrderRepository.getVIPCustomer(tenantId, from, to)).thenReturn(getVIPCustomerData());

        // Act
        KitchenDashboardSummaryDTO summary = kitchenDashboardService.getSummary(tenantId, from, to);

        // Assert
        assertNotNull(summary);
        assertEquals("Restaurant Test", summary.getTenantName());
        assertEquals(3, summary.getTopDishes().size());
        assertEquals(1, summary.getTopDishes().get(0).getRank());
        assertEquals(45L, summary.getCompletedOrders().getCompletedOrders());
        assertEquals(42L, summary.getCompletedOrders().getSuccessfulDeliveries());
        assertNotNull(summary.getRepeatPurchaseRate());
        assertNotNull(summary.getVipAlert());
    }

    @Test
    void testGetSummary_WithNullDates_ShouldDefaultToTodayAndNow() {
        // Arrange
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(mockTenant));
        when(clientOrderRepository.getTopDishes(any(), any(), any())).thenReturn(new ArrayList<>());
        when(dashboardService.getRepeatPurchaseRate(any(), any(), any())).thenReturn(new RepeatPurchaseRateDTO());
        when(clientOrderRepository.countCompletedOrders(any(), any(), any())).thenReturn(0L);
        when(clientOrderRepository.countSuccessfulDeliveries(any(), any(), any())).thenReturn(0L);
        when(dashboardService.getCustomizationAnalysis(any(), any(), any())).thenReturn(new ArrayList<>());
        when(clientOrderRepository.getVIPCustomer(any(), any(), any())).thenReturn(null);

        // Act
        LocalDateTime now = LocalDateTime.now();
        KitchenDashboardSummaryDTO summary = kitchenDashboardService.getSummary(tenantId, null, null);

        // Assert
        assertNotNull(summary);
        assertEquals("Restaurant Test", summary.getTenantName());
    }

    @Test
    void testGetSummary_TopDishesMapping() {
        // Arrange
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(mockTenant));
        when(clientOrderRepository.getTopDishes(tenantId, from, to)).thenReturn(getTopDishesData());
        when(dashboardService.getRepeatPurchaseRate(tenantId, from, to)).thenReturn(new RepeatPurchaseRateDTO());
        when(clientOrderRepository.countCompletedOrders(tenantId, from, to)).thenReturn(0L);
        when(clientOrderRepository.countSuccessfulDeliveries(tenantId, from, to)).thenReturn(0L);
        when(dashboardService.getCustomizationAnalysis(tenantId, from, to)).thenReturn(new ArrayList<>());
        when(clientOrderRepository.getVIPCustomer(tenantId, from, to)).thenReturn(null);

        // Act
        KitchenDashboardSummaryDTO summary = kitchenDashboardService.getSummary(tenantId, from, to);

        // Assert
        List<TopDishDTO> topDishes = summary.getTopDishes();
        assertEquals(3, topDishes.size());
        
        assertEquals(1L, topDishes.get(0).getProductId());
        assertEquals("Pizza Margherita", topDishes.get(0).getProductName());
        assertEquals(150L, topDishes.get(0).getQuantity());
        assertEquals(1, topDishes.get(0).getRank());
        
        assertEquals(2L, topDishes.get(1).getProductId());
        assertEquals("Pasta Carbonara", topDishes.get(1).getProductName());
        assertEquals(120L, topDishes.get(1).getQuantity());
        assertEquals(2, topDishes.get(1).getRank());
    }

    @Test
    void testGetSummary_VIPCustomerMapping() {
        // Arrange
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(mockTenant));
        when(clientOrderRepository.getTopDishes(tenantId, from, to)).thenReturn(new ArrayList<>());
        when(dashboardService.getRepeatPurchaseRate(tenantId, from, to)).thenReturn(new RepeatPurchaseRateDTO());
        when(clientOrderRepository.countCompletedOrders(tenantId, from, to)).thenReturn(0L);
        when(clientOrderRepository.countSuccessfulDeliveries(tenantId, from, to)).thenReturn(0L);
        when(dashboardService.getCustomizationAnalysis(tenantId, from, to)).thenReturn(new ArrayList<>());
        when(clientOrderRepository.getVIPCustomer(tenantId, from, to)).thenReturn(getVIPCustomerData());

        // Act
        KitchenDashboardSummaryDTO summary = kitchenDashboardService.getSummary(tenantId, from, to);

        // Assert
        VIPAlertDTO vipAlert = summary.getVipAlert();
        assertNotNull(vipAlert);
        assertTrue(vipAlert.getActive());
        assertEquals(100L, vipAlert.getCustomerId());
        assertEquals("Juan Pérez", vipAlert.getCustomerName());
        assertEquals("juan@example.com", vipAlert.getCustomerEmail());
        assertEquals(new BigDecimal("5250.00"), vipAlert.getLifetimeValue());
    }

    @Test
    void testGetSummary_NoVIPCustomer() {
        // Arrange
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(mockTenant));
        when(clientOrderRepository.getTopDishes(tenantId, from, to)).thenReturn(new ArrayList<>());
        when(dashboardService.getRepeatPurchaseRate(tenantId, from, to)).thenReturn(new RepeatPurchaseRateDTO());
        when(clientOrderRepository.countCompletedOrders(tenantId, from, to)).thenReturn(0L);
        when(clientOrderRepository.countSuccessfulDeliveries(tenantId, from, to)).thenReturn(0L);
        when(dashboardService.getCustomizationAnalysis(tenantId, from, to)).thenReturn(new ArrayList<>());
        when(clientOrderRepository.getVIPCustomer(tenantId, from, to)).thenReturn(null);

        // Act
        KitchenDashboardSummaryDTO summary = kitchenDashboardService.getSummary(tenantId, from, to);

        // Assert
        assertNull(summary.getVipAlert());
    }

    @Test
    void testGetSummary_TenantNotFound() {
        // Arrange
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());
        when(clientOrderRepository.getTopDishes(tenantId, from, to)).thenReturn(new ArrayList<>());
        when(dashboardService.getRepeatPurchaseRate(tenantId, from, to)).thenReturn(new RepeatPurchaseRateDTO());
        when(clientOrderRepository.countCompletedOrders(tenantId, from, to)).thenReturn(0L);
        when(clientOrderRepository.countSuccessfulDeliveries(tenantId, from, to)).thenReturn(0L);
        when(dashboardService.getCustomizationAnalysis(tenantId, from, to)).thenReturn(new ArrayList<>());
        when(clientOrderRepository.getVIPCustomer(tenantId, from, to)).thenReturn(null);

        // Act
        KitchenDashboardSummaryDTO summary = kitchenDashboardService.getSummary(tenantId, from, to);

        // Assert
        assertNotNull(summary);
        assertEquals("Unknown Tenant", summary.getTenantName());
    }

    // Helper methods to create mock data
    private List<Object[]> getTopDishesData() {
        List<Object[]> data = new ArrayList<>();
        data.add(new Object[]{1L, "Pizza Margherita", 150L, new BigDecimal("1500.00")});
        data.add(new Object[]{2L, "Pasta Carbonara", 120L, new BigDecimal("1200.00")});
        data.add(new Object[]{3L, "Burger Deluxe", 100L, new BigDecimal("1000.00")});
        return data;
    }

    private RepeatPurchaseRateDTO getRepeatPurchaseRate() {
        return RepeatPurchaseRateDTO.builder()
                .totalCustomers(200L)
                .repeatCustomers(75L)
                .repeatRate(new BigDecimal("37.50"))
                .oneTimeBuyers(125L)
                .multiTimeBuyers(75L)
                .build();
    }

    private List<CustomizationAnalysisDTO> getCustomizationAnalysis() {
        List<CustomizationAnalysisDTO> data = new ArrayList<>();
        data.add(CustomizationAnalysisDTO.builder()
                .keyword("sin cebolla")
                .frequency(45L)
                .percentage(22.5)
                .build());
        data.add(CustomizationAnalysisDTO.builder()
                .keyword("bien cocido")
                .frequency(38L)
                .percentage(19.0)
                .build());
        return data;
    }

    private Object[] getVIPCustomerData() {
        return new Object[]{100L, "Juan Pérez", "juan@example.com", new BigDecimal("5250.00")};
    }
}
