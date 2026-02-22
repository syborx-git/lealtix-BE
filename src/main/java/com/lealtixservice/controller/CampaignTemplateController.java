package com.lealtixservice.controller;

import com.lealtixservice.dto.CampaignTemplateDTO;
import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.GenericResponseProd;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.service.CampaignTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaign-templates")
@Tag(name = "CampaignTemplate", description = "Operaciones sobre templates de campañas")
@RequiredArgsConstructor
public class CampaignTemplateController {

    private final CampaignTemplateService templateService;

    @Operation(summary = "Listar todos los templates")
    @GetMapping
    public ResponseEntity<GenericResponseProd> findAll() {
        try {
            List<CampaignTemplateDTO> list = templateService.findAll();
            return ResponseEntity.ok(new GenericResponseProd(200, "Templates obtenidos", list, list.size()));
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponseProd(500, "Error interno", null, 0));
        }
    }

    @Operation(summary = "Listar templates con estado de uso por tenant")
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<GenericResponseProd> findAllByTenant(@PathVariable Long tenantId) {
        try {
            List<CampaignTemplateDTO> list = templateService.findAllWithUsageStatus(tenantId);
            return ResponseEntity.ok(new GenericResponseProd(200, "Templates obtenidos con estado de uso", list, list.size()));
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponseProd(500, "Error interno", null, 0));
        }
    }

    @Operation(summary = "Obtener template por id")
    @GetMapping("/{id}")
    public ResponseEntity<GenericResponse> findById(@PathVariable Long id) {
        try {
            CampaignTemplateDTO dto = templateService.findById(id);
            return ResponseEntity.ok(new GenericResponse(200, "Template obtenido", dto));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponse(500, "Error interno", null));
        }
    }

    @Operation(summary = "Crear template")
    @PostMapping
    public ResponseEntity<GenericResponse> create(@RequestBody CampaignTemplateDTO request) {
        try {
            CampaignTemplateDTO saved = templateService.create(request);
            return ResponseEntity.ok(new GenericResponse(201, "Template creado", saved));
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponse(500, "Error interno", null));
        }
    }

    @Operation(summary = "Actualizar template")
    @PutMapping("/{id}")
    public ResponseEntity<GenericResponse> update(@PathVariable Long id, @RequestBody CampaignTemplateDTO request) {
        try {
            CampaignTemplateDTO updated = templateService.update(id, request);
            return ResponseEntity.ok(new GenericResponse(200, "Template actualizado", updated));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponse(500, "Error interno", null));
        }
    }

    @Operation(summary = "Eliminar template")
    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponse> delete(@PathVariable Long id) {
        try {
            templateService.delete(id);
            return ResponseEntity.ok(new GenericResponse(200, "Template eliminado", null));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.ok(new GenericResponse(500, "Error interno", null));
        }
    }
}

