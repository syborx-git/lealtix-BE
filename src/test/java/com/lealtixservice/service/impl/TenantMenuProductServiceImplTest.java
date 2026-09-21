package com.lealtixservice.service.impl;

import com.lealtixservice.entity.TenantMenuProduct;
import com.lealtixservice.repository.TenantMenuProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantMenuProductServiceImplTest {

    @Mock
    private TenantMenuProductRepository productRepository;

    @InjectMocks
    private TenantMenuProductServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_returnsSavedProduct() {
        TenantMenuProduct product = new TenantMenuProduct();
        when(productRepository.save(product)).thenReturn(product);
        assertEquals(product, service.save(product));
    }

    @Test
    void findById_found() {
        TenantMenuProduct product = new TenantMenuProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        assertTrue(service.findById(1L).isPresent());
    }

    @Test
    void findById_notFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        assertFalse(service.findById(1L).isPresent());
    }

    @Test
    void findAll_returnsList() {
        List<TenantMenuProduct> products = Arrays.asList(new TenantMenuProduct(), new TenantMenuProduct());
        when(productRepository.findAll()).thenReturn(products);
        assertEquals(2, service.findAll().size());
    }
}

