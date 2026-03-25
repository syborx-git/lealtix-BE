package com.lealtixservice.service.impl;

import com.lealtixservice.dto.CrossSellingDTO;
import com.lealtixservice.dto.ProductCrossSellingRequest;
import com.lealtixservice.entity.ProductCrossSelling;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantMenuCategory;
import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.repository.ProductCrossSellingRepository;
import com.lealtixservice.repository.TenantMenuProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCrossSellingServiceImplTest {

    @Mock
    private ProductCrossSellingRepository crossSellingRepository;

    @Mock
    private TenantMenuProductRepository productRepository;

    @InjectMocks
    private ProductCrossSellingServiceImpl crossSellingService;

    private Tenant tenant;
    private TenantMenuCategory category;
    private TenantMenuProduct mainProduct;
    private TenantMenuProduct suggestedProduct1;
    private TenantMenuProduct suggestedProduct2;

    @BeforeEach
    void setUp() {
        // Setup tenant
        tenant = Tenant.builder()
            .id(1L)
            .nombreNegocio("Test Restaurant")
            .build();

        // Setup category
        category = TenantMenuCategory.builder()
            .id(1L)
            .tenant(tenant)
            .nombre("Bebidas")
            .build();

        // Setup main product
        mainProduct = TenantMenuProduct.builder()
            .id(1L)
            .category(category)
            .nombre("Hamburguesa")
            .precio(new BigDecimal("10.00"))
            .imgUrl("/images/burger.jpg")
            .isActive(true)
            .build();

        // Setup suggested products
        suggestedProduct1 = TenantMenuProduct.builder()
            .id(2L)
            .category(category)
            .nombre("Papas Fritas")
            .precio(new BigDecimal("3.50"))
            .imgUrl("/images/fries.jpg")
            .descripcion("Papas crujientes")
            .isActive(true)
            .build();

        suggestedProduct2 = TenantMenuProduct.builder()
            .id(3L)
            .category(category)
            .nombre("Coca Cola")
            .precio(new BigDecimal("2.00"))
            .imgUrl(null) // Test placeholder
            .descripcion("Bebida refrescante")
            .isActive(true)
            .build();
    }

    @Test
    void getSuggestionsByProduct_Success() {
        // Given
        Long productId = 1L;
        Long tenantId = 1L;

        ProductCrossSelling crossSelling1 = ProductCrossSelling.builder()
            .id(1L)
            .product(mainProduct)
            .suggestedProduct(suggestedProduct1)
            .tenant(tenant)
            .isActive(true)
            .displayOrder(1)
            .build();

        ProductCrossSelling crossSelling2 = ProductCrossSelling.builder()
            .id(2L)
            .product(mainProduct)
            .suggestedProduct(suggestedProduct2)
            .tenant(tenant)
            .isActive(true)
            .displayOrder(2)
            .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(mainProduct));
        when(crossSellingRepository.findActiveSuggestionsByProductAndTenant(productId, tenantId))
            .thenReturn(Arrays.asList(crossSelling1, crossSelling2));

        // When
        List<CrossSellingDTO> result = crossSellingService.getSuggestionsByProduct(productId, tenantId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        
        CrossSellingDTO dto1 = result.get(0);
        assertEquals(suggestedProduct1.getId(), dto1.getId());
        assertEquals("Papas Fritas", dto1.getName());
        assertEquals(new BigDecimal("3.50"), dto1.getPrice());
        assertEquals("/images/fries.jpg", dto1.getImageUrl());

        CrossSellingDTO dto2 = result.get(1);
        assertEquals(suggestedProduct2.getId(), dto2.getId());
        assertEquals("Coca Cola", dto2.getName());
        assertEquals("/assets/product-placeholder.png", dto2.getImageUrl()); // Placeholder

        verify(productRepository).findById(productId);
        verify(crossSellingRepository).findActiveSuggestionsByProductAndTenant(productId, tenantId);
    }

    @Test
    void getSuggestionsByProduct_ProductNotFound() {
        // Given
        Long productId = 999L;
        Long tenantId = 1L;

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            crossSellingService.getSuggestionsByProduct(productId, tenantId);
        });

        verify(productRepository).findById(productId);
        verify(crossSellingRepository, never()).findActiveSuggestionsByProductAndTenant(anyLong(), anyLong());
    }

    @Test
    void getSuggestionsByProduct_WrongTenant() {
        // Given
        Long productId = 1L;
        Long wrongTenantId = 999L;

        when(productRepository.findById(productId)).thenReturn(Optional.of(mainProduct));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            crossSellingService.getSuggestionsByProduct(productId, wrongTenantId);
        });

        verify(productRepository).findById(productId);
        verify(crossSellingRepository, never()).findActiveSuggestionsByProductAndTenant(anyLong(), anyLong());
    }

    @Test
    void getSuggestionsByProduct_NullParameters() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            crossSellingService.getSuggestionsByProduct(null, 1L);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            crossSellingService.getSuggestionsByProduct(1L, null);
        });

        verify(productRepository, never()).findById(anyLong());
        verify(crossSellingRepository, never()).findActiveSuggestionsByProductAndTenant(anyLong(), anyLong());
    }

    @Test
    void getSuggestionsByProduct_NoSuggestions() {
        // Given
        Long productId = 1L;
        Long tenantId = 1L;

        when(productRepository.findById(productId)).thenReturn(Optional.of(mainProduct));
        when(crossSellingRepository.findActiveSuggestionsByProductAndTenant(productId, tenantId))
            .thenReturn(List.of());

        // When
        List<CrossSellingDTO> result = crossSellingService.getSuggestionsByProduct(productId, tenantId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(productRepository).findById(productId);
        verify(crossSellingRepository).findActiveSuggestionsByProductAndTenant(productId, tenantId);
    }
    
    @Test
    void createCrossSelling_ExceedsMaxLimit() {
        // Given
        ProductCrossSellingRequest request = ProductCrossSellingRequest.builder()
            .productId(1L)
            .suggestedProductId(6L)
            .tenantId(1L)
            .displayOrder(6)
            .isActive(true)
            .build();
        
        // Simular que ya existen 5 sugerencias (el máximo)
        List<ProductCrossSelling> existingSuggestions = Arrays.asList(
            ProductCrossSelling.builder().id(1L).build(),
            ProductCrossSelling.builder().id(2L).build(),
            ProductCrossSelling.builder().id(3L).build(),
            ProductCrossSelling.builder().id(4L).build(),
            ProductCrossSelling.builder().id(5L).build()
        );
        
        when(crossSellingRepository.existsByProductIdAndSuggestedProductIdAndTenantId(
            anyLong(), anyLong(), anyLong())).thenReturn(false);
        when(crossSellingRepository.findByProductIdAndTenantId(1L, 1L))
            .thenReturn(existingSuggestions);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            crossSellingService.createCrossSelling(request);
        });
        
        assertTrue(exception.getMessage().contains("máximo de 3 sugerencias"));
        assertTrue(exception.getMessage().contains("elimina una sugerencia existente"));
        
        verify(crossSellingRepository).findByProductIdAndTenantId(1L, 1L);
        verify(productRepository, never()).findById(anyLong());
    }
}
