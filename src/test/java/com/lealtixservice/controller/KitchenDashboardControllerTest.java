package com.lealtixservice.controller;

import com.lealtixservice.dto.dashboard.KitchenDashboardSummaryDTO;
import com.lealtixservice.service.KitchenDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KitchenDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KitchenDashboardService kitchenDashboardService;

    private Long tenantId;
    private KitchenDashboardSummaryDTO mockSummary;

    @BeforeEach
    void setUp() {
        tenantId = 24L;
        mockSummary = KitchenDashboardSummaryDTO.builder()
                .tenantName("Restaurant Test")
                .topDishes(null)
                .repeatPurchaseRate(null)
                .completedOrders(null)
                .customizationAnalysis(null)
                .vipAlert(null)
                .build();
    }

    @Test
    void testGetKitchenSummary_Success() throws Exception {
        when(kitchenDashboardService.getSummary(eq(tenantId), any(), any()))
                .thenReturn(mockSummary);

        mockMvc.perform(get("/api/dashboard/kitchen/summary")
                        .param("tenantId", tenantId.toString())
                        .with(user("testuser").authorities(() -> "dashboard_kitchen")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
