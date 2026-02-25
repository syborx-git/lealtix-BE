package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.ProductCrossSellingRequest;
import com.lealtixservice.dto.ProductCrossSellingResponse;
import com.lealtixservice.service.ProductCrossSellingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCrossSellingControllerTest {

    @Mock
    private ProductCrossSellingService crossSellingService;

    @InjectMocks
    private ProductCrossSellingController controller;

    private ProductCrossSellingRequest request;
    private ProductCrossSellingResponse response;

    @BeforeEach
    void setUp() {
        request = ProductCrossSellingRequest.builder()
            .productId(1L)
            .suggestedProductId(2L)
            .tenantId(100L)
            .displayOrder(1)
            .isActive(true)
            .build();

        response = ProductCrossSellingResponse.builder()
            .id(1L)
            .productId(1L)
            .productName("Hamburguesa")
            .suggestedProductId(2L)
            .suggestedProductName("Papas Fritas")
            .tenantId(100L)
            .displayOrder(1)
            .isActive(true)
            .build();
    }

    @Test
    void createCrossSelling_Success() {
        // Given
        when(crossSellingService.createCrossSelling(any())).thenReturn(response);

        // When
        ResponseEntity<GenericResponse> result = controller.createCrossSelling(request);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(201, result.getBody().getCode());
        assertNotNull(result.getBody().getObject());

        verify(crossSellingService).createCrossSelling(any());
    }

    @Test
    void createCrossSelling_ValidationError() {
        // Given
        when(crossSellingService.createCrossSelling(any()))
            .thenThrow(new IllegalArgumentException("Un producto no puede sugerirse a sí mismo"));

        // When
        ResponseEntity<GenericResponse> result = controller.createCrossSelling(request);

        // Then
        assertNotNull(result);
        assertNotNull(result.getBody());
        assertEquals(400, result.getBody().getCode());
        assertTrue(result.getBody().getMessage().contains("Error de validación"));
    }

    @Test
    void getAllByTenant_Success() {
        // Given
        List<ProductCrossSellingResponse> responses = Arrays.asList(response);
        when(crossSellingService.getAllByTenant(100L)).thenReturn(responses);

        // When
        ResponseEntity<GenericResponse> result = controller.getAllByTenant(100L);

        // Then
        assertNotNull(result);
        assertNotNull(result.getBody());
        assertEquals(200, result.getBody().getCode());
        assertNotNull(result.getBody().getObject());

        verify(crossSellingService).getAllByTenant(100L);
    }

    @Test
    void getByProduct_Success() {
        // Given
        List<ProductCrossSellingResponse> responses = Arrays.asList(response);
        when(crossSellingService.getByProduct(1L, 100L)).thenReturn(responses);

        // When
        ResponseEntity<GenericResponse> result = controller.getByProduct(1L, 100L);

        // Then
        assertNotNull(result);
        assertNotNull(result.getBody());
        assertEquals(200, result.getBody().getCode());
        assertNotNull(result.getBody().getObject());

        verify(crossSellingService).getByProduct(1L, 100L);
    }

    @Test
    void updateCrossSelling_Success() {
        // Given
        when(crossSellingService.updateCrossSelling(eq(1L), any())).thenReturn(response);

        // When
        ResponseEntity<GenericResponse> result = controller.updateCrossSelling(1L, request);

        // Then
        assertNotNull(result);
        assertNotNull(result.getBody());
        assertEquals(200, result.getBody().getCode());
        assertTrue(result.getBody().getMessage().contains("actualizada"));

        verify(crossSellingService).updateCrossSelling(eq(1L), any());
    }

    @Test
    void deleteCrossSelling_Success() {
        // Given
        doNothing().when(crossSellingService).deleteCrossSelling(1L, 100L);

        // When
        ResponseEntity<GenericResponse> result = controller.deleteCrossSelling(1L, 100L);

        // Then
        assertNotNull(result);
        assertNotNull(result.getBody());
        assertEquals(200, result.getBody().getCode());
        assertTrue(result.getBody().getMessage().contains("eliminada"));

        verify(crossSellingService).deleteCrossSelling(1L, 100L);
    }

    @Test
    void toggleActive_Success() {
        // Given
        when(crossSellingService.toggleActive(1L, 100L, false)).thenReturn(response);

        // When
        ResponseEntity<GenericResponse> result = controller.toggleActive(1L, 100L, false);

        // Then
        assertNotNull(result);
        assertNotNull(result.getBody());
        assertEquals(200, result.getBody().getCode());
        assertTrue(result.getBody().getMessage().contains("desactivada"));

        verify(crossSellingService).toggleActive(1L, 100L, false);
    }
}
