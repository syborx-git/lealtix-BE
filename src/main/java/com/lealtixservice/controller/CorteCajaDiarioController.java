package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.corte.CorteCajaDiarioDTO;
import com.lealtixservice.dto.corte.EstadoCorteDiarioHoyDTO;
import com.lealtixservice.dto.corte.GenerarCorteDiarioRequest;
import com.lealtixservice.service.CorteCajaDiarioService;
import com.lealtixservice.util.RequirePermission;
import com.lealtixservice.util.TenantOwnership;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/caja/corte-diario")
@RequiredArgsConstructor
@Tag(name = "Corte de Caja Diario", description = "Endpoints para consulta de estado y generación inmutable del corte del día")
public class CorteCajaDiarioController {

    private final CorteCajaDiarioService corteCajaDiarioService;

    @Operation(summary = "Consultar estado del corte de hoy (si ya fue generado o valores esperados actuales)")
    @GetMapping("/hoy")
    @RequirePermission(value = "process_payment", alternative = {"manage_all", "view_sales"})
    @TenantOwnership(tenantIdParam = "tenantId")
    public ResponseEntity<GenericResponse> getEstadoHoy(
            @Parameter(description = "ID del tenant") @RequestParam Long tenantId) {
        try {
            EstadoCorteDiarioHoyDTO estado = corteCajaDiarioService.consultarEstadoHoy(tenantId);
            return ResponseEntity.ok(new GenericResponse(200, "Estado del corte de hoy", estado));
        } catch (Exception e) {
            log.error("Error al consultar el estado del corte de hoy para tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error al consultar estado de corte", null));
        }
    }

    @Operation(summary = "Generar corte de caja del día (Registro inmutable único por fecha)")
    @PostMapping
    @RequirePermission(value = "process_payment", alternative = {"manage_all", "view_sales"})
    public ResponseEntity<GenericResponse> generarCorteDiario(
            @Valid @RequestBody GenerarCorteDiarioRequest request) {
        try {
            String userEmail = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                userEmail = auth.getName();
            }

            log.info("Generando corte de caja del día para tenant {}. Usuario={}", request.getTenantId(), userEmail);
            CorteCajaDiarioDTO corte = corteCajaDiarioService.generarCorteDiario(request, null, userEmail);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new GenericResponse(201, "Corte de caja del día generado exitosamente", corte));
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("Validación al generar corte de caja: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error inesperado generando corte de caja del día", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno al generar el corte de caja", null));
        }
    }
}
