package com.lealtixservice.controller;

import com.lealtixservice.dto.AsignarMeseroRequest;
import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.MesaDTO;
import com.lealtixservice.dto.MesaRequest;
import com.lealtixservice.enums.MesaEstado;
import com.lealtixservice.service.MesaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/mesas")
@Tag(name = "Mesas", description = "Operaciones sobre el mapeo de mesas del local (hostess)")
public class MesaController {

    private final MesaService mesaService;

    public MesaController(MesaService mesaService) {
        this.mesaService = mesaService;
    }

    @Operation(summary = "Listar mesas de un tenant")
    @GetMapping
    public ResponseEntity<GenericResponse> listByTenant(@RequestParam Long tenantId) {
        try {
            List<MesaDTO> mesas = mesaService.listByTenant(tenantId);
            return ResponseEntity.ok(new GenericResponse(200, "SUCCESS", mesas));
        } catch (Exception e) {
            log.error("Error listando mesas para tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Listar mesas de un tenant por estado")
    @GetMapping("/estado")
    public ResponseEntity<GenericResponse> listByTenantAndEstado(
            @RequestParam Long tenantId,
            @RequestParam MesaEstado estado) {
        try {
            List<MesaDTO> mesas = mesaService.listByTenantAndEstado(tenantId, estado);
            return ResponseEntity.ok(new GenericResponse(200, "SUCCESS", mesas));
        } catch (Exception e) {
            log.error("Error listando mesas por estado {} para tenant {}: {}", estado, tenantId, e.getMessage(), e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Obtener mesa por ID")
    @GetMapping("/{id}")
    public ResponseEntity<GenericResponse> getById(@PathVariable Long id, @RequestParam Long tenantId) {
        try {
            MesaDTO mesa = mesaService.getById(id, tenantId);
            return ResponseEntity.ok(new GenericResponse(200, "SUCCESS", mesa));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(new GenericResponse(404, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error obteniendo mesa {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Crear nueva mesa")
    @PostMapping
    public ResponseEntity<GenericResponse> create(@RequestParam Long tenantId, @RequestBody MesaRequest request) {
        try {
            MesaDTO mesa = mesaService.create(tenantId, request);
            return ResponseEntity.ok(new GenericResponse(201, "Mesa creada exitosamente", mesa));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error creando mesa para tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Actualizar mesa")
    @PutMapping("/{id}")
    public ResponseEntity<GenericResponse> update(@PathVariable Long id,
                                                  @RequestParam Long tenantId,
                                                  @RequestBody MesaRequest request) {
        try {
            MesaDTO mesa = mesaService.update(id, tenantId, request);
            return ResponseEntity.ok(new GenericResponse(200, "Mesa actualizada exitosamente", mesa));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error actualizando mesa {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Asignar o quitar mesero a una mesa")
    @PutMapping("/{id}/asignar-mesero")
    public ResponseEntity<GenericResponse> assignMesero(@PathVariable Long id,
                                                        @RequestParam Long tenantId,
                                                        @RequestBody AsignarMeseroRequest request) {
        try {
            MesaDTO mesa = mesaService.assignMesero(id, tenantId, request);
            return ResponseEntity.ok(new GenericResponse(200, "Mesero asignado exitosamente", mesa));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error asignando mesero a mesa {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Cambiar estado de la mesa (LIBRE/OCUPADA/RESERVADA)")
    @PutMapping("/{id}/estado")
    public ResponseEntity<GenericResponse> changeEstado(@PathVariable Long id,
                                                        @RequestParam Long tenantId,
                                                        @RequestBody MesaEstado estado) {
        try {
            MesaDTO mesa = mesaService.changeEstado(id, tenantId, estado);
            return ResponseEntity.ok(new GenericResponse(200, "Estado de mesa actualizado exitosamente", mesa));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error cambiando estado de mesa {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Eliminar mesa")
    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponse> delete(@PathVariable Long id, @RequestParam Long tenantId) {
        try {
            mesaService.delete(id, tenantId);
            return ResponseEntity.ok(new GenericResponse(200, "Mesa eliminada exitosamente", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(new GenericResponse(404, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error eliminando mesa {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }
}