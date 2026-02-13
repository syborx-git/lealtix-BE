package com.lealtixservice.service.impl;

import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.repository.TenantCustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantCustomerServiceImplTest {

    @Mock
    private TenantCustomerRepository repository;

    @InjectMocks
    private TenantCustomerServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_returnsSavedEntity() {
        TenantCustomer customer = new TenantCustomer();
        when(repository.save(customer)).thenReturn(customer);
        assertEquals(customer, service.save(customer));
    }

    @Test
    void update_updatesEntity() {
        TenantCustomer customer = new TenantCustomer();
        customer.setId(1L);
        customer.setName("Nuevo Nombre");
        customer.setEmail("nuevo@email.com");
        customer.setActive(true);
        customer.setUpdatedAt(LocalDateTime.now());
        TenantCustomer existing = new TenantCustomer();
        existing.setId(1L);
        existing.setName("Viejo Nombre");
        existing.setEmail("viejo@email.com");
        existing.setActive(false);
        existing.setUpdatedAt(LocalDateTime.now().minusDays(1));
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(TenantCustomer.class))).thenAnswer(i -> i.getArgument(0));
        TenantCustomer result = service.update(customer);
        assertNotNull(result);
        assertEquals("Nuevo Nombre", result.getName());
        assertEquals("nuevo@email.com", result.getEmail());
        assertTrue(result.getActive());
    }

    @Test
    void update_returnsNullIfNotFound() {
        TenantCustomer customer = new TenantCustomer();
        customer.setId(99L);
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertNull(service.update(customer));
    }

    @Test
    void findById_found() {
        TenantCustomer customer = new TenantCustomer();
        when(repository.findById(1L)).thenReturn(Optional.of(customer));
        assertTrue(service.findById(1L).isPresent());
    }

    @Test
    void findById_notFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertFalse(service.findById(1L).isPresent());
    }

    @Test
    void findAll_returnsList() {
        List<TenantCustomer> customers = Arrays.asList(new TenantCustomer(), new TenantCustomer());
        when(repository.findAll()).thenReturn(customers);
        assertEquals(2, service.findAll().size());
    }

    @Test
    void deleteById_callsRepository() {
        doNothing().when(repository).deleteById(1L);
        service.deleteById(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void findByTenantId_returnsList() {
        List<TenantCustomer> customers = Arrays.asList(new TenantCustomer(), new TenantCustomer());
        when(repository.findByTenantId(10L)).thenReturn(customers);
        assertEquals(2, service.findByTenantId(10L).size());
    }
}
