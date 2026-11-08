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

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private com.lealtixservice.repository.TenantRepository tenantRepository;

    @Operation(summary = "Semillar datos y menú completo para VectorInside")
    @GetMapping("/seed/vectorinside")
    public ResponseEntity<GenericResponse> seedVectorInside(
            @RequestParam(name = "secret", required = false, defaultValue = "") String secret) {
        log.info("Iniciando semillado de VectorInside vía endpoint...");
        try {
            org.springframework.core.io.Resource resource = new org.springframework.core.io.ClassPathResource("db/migration/V76__seed_vectorinside_tenant_and_menu.sql");
            String sql = org.springframework.util.StreamUtils.copyToString(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);

            try (java.sql.Connection conn = dataSource.getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }

            Optional<Tenant> tenantOpt = tenantRepository.getBySlug("vectorinside");
            Long tenantId = tenantOpt.map(Tenant::getId).orElse(null);

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("status", "SUCCESS");
            data.put("slug", "vectorinside");
            data.put("tenantId", tenantId);
            data.put("adminEmail", "admin@vectorinside.com");
            data.put("password", "VectorInside2025!");

            log.info("¡Semillado de VectorInside completado exitosamente! TenantId: {}", tenantId);
            return ResponseEntity.ok(new GenericResponse(200, "¡Tenant VectorInside y menú semillados exitosamente!", data));
        } catch (Exception e) {
            log.error("Error al semillar VectorInside: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error al semillar: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Semillar datos y menú completo para VectorInside (POST)")
    @PostMapping("/seed/vectorinside")
    public ResponseEntity<GenericResponse> seedVectorInsidePost(
            @RequestParam(name = "secret", required = false, defaultValue = "") String secret) {
        return seedVectorInside(secret);
    }

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
        return ResponseEntity.ok(TenantUserMapper.toTenantDTO(existing.get()));
    }

    /**
     * Guarda el HTML del sitio personalizado (Web Studio / Admin Page) de un tenant.
     * El landing público lo renderiza cuando el tenant tiene este HTML guardado.
     */
    @Operation(summary = "Guardar el sitio personalizado (Web Studio) de un Tenant")
    @PutMapping("/{id}/custom-site")
    public ResponseEntity<GenericResponse> saveCustomSite(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        Optional<Tenant> existing = tenantService.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse(404, "Tenant no encontrado", null));
        }
        Tenant tenant = existing.get();
        tenant.setCustomSiteHtml(body != null ? body.get("html") : null);
        tenantService.save(tenant);
        return ResponseEntity.ok(new GenericResponse(200, "Sitio personalizado guardado", null));
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
