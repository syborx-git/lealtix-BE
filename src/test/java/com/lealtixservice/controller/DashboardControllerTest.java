package com.lealtixservice.controller;

import com.lealtixservice.service.DashboardService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @Test
    void testGetTotalCustomers_FormatoSimple() throws Exception {
        // Arrange
        when(dashboardService.getTotalCustomers(eq(24L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(150L);

        // Act & Assert
        mockMvc.perform(get("/api/dashboard/customers/total")
                        .param("tenantId", "24")
                        .param("from", "2026-01-01T06:00:00")
                        .param("to", "2026-02-19T04:54:49"))
                .andExpect(status().isOk())
                .andExpect(content().string("150"));
    }

    @Test
    void testGetTotalCustomers_FormatoConMilisegundos() throws Exception {
        // Arrange
        when(dashboardService.getTotalCustomers(eq(24L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(150L);

        // Act & Assert
        mockMvc.perform(get("/api/dashboard/customers/total")
                        .param("tenantId", "24")
                        .param("from", "2026-01-01T06:00:00.000")
                        .param("to", "2026-02-19T04:54:49.568"))
                .andExpect(status().isOk())
                .andExpect(content().string("150"));
    }

    @Test
    void testGetTotalCustomers_FormatoConZonaHorariaUTC() throws Exception {
        // Arrange
        when(dashboardService.getTotalCustomers(eq(24L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(150L);

        // Act & Assert
        mockMvc.perform(get("/api/dashboard/customers/total")
                        .param("tenantId", "24")
                        .param("from", "2026-01-01T06:00:00.000Z")
                        .param("to", "2026-02-19T04:54:49.568Z"))
                .andExpect(status().isOk())
                .andExpect(content().string("150"));
    }

    @Test
    void testGetTotalCustomers_FormatoConZonaHorariaZ() throws Exception {
        // Arrange
        when(dashboardService.getTotalCustomers(eq(24L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(150L);

        // Act & Assert
        mockMvc.perform(get("/api/dashboard/customers/total")
                        .param("tenantId", "24")
                        .param("from", "2026-01-01T06:00:00Z")
                        .param("to", "2026-02-19T04:54:49Z"))
                .andExpect(status().isOk())
                .andExpect(content().string("150"));
    }

    @Test
    void testGetTotalCustomers_FormatoConOffset() throws Exception {
        // Arrange
        when(dashboardService.getTotalCustomers(eq(24L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(150L);

        // Act & Assert
        mockMvc.perform(get("/api/dashboard/customers/total")
                        .param("tenantId", "24")
                        .param("from", "2026-01-01T06:00:00-05:00")
                        .param("to", "2026-02-19T04:54:49+00:00"))
                .andExpect(status().isOk())
                .andExpect(content().string("150"));
    }

    @Test
    void testGetTotalCustomers_FormatoInvalido() throws Exception {
        // Act & Assert - Debería retornar 400 con mensaje de error claro
        mockMvc.perform(get("/api/dashboard/customers/total")
                        .param("tenantId", "24")
                        .param("from", "01/01/2026")  // Formato incorrecto
                        .param("to", "2026-02-19T04:54:49"))
                .andExpect(status().isOk())  // GlobalExceptionHandler retorna 200 con code 400
                .andExpect(content().json("{\"code\":400}"));
    }
}
