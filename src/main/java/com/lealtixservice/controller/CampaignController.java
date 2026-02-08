package com.lealtixservice.controller;

import com.lealtixservice.dto.*;
import com.lealtixservice.exception.BusinessRuleException;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/campaigns")
@Tag(name = "Campaign", description = "Operaciones sobre campañas")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @Operation(summary = "Crear campaña")
    @PostMapping
    public ResponseEntity<GenericResponse> create(@RequestBody CreateCampaignRequest request) {
        try {
            CampaignResponse response = campaignService.create(request);
            return ResponseEntity.ok(new GenericResponse(201, "Campaña creada", response));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.ok(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponse(500, "Error interno", null));
        }
    }

    @Operation(summary = "Actualizar campaña")
    @PutMapping("/{id}")
    public ResponseEntity<GenericResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateCampaignRequest request) {
        try {
            CampaignResponse response = campaignService.update(id, request);
            return ResponseEntity.ok(new GenericResponse(200, "Campaña actualizada", response));
        } catch (ResourceNotFoundException ex) {
            log.error("Error updating campaign", ex);
            return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), null));
        } catch (IllegalArgumentException ex) {
            log.error("Error updating campaign", ex);
            return ResponseEntity.ok(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating campaign", e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno", null));
        }
    }

    @Operation(summary = "Obtener campaña por id")
    @GetMapping("/{id}")
    public ResponseEntity<GenericResponse> findById(@PathVariable Long id) {
        try {
            CampaignResponse response = campaignService.findById(id);
            return ResponseEntity.ok(new GenericResponse(200, "Campaña obtenida", response));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponse(500, "Error interno", null));
        }
    }

    @Operation(summary = "Listar campañas por businessId")
    @GetMapping("/business/{businessId}")
    public ResponseEntity<GenericResponseProd> findByBusiness(@PathVariable Long businessId) {
        try {
            List<CampaignResponse> list = campaignService.findByBusinessId(businessId);
            return ResponseEntity.ok(new GenericResponseProd(200, "Campañas obtenidas", list, list.size()));
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponseProd(500, "Error interno", null, 0));
        }
    }

    @Operation(summary = "Eliminar campaña")
    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponse> delete(@PathVariable Long id) {
        try {
            campaignService.delete(id);
            return ResponseEntity.ok(new GenericResponse(200, "Campaña eliminada", null));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponse(500, "Error interno", null));
        }
    }

    // ===== ENDPOINTS PARA BORRADORES =====

    @Operation(summary = "Crear campaña directamente (no borrador)")
    @PostMapping("/final")
    public ResponseEntity<GenericResponse> createFinal(@Valid @RequestBody CreateCampaignDTO dto) {
        try {
            CampaignResponse response = campaignService.create(dto);
            return ResponseEntity.ok(new GenericResponse(201, "Campaña creada exitosamente", response));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.ok(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponse(500, "Error interno", null));
        }
    }

    @Operation(summary = "Crear borrador de campaña")
    @PostMapping("/draft")
    public ResponseEntity<GenericResponse> createDraft(@Valid @RequestBody CreateCampaignDraftDto dto) {
        try {
            CampaignResponse response = campaignService.createDraft(dto);
            return ResponseEntity.ok(new GenericResponse(201, "Borrador creado exitosamente", response));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.ok(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponse(500, "Error interno", null));
        }
    }

    @Operation(summary = "Actualizar borrador de campaña")
    @PutMapping("/draft/{id}")
    public ResponseEntity<GenericResponse> updateDraft(@PathVariable Long id, @Valid @RequestBody CreateCampaignDraftDto dto) {
        try {
            CampaignResponse response = campaignService.updateDraft(id, dto);
            return ResponseEntity.ok(new GenericResponse(200, "Borrador actualizado exitosamente", response));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), null));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.ok(new GenericResponse(400, ex.getReason(), null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.ok(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponse(500, "Error interno", null));
        }
    }

    @Operation(summary = "Publicar borrador de campaña")
    @PostMapping("/draft/{id}/publish")
    public ResponseEntity<GenericResponse> publishDraft(@PathVariable Long id) {
        try {
            CampaignResponse response = campaignService.publishDraft(id);
            return ResponseEntity.ok(new GenericResponse(200, "Campaña publicada exitosamente", response));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), null));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.ok(new GenericResponse(400, ex.getReason(), null));
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponse(500, "Error interno", null));
        }
    }

    @Operation(summary = "Obtener borradores por businessId")
    @GetMapping("/business/{businessId}/drafts")
    public ResponseEntity<GenericResponseProd> getDraftsByBusiness(@PathVariable Long businessId) {
        try {
            List<CampaignResponse> drafts = campaignService.getDraftsByBusiness(businessId);
            return ResponseEntity.ok(new GenericResponseProd(200, "Borradores obtenidos exitosamente", drafts, drafts.size()));
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponseProd(500, "Error interno", null, 0));
        }
    }

    @Operation(summary = "Obtener campañas activas por businessId")
    @GetMapping("/business/{businessId}/active")
    public ResponseEntity<GenericResponseProd> getActiveCampaigns(@PathVariable Long businessId) {
        try {
            List<CampaignResponse> campaigns = campaignService.getActiveCampaigns(businessId);
            return ResponseEntity.ok(new GenericResponseProd(200, "Campañas activas obtenidas exitosamente", campaigns, campaigns.size()));
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponseProd(500, "Error interno", null, 0));
        }
    }

    @Operation(summary = "Configurar reward para una campaña")
    @PostMapping("/{campaignId}/reward")
    public ResponseEntity<GenericResponse> configureReward(
            @PathVariable Long campaignId,
            @Valid @RequestBody ConfigureRewardRequest request) {
        try {
            PromotionRewardResponse response = campaignService.configureReward(campaignId, request);
            return ResponseEntity.ok(new GenericResponse(200, "Reward configurado exitosamente", response));
        } catch (ResourceNotFoundException ex) {
            log.error("Campaña no encontrada: {}", ex.getMessage());
            return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), null));
        } catch (BusinessRuleException ex) {
            log.error("Error de validación de negocio: {}", ex.getMessage());
            return ResponseEntity.ok(new GenericResponse(422, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error configurando reward", e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Activar campaña (FASE 4)")
    @PostMapping("/{id}/activate")
    public ResponseEntity<GenericResponse> activateCampaign(@PathVariable Long id) {
        try {
            CampaignResponse response = campaignService.activateCampaign(id);
            return ResponseEntity.ok(new GenericResponse(200, "Campaña activada exitosamente", response));
        } catch (ResourceNotFoundException ex) {
            log.error("Campaña no encontrada: {}", ex.getMessage());
            return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), null));
        } catch (BusinessRuleException ex) {
            log.error("Error de validación al activar campaña: {}", ex.getMessage());
            return ResponseEntity.ok(new GenericResponse(422, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error activando campaña", e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Validar si un tenant tiene campaña de bienvenida activa")
    @GetMapping("/tenant/{tenantId}/has-welcome")
    public ResponseEntity<GenericResponse> hasActiveWelcomeCampaign(@PathVariable Long tenantId) {
        try {
            boolean hasActive = campaignService.hasActiveWelcomeCampaign(tenantId);
            WelcomeCheckResponse response = WelcomeCheckResponse.builder()
                    .tenantId(tenantId)
                    .hasActiveWelcomeCampaign(hasActive)
                    .message(hasActive ? "El tenant tiene una campaña de bienvenida activa" :
                            "El tenant no tiene campañas de bienvenida activas")
                    .build();
            return ResponseEntity.ok(new GenericResponse(200, "Verificación completada", response));
        } catch (Exception e) {
            log.error("Error verificando campaña de bienvenida para tenant {}", tenantId, e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Crear campaña de bienvenida automática para un tenant")
    @PostMapping("/tenant/{tenantId}/welcome")
    public ResponseEntity<GenericResponse> createWelcomeCampaignForTenant(@PathVariable Long tenantId) {
        try {
            CampaignResponse response = campaignService.createWelcomeCampaignForTenant(tenantId);
            return ResponseEntity.ok(new GenericResponse(201, "Campaña de bienvenida creada exitosamente", response));
        } catch (ResourceNotFoundException ex) {
            log.error("Error creando campaña de bienvenida: {}", ex.getMessage());
            return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error creando campaña de bienvenida para tenant {}", tenantId, e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Validar configuración de campañas de un negocio (alertas UI)")
    @GetMapping("/business/{businessId}/validate")
    public ResponseEntity<GenericResponse> validateCampaignsForBusiness(@PathVariable Long businessId) {
        try {
            List<CampaignValidationResult> validations = campaignService.validateCampaignsForBusiness(businessId);
            return ResponseEntity.ok(new GenericResponse(200, "Validación completada", validations));
        } catch (Exception e) {
            log.error("Error validando campañas para businessId {}", businessId, e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    // DTO interno para respuesta de validación de bienvenida
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WelcomeCheckResponse {
        private Long tenantId;
        private Boolean hasActiveWelcomeCampaign;
        private String message;
    }
}
