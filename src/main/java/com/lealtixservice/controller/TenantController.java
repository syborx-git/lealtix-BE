package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.TenantDTO;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.service.TenantService;
import com.lealtixservice.service.TenantUserService;
import com.lealtixservice.util.TenantUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/tenant")
@Tag(name = "Tenant Controller", description = "Operaciones CRUD para Tenant")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantUserService  tenantUserService;

    /**
     * Crea un nuevo Tenant.
     * @param dto datos del tenant
     * @return TenantDTO creado
     */
    @Operation(summary = "Crear un nuevo Tenant", description = "Crea un nuevo registro de Tenant.")
    @PostMapping
    public ResponseEntity<TenantDTO> create(@RequestBody TenantDTO dto) {
        Tenant saved;
        try {
            saved = tenantService.create(dto);
        }catch (Exception e){
            log.error("Error creating tenant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok(TenantUserMapper.toTenantDTO(saved));
    }

    /**
     * Obtiene un Tenant por su ID.
     * @param id identificador del tenant
     * @return TenantDTO encontrado o 404
     */
    @Operation(summary = "Obtener Tenant por ID", description = "Obtiene un Tenant por su identificador único.")
    @GetMapping("/{id}")
    public ResponseEntity<TenantDTO> getById(@PathVariable Long id) {
        Optional<Tenant> tenant = tenantService.findById(id);
        return tenant.map(t -> ResponseEntity.ok(TenantUserMapper.toTenantDTO(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtiene un Tenant por su slug.
     * @param slug del tenant
     * @return TenantDTO encontrado o 404
     */
    @Operation(summary = "Obtener Tenant por slug", description = "Obtiene un Tenant por su identificador slug único.")
    @GetMapping("/slug/{slug}")
    public ResponseEntity<GenericResponse> getBySlug(@PathVariable String slug) {
        try {
            var resp = tenantService.getBySlug(slug);
            if (resp == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new GenericResponse(404, "NOT FOUND", null));
            }
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new GenericResponse(200, "SUCCESS", resp));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }


    /**
     * Obtiene un Tenant por email.
     * @param email del tenant
     * @return TenantDTO encontrado o 404
     */
    @Operation(summary = "Obtener Tenant por email", description = "Obtiene un Tenant por su email único.")
    @GetMapping("/config/{email}")
    public ResponseEntity<GenericResponse> getByEmail(@PathVariable String email) {
        try {
            var resp = tenantService.getByEmail(email);
            if (resp != null) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new GenericResponse(200, "SUCCESS", resp));
            }
            
            // Si no es un Tenant, busca si es un usuario creado por un Tenant
            var tenantUserResp = tenantUserService.getByEmail(email);
            if (tenantUserResp != null) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new GenericResponse(200, "SUCCESS", tenantUserResp));
            }
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse(404, "NOT FOUND", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }

    /**
     * Lista todos los Tenants.
     * @return lista de TenantDTO
     */
    @Operation(summary = "Listar todos los Tenants", description = "Obtiene una lista de todos los Tenants registrados.")
    @GetMapping
    public ResponseEntity<List<TenantDTO>> getAll() {
        List<TenantDTO> tenants = tenantService.findAll().stream()
                .map(TenantUserMapper::toTenantDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tenants);
    }

    /**
     * Actualiza un Tenant existente.
     * @param id identificador del tenant
     * @param dto datos actualizados
     * @return TenantDTO actualizado o 404
     */
    @Operation(summary = "Actualizar un Tenant existente", description = "Actualiza los datos de un Tenant existente.")
    @PutMapping("/{id}")
    public ResponseEntity<TenantDTO> update(@PathVariable Long id, @RequestBody TenantDTO dto) {
        Optional<Tenant> existing = tenantService.updateTenant(id, dto);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Tenant toUpdate = TenantUserMapper.toTenant(dto);
        toUpdate.setId(id);
        Tenant updated = tenantService.save(toUpdate);
        return ResponseEntity.ok(TenantUserMapper.toTenantDTO(updated));
    }

    /**
     * Elimina un Tenant por su ID.
     * @param id identificador del tenant
     * @return 204 si fue eliminado
     */
    @Operation(summary = "Eliminar un Tenant por ID", description = "Elimina un Tenant por su identificador único.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tenantService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
