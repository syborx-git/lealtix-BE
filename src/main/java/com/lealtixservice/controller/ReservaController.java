package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.ReservaDTO;
import com.lealtixservice.dto.ReservaRequest;
import com.lealtixservice.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservaciones", description = "Operaciones sobre reservaciones del local (hostess)")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Operation(summary = "Listar reservaciones de un tenant")
    @GetMapping
    public ResponseEntity<GenericResponse> listByTenant(@RequestParam Long tenantId) {
        try {
            List<ReservaDTO> reservas = reservaService.listByTenant(tenantId);
            return ResponseEntity.ok(new GenericResponse(200, "SUCCESS", reservas));
        } catch (Exception e) {
            log.error("Error listando reservaciones para tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Obtener reservación por ID")
    @GetMapping("/{id}")
    public ResponseEntity<GenericResponse> getById(@PathVariable Long id, @RequestParam Long tenantId) {
        try {
            ReservaDTO reserva = reservaService.getById(id, tenantId);
            return ResponseEntity.ok(new GenericResponse(200, "SUCCESS", reserva));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(new GenericResponse(404, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error obteniendo reservación {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Crear nueva reservación")
    @PostMapping
    public ResponseEntity<GenericResponse> create(@RequestParam Long tenantId, @RequestBody ReservaRequest request) {
        try {
            ReservaDTO reserva = reservaService.create(tenantId, request);
            return ResponseEntity.ok(new GenericResponse(201, "Reservación creada exitosamente", reserva));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error creando reservación para tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Actualizar reservación")
    @PutMapping("/{id}")
    public ResponseEntity<GenericResponse> update(@PathVariable Long id,
                                                  @RequestParam Long tenantId,
                                                  @RequestBody ReservaRequest request) {
        try {
            ReservaDTO reserva = reservaService.update(id, tenantId, request);
            return ResponseEntity.ok(new GenericResponse(200, "Reservación actualizada exitosamente", reserva));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error actualizando reservación {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Cancelar reservación")
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<GenericResponse> cancel(@PathVariable Long id, @RequestParam Long tenantId) {
        try {
            ReservaDTO reserva = reservaService.cancel(id, tenantId);
            return ResponseEntity.ok(new GenericResponse(200, "Reservación cancelada exitosamente", reserva));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error cancelando reservación {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Eliminar reservación")
    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponse> delete(@PathVariable Long id, @RequestParam Long tenantId) {
        try {
            reservaService.delete(id, tenantId);
            return ResponseEntity.ok(new GenericResponse(200, "Reservación eliminada exitosamente", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(new GenericResponse(404, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error eliminando reservación {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }
}