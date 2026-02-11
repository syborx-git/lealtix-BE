package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.TenantCustomerDTO;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.service.TenantCustomerService;
import com.lealtixservice.util.TenantCustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantCustomerControllerTest {

    @Mock
    private TenantCustomerService service;

    @InjectMocks
    private TenantCustomerController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void create_success() {
        TenantCustomerDTO req = TenantCustomerDTO.builder()
                .tenantId(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();
        TenantCustomer savedEntity = TenantCustomerMapper.toEntity(req);
        when(service.save(any(TenantCustomer.class))).thenReturn(savedEntity);
        ResponseEntity<GenericResponse> response = controller.create(req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertTrue(response.getBody().getObject() instanceof TenantCustomerDTO);
    }

    @Test
    void getById_found() {
        TenantCustomer entity = new TenantCustomer();
        when(service.findById(1L)).thenReturn(Optional.of(entity));
        ResponseEntity<GenericResponse> response = controller.getById(1L);
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertTrue(response.getBody().getObject() instanceof TenantCustomerDTO);
    }

    @Test
    void getById_notFound() {
        when(service.findById(1L)).thenReturn(Optional.empty());
        ResponseEntity<GenericResponse> response = controller.getById(1L);
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getCode());
        assertEquals("NOT FOUND", response.getBody().getMessage());
    }

    @Test
    void getAll_returnsList() {
        List<TenantCustomer> customers = Arrays.asList(new TenantCustomer(), new TenantCustomer());
        when(service.findAll()).thenReturn(customers);
        ResponseEntity<List<TenantCustomerDTO>> response = controller.getAll();
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().get(0) instanceof TenantCustomerDTO);
        verify(service).findAll();
    }

    @Test
    void delete_noContent() {
        doNothing().when(service).deleteById(1L);
        ResponseEntity<Void> response = controller.delete(1L);
        assertEquals(204, response.getStatusCode().value());
        verify(service).deleteById(1L);
    }


    @Test
    void update_success() {
        TenantCustomerDTO req = TenantCustomerDTO.builder()
                .tenantId(1L)
                .name("Jane Doe")
                .email("jane@example.com")
                .build();
        TenantCustomer existing = new TenantCustomer();
        when(service.findById(1L)).thenReturn(Optional.of(existing));
        when(service.save(any(TenantCustomer.class))).thenReturn(existing);
        ResponseEntity<GenericResponse> response = controller.update(1L, req);
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertTrue(response.getBody().getObject() instanceof TenantCustomerDTO);
        verify(service).save(any(TenantCustomer.class));
    }

    @Test
    void update_notFound() {
        TenantCustomerDTO req = TenantCustomerDTO.builder().tenantId(1L).name("Jane").email("jane@example.com").build();
        when(service.findById(1L)).thenReturn(Optional.empty());
        ResponseEntity<GenericResponse> response = controller.update(1L, req);
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getCode());
        assertEquals("NOT FOUND", response.getBody().getMessage());
    }
}
