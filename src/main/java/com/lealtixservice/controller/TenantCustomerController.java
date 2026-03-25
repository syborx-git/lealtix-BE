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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Operation(summary = "Listar clientes por tenant (paginado)")
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<GenericResponse> getByTenantId(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String email) {
        try {
            // Log de parámetros recibidos
            log.info("GET /tenant-customers/tenant/{} - page={}, size={}, sort={}, email={}", 
                    tenantId, page, size, sort, email);

            // Validar parámetros
            if (page < 0) {
                log.warn("Invalid page parameter: {}, using default 0", page);
                page = 0;
            }
            if (size <= 0 || size > 100) {
                log.warn("Invalid size parameter: {}, using default 10", size);
                size = 10;
            }

            // Parsear sort (formato: "campo,asc" o "campo,desc")
            org.springframework.data.domain.Sort sortObj = parseSort(sort);
            
            // Crear Pageable
            Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sortObj);
            
            // Ejecutar consulta con o sin filtro de email
            Page<TenantCustomer> pageResult;
            if (email != null && !email.trim().isEmpty()) {
                String emailTrimmed = email.trim();
                log.debug("Applying email filter: {}", emailTrimmed);
                pageResult = tenantCustomerService.findByTenantIdAndEmailPaginated(tenantId, emailTrimmed, pageable);
            } else {
                pageResult = tenantCustomerService.findByTenantIdPaginated(tenantId, pageable);
            }
            
            // Mapear a DTOs
            List<TenantCustomerDTO> customers = pageResult.getContent().stream()
                    .map(TenantCustomerMapper::toDTO)
                    .collect(Collectors.toList());
            
            // Construir respuesta con metadata de paginación
            Map<String, Object> response = new HashMap<>();
            response.put("content", customers);
            response.put("page", pageResult.getNumber());
            response.put("size", pageResult.getSize());
            response.put("totalElements", pageResult.getTotalElements());
            response.put("totalPages", pageResult.getTotalPages());
            
            log.info("Returning {} customers, page {}/{}, totalElements={}", 
                    customers.size(), pageResult.getNumber() + 1, pageResult.getTotalPages(), 
                    pageResult.getTotalElements());
            
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new GenericResponse(200, "OK", response));
        } catch (Exception e) {
            log.error("Error retrieving tenant customers for tenantId={}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, "Error: " + e.getMessage(), null));
        }
    }

    /**
     * Parsea el parámetro sort en formato "campo,dirección" (ej: "name,asc" o "email,desc")
     * Si el formato es inválido o el campo no existe, retorna ordenamiento por defecto (createdAt desc)
     */
    private org.springframework.data.domain.Sort parseSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            log.debug("No sort parameter provided, using default: createdAt desc");
            return org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
        }
        
        try {
            String[] parts = sort.split(",");
            if (parts.length != 2) {
                log.warn("Invalid sort format: {}, expected 'field,direction'. Using default sort.", sort);
                return org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
            }
            
            String field = parts[0].trim();
            String direction = parts[1].trim().toLowerCase();
            
            // Mapear nombres de campos del frontend a nombres de campos de la entidad
            field = mapSortField(field);
            
            // Validar dirección
            org.springframework.data.domain.Sort.Direction sortDirection;
            if ("desc".equals(direction)) {
                sortDirection = org.springframework.data.domain.Sort.Direction.DESC;
            } else if ("asc".equals(direction)) {
                sortDirection = org.springframework.data.domain.Sort.Direction.ASC;
            } else {
                log.warn("Invalid sort direction: {}, using ASC", direction);
                sortDirection = org.springframework.data.domain.Sort.Direction.ASC;
            }
            
            log.debug("Parsed sort: field={}, direction={}", field, sortDirection);
            return org.springframework.data.domain.Sort.by(sortDirection, field);
            
        } catch (Exception e) {
            log.warn("Error parsing sort parameter '{}': {}. Using default sort.", sort, e.getMessage());
            return org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
        }
    }

    /**
     * Mapea nombres de campos del frontend a nombres de campos de la entidad TenantCustomer
     */
    private String mapSortField(String field) {
        if (field == null) {
            return "createdAt";
        }
        
        // Mapear campos que puedan tener nombres diferentes entre frontend y backend
        switch (field.toLowerCase()) {
            case "nombrecompleto":
            case "nombre":
                return "name";
            case "email":
                return "email";
            case "genero":
            case "gender":
                return "gender";
            case "telefono":
            case "phone":
                return "phone";
            case "fechanacimiento":
            case "birthdate":
                return "birthDate";
            case "createdat":
            case "fechacreacion":
                return "createdAt";
            case "updatedat":
            case "fechaactualizacion":
                return "updatedAt";
            default:
                // Si el campo no está mapeado, intentar usarlo tal cual
                // (asumiendo que el frontend usa nombres correctos de la entidad)
                log.debug("Using unmapped field for sort: {}", field);
                return field;
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
                toUpdate.setActive(existingEntity.getActive());
            }

            TenantCustomer updated = tenantCustomerService.update(toUpdate);
            TenantCustomerDTO respDTO = TenantCustomerMapper.toDTO(updated);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new GenericResponse(200, "SUCCESS", respDTO));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }
}