package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.ProductCrossSellingRequest;
import com.lealtixservice.dto.ProductCrossSellingResponse;
import com.lealtixservice.service.ProductCrossSellingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para la administración de configuraciones de Cross-Selling.
 * Permite a los tenants crear, listar, actualizar y eliminar sugerencias de productos.
 */
@Slf4j
@RestController
@RequestMapping("/api/cross-selling")
@RequiredArgsConstructor
@Tag(name = "Cross-Selling Admin", description = "Administración de configuraciones de venta cruzada")
public class ProductCrossSellingController {

    private final ProductCrossSellingService crossSellingService;

    @Operation(
        summary = "Crear nueva configuración de cross-selling",
        description = "Permite configurar un producto sugerido cuando se selecciona un producto principal. " +
                      "Valida que ambos productos pertenezcan al mismo tenant y que no sean el mismo producto."
    )
    @PostMapping
    public ResponseEntity<GenericResponse> createCrossSelling(
            @Valid @RequestBody ProductCrossSellingRequest request) {
        try {
            log.info("POST /api/cross-selling - Creating cross-selling: productId={}, suggestedProductId={}, tenantId={}", 
                     request.getProductId(), request.getSuggestedProductId(), request.getTenantId());
            
            ProductCrossSellingResponse response = crossSellingService.createCrossSelling(request);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GenericResponse(201, "Configuración creada exitosamente", response));
                
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating cross-selling: {}", e.getMessage());
            return ResponseEntity.ok(
                new GenericResponse(400, "Error de validación: " + e.getMessage(), null)
            );
        } catch (Exception e) {
            log.error("Error creating cross-selling", e);
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }

    @Operation(
        summary = "Obtener todas las configuraciones de un tenant",
        description = "Lista todas las configuraciones de cross-selling del tenant, incluyendo activas e inactivas."
    )
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<GenericResponse> getAllByTenant(
            @Parameter(description = "ID del tenant") 
            @PathVariable Long tenantId) {
        try {
            log.info("GET /api/cross-selling/tenant/{} - Fetching all configurations", tenantId);
            
            List<ProductCrossSellingResponse> configurations = crossSellingService.getAllByTenant(tenantId);
            
            return ResponseEntity.ok(
                new GenericResponse(200, "Configuraciones obtenidas exitosamente", configurations)
            );
            
        } catch (Exception e) {
            log.error("Error fetching cross-selling configurations for tenant {}", tenantId, e);
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }

    @Operation(
        summary = "Obtener configuraciones de un producto específico",
        description = "Lista todas las sugerencias configuradas para un producto en particular."
    )
    @GetMapping("/product/{productId}")
    public ResponseEntity<GenericResponse> getByProduct(
            @Parameter(description = "ID del producto") 
            @PathVariable Long productId,
            @Parameter(description = "ID del tenant") 
            @RequestParam Long tenantId) {
        try {
            log.info("GET /api/cross-selling/product/{} - tenantId={}", productId, tenantId);
            
            List<ProductCrossSellingResponse> configurations = 
                crossSellingService.getByProduct(productId, tenantId);
            
            return ResponseEntity.ok(
                new GenericResponse(200, "Configuraciones obtenidas exitosamente", configurations)
            );
            
        } catch (Exception e) {
            log.error("Error fetching cross-selling for product {} tenant {}", productId, tenantId, e);
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }

    @Operation(
        summary = "Actualizar configuración existente",
        description = "Permite modificar una configuración de cross-selling existente."
    )
    @PutMapping("/{id}")
    public ResponseEntity<GenericResponse> updateCrossSelling(
            @Parameter(description = "ID de la configuración") 
            @PathVariable Long id,
            @Valid @RequestBody ProductCrossSellingRequest request) {
        try {
            log.info("PUT /api/cross-selling/{} - Updating configuration", id);
            
            ProductCrossSellingResponse response = crossSellingService.updateCrossSelling(id, request);
            
            return ResponseEntity.ok(
                new GenericResponse(200, "Configuración actualizada exitosamente", response)
            );
            
        } catch (IllegalArgumentException e) {
            log.warn("Validation error updating cross-selling {}: {}", id, e.getMessage());
            return ResponseEntity.ok(
                new GenericResponse(400, "Error de validación: " + e.getMessage(), null)
            );
        } catch (Exception e) {
            log.error("Error updating cross-selling {}", id, e);
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }

    @Operation(
        summary = "Eliminar configuración",
        description = "Elimina permanentemente una configuración de cross-selling."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponse> deleteCrossSelling(
            @Parameter(description = "ID de la configuración") 
            @PathVariable Long id,
            @Parameter(description = "ID del tenant para validación de seguridad") 
            @RequestParam Long tenantId) {
        try {
            log.info("DELETE /api/cross-selling/{} - tenantId={}", id, tenantId);
            
            crossSellingService.deleteCrossSelling(id, tenantId);
            
            return ResponseEntity.ok(
                new GenericResponse(200, "Configuración eliminada exitosamente", null)
            );
            
        } catch (IllegalArgumentException e) {
            log.warn("Validation error deleting cross-selling {}: {}", id, e.getMessage());
            return ResponseEntity.ok(
                new GenericResponse(400, "Error de validación: " + e.getMessage(), null)
            );
        } catch (Exception e) {
            log.error("Error deleting cross-selling {}", id, e);
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }

    @Operation(
        summary = "Activar/Desactivar configuración",
        description = "Cambia el estado activo/inactivo de una configuración sin eliminarla."
    )
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<GenericResponse> toggleActive(
            @Parameter(description = "ID de la configuración") 
            @PathVariable Long id,
            @Parameter(description = "ID del tenant") 
            @RequestParam Long tenantId,
            @Parameter(description = "Nuevo estado (true=activo, false=inactivo)") 
            @RequestParam Boolean isActive) {
        try {
            log.info("PATCH /api/cross-selling/{}/toggle - tenantId={}, isActive={}", id, tenantId, isActive);
            
            ProductCrossSellingResponse response = crossSellingService.toggleActive(id, tenantId, isActive);
            
            String message = isActive ? "Configuración activada exitosamente" : "Configuración desactivada exitosamente";
            
            return ResponseEntity.ok(
                new GenericResponse(200, message, response)
            );
            
        } catch (IllegalArgumentException e) {
            log.warn("Validation error toggling cross-selling {}: {}", id, e.getMessage());
            return ResponseEntity.ok(
                new GenericResponse(400, "Error de validación: " + e.getMessage(), null)
            );
        } catch (Exception e) {
            log.error("Error toggling cross-selling {}", id, e);
            return ResponseEntity.ok(
                new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null)
            );
        }
    }
}
