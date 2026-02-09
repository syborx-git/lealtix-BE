package com.lealtixservice.controller;

import com.lealtixservice.dto.BulkCustomerUploadResponse;
import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.TenantCustomerDTO;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.service.TenantCustomerService;
import com.lealtixservice.util.TenantCustomerMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "TenantCustomer", description = "Operaciones relacionadas con los clientes de un tenant")
@RestController
@RequestMapping("/api/tenant-customers")
public class TenantCustomerController {

    @Autowired
    private TenantCustomerService tenantCustomerService;

    @Operation(summary = "Crear un nuevo cliente")
    @PostMapping
    public ResponseEntity<GenericResponse> create(@RequestBody TenantCustomerDTO customerDTO) {
        try {
            TenantCustomer toSave = TenantCustomerMapper.toEntity(customerDTO);
            TenantCustomer saved = tenantCustomerService.save(toSave);
            TenantCustomerDTO respDTO = TenantCustomerMapper.toDTO(saved);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new GenericResponse(200, "SUCCESS", respDTO));
        } catch (Exception e) {
            log.error("Error creating TenantCustomer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }

    @Operation(summary = "Carga masiva de clientes")
    @PostMapping("/bulk-upload")
    public ResponseEntity<GenericResponse> bulkUpload(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId,
            @RequestBody List<TenantCustomerDTO> customers) {
        try {
            BulkCustomerUploadResponse response = tenantCustomerService.bulkUpload(tenantId, customers);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new GenericResponse(200, "SUCCESS", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }

    @Operation(summary = "Obtener un cliente por ID")
    @GetMapping("/{id}")
    public ResponseEntity<GenericResponse> getById(@PathVariable Long id) {
        try {
            Optional<TenantCustomer> customer = tenantCustomerService.findById(id);
            if (customer.isPresent()) {
                TenantCustomerDTO dto = TenantCustomerMapper.toDTO(customer.get());
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new GenericResponse(200, "SUCCESS", dto));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new GenericResponse(404, "NOT FOUND", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }

    @Operation(summary = "Listar todos los clientes")
    @GetMapping
    public ResponseEntity<List<TenantCustomerDTO>> getAll() {
        List<TenantCustomerDTO> list = tenantCustomerService.findAll().stream()
                .map(TenantCustomerMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Eliminar un cliente por ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tenantCustomerService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Soft delete - Marcar cliente como inactivo")
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<GenericResponse> softDelete(@PathVariable Long id) {
        try {
            Optional<TenantCustomer> customer = tenantCustomerService.findById(id);
            if (customer.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new GenericResponse(404, "NOT FOUND", null));
            }
            tenantCustomerService.softDeleteById(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new GenericResponse(200, "SUCCESS", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }

    @Operation(summary = "Listar clientes por tenant (paginado)")
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<GenericResponse> getByTenantId(
            @PathVariable Long tenantId,
            @Parameter(description = "Filtro opcional por email") @RequestParam(required = false) String email,
            @Parameter(description = "Número de página (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de la página") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            Pageable pageable = PageRequest.of(page, pageSize);
            Page<TenantCustomer> result;
            
            if (email != null && !email.trim().isEmpty()) {
                result = tenantCustomerService.findByTenantIdAndEmailPaginated(tenantId, email, pageable);
            } else {
                result = tenantCustomerService.findByTenantIdPaginated(tenantId, pageable);
            }
            
            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new GenericResponse(404, "NOT FOUND", null));
            }
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new GenericResponse(200, "SUCCESS", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }

    @Operation(summary = "Actualizar un cliente")
    @PutMapping("/{id}")
    public ResponseEntity<GenericResponse> update(@PathVariable Long id, @RequestBody TenantCustomerDTO customerDTO) {
        try {
            Optional<TenantCustomer> existing = tenantCustomerService.findById(id);
            if (existing.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new GenericResponse(404, "NOT FOUND", null));
            }
            // merge: conservar valores existentes si el DTO no provee campos de consentimiento
            TenantCustomer existingEntity = existing.get();
            customerDTO.setId(id);
            TenantCustomer toUpdate = TenantCustomerMapper.toEntity(customerDTO);

            // Preserve tenant and createdAt from existing if mapper didn't provide them
            if (toUpdate.getTenant() == null) {
                toUpdate.setTenant(existingEntity.getTenant());
            }
            if (toUpdate.getCreatedAt() == null) {
                toUpdate.setCreatedAt(existingEntity.getCreatedAt());
            }

            // Merge acceptedPromotions: if DTO did not include it (null) keep existing value
            if (customerDTO.getAcceptedPromotions() == null) {
                toUpdate.setAcceptedPromotions(existingEntity.isAcceptedPromotions());
            }
            // Merge acceptedAt: if DTO did not include it keep existing
            if (customerDTO.getAcceptedAt() == null) {
                toUpdate.setAcceptedAt(existingEntity.getAcceptedAt());
            }
            // Merge active: if DTO did not include it keep existing value
            if (customerDTO.getActive() == null) {
                toUpdate.setActive(existingEntity.isActive());
            }

            TenantCustomer updated = tenantCustomerService.save(toUpdate);
            TenantCustomerDTO respDTO = TenantCustomerMapper.toDTO(updated);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new GenericResponse(200, "SUCCESS", respDTO));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }
}