package com.lealtixservice.service.impl;

import com.lealtixservice.dto.dashboard.VipClientDTO;
import com.lealtixservice.repository.WaiterDashboardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaiterDashboardServiceImplTest {

    @Mock
    private WaiterDashboardRepository waiterDashboardRepository;

    @InjectMocks
    private WaiterDashboardServiceImpl service;

    @Test
    void getVipClients_mapsNativeQueryTypesSafely() {
        LocalDateTime lastVisit = LocalDateTime.of(2026, 7, 10, 14, 30);
        Object[] row = new Object[] {
                15,
                "Cliente VIP",
                "vip@test.com",
                "5551234567",
                new BigDecimal("1520.50"),
                Timestamp.valueOf(lastVisit),
                4,
                380.125
        };
        List<Object[]> rawData = Collections.singletonList(row);

        when(waiterDashboardRepository.getVipClients(41L, 5)).thenReturn(rawData);

        List<VipClientDTO> result = service.getVipClients(41L, "20", 5);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(15L, result.get(0).getId());
        assertEquals("Cliente VIP", result.get(0).getName());
        assertEquals("vip@test.com", result.get(0).getEmail());
        assertEquals("5551234567", result.get(0).getPhone());
        assertEquals(new BigDecimal("1520.50"), result.get(0).getLtv());
        assertEquals(lastVisit, result.get(0).getLastVisitDate());
        assertEquals(4L, result.get(0).getVisitCount());
        assertEquals(new BigDecimal("380.125"), result.get(0).getAverageTicket());

        verify(waiterDashboardRepository).getVipClients(41L, 5);
    }
}
