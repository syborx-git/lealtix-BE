package com.lealtixservice.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TenantMenuProductDTOTest {

    @Test
    void projectionConstructor_initializesCrossSellingProducts() {
        TenantMenuProductDTO dto = new TenantMenuProductDTO(
                10L,
                20L,
                "Bebidas",
                "Frias",
                true,
                1,
                30L,
                "Limonada",
                "Natural",
                true,
                new BigDecimal("49.90"),
                "https://img.test/limonada.png"
        );

        assertEquals(10L, dto.getId());
        assertEquals(20L, dto.getCategoryId());
        assertEquals("Bebidas", dto.getCategoryName());
        assertEquals(30L, dto.getTenantId());
        assertEquals("Limonada", dto.getName());
        assertEquals(new BigDecimal("49.90"), dto.getPrice());
        assertNotNull(dto.getCrossSellingProducts());
        assertEquals(List.of(), dto.getCrossSellingProducts());
    }
}
