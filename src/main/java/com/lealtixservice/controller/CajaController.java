package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.caja.*;
import com.lealtixservice.entity.LiquidacionPropina;
import com.lealtixservice.entity.TenantUser;
import com.lealtixservice.enums.RoleEnum;
import com.lealtixservice.repository.TenantUserRepository;
import com.lealtixservice.service.CajaTurnoService;
import com.lealtixservice.util.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "Caja y Turnos", description = "Operaciones de caja, apertura/cierre de turnos, pre-cuentas, cobros y liquidación de propinas")
@RestController
@RequestMapping("/api/caja")
@RequiredArgsConstructor
public class CajaController {

    private final CajaTurnoService cajaTurnoService;
    private final TenantUserRepository tenantUserRepository;

    @Operation(summary = "Obtener turno activo del cajero")
    @GetMapping("/turno-activo")
    public ResponseEntity<GenericResponse> getTurnoActivo(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long cajeroId) {
        try {
            TurnoDTO turno = cajaTurnoService.obtenerTurnoActivo(tenantId, cajeroId);
            return ResponseEntity.ok(new GenericResponse(200, "Turno activo", turno));
        } catch (Exception e) {
            log.error("Error obteniendo turno activo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }

    @Operation(summary = "Abrir un turno de caja")
    @PostMapping("/turnos/abrir")
    @RequirePermission(value = "process_payment", alternative = {"manage_all"})
    public ResponseEntity<GenericResponse> abrirTurno(@Valid @RequestBody AbrirTurnoRequest request) {
        try {
            TurnoDTO turno = cajaTurnoService.abrirTurno(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new GenericResponse(201, "Turno abierto exitosamente", turno));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error abriendo turno", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }

    @Operation(summary = "Cerrar turno y realizar corte de caja")
    @PostMapping("/turnos/cerrar")
    @RequirePermission(value = "process_payment", alternative = {"manage_all"})
    public ResponseEntity<GenericResponse> cerrarTurno(@Valid @RequestBody CerrarTurnoRequest request) {
        try {
            TurnoDTO turno = cajaTurnoService.cerrarTurno(request);
            return ResponseEntity.ok(new GenericResponse(200, "Turno cerrado exitosamente", turno));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error cerrando turno", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }

    @Operation(summary = "Tablero de caja: Cuentas Abiertas vs Cuentas Por Cobrar")
    @GetMapping("/tablero")
    @RequirePermission(value = "process_payment", alternative = {"manage_all", "view_sales", "view_reports", "view_dashboard"})
    public ResponseEntity<GenericResponse> getTablero(@RequestParam Long tenantId) {
        try {
            TableroCajaDTO tablero = cajaTurnoService.obtenerTablero(tenantId);
            if (tablero == null) {
                tablero = TableroCajaDTO.builder()
                        .cuentasAbiertas(new ArrayList<>())
                        .cuentasPorCobrar(new ArrayList<>())
                        .build();
            }
            return ResponseEntity.ok(new GenericResponse(200, "Tablero de caja", tablero));
        } catch (Exception e) {
            log.error("Error obteniendo tablero de caja", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }

    @Operation(summary = "Imprimir ticket de pre-cuenta y cambiar estado a POR_COBRAR")
    @PostMapping("/comandas/{orderId}/imprimir-ticket")
    @RequirePermission(value = "process_payment", alternative = {"manage_all"})
    public ResponseEntity<GenericResponse> imprimirTicket(
            @PathVariable UUID orderId,
            @RequestParam Long tenantId) {
        try {
            TicketPrecuentaDTO ticket = cajaTurnoService.imprimirTicketPrecuenta(tenantId, orderId);
            return ResponseEntity.ok(new GenericResponse(200, "Ticket de pre-cuenta generado", ticket));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error imprimiendo ticket", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }

    @Operation(summary = "Procesar cobro de comanda en caja (Transaccional ACID)")
    @PostMapping("/comandas/{orderId}/pagar")
    @RequirePermission(value = "process_payment", alternative = {"manage_all"})
    public ResponseEntity<GenericResponse> pagarComanda(
            @PathVariable UUID orderId,
            @Valid @RequestBody CobrarComandaRequest request) {
        try {
            PagoDTO pago = cajaTurnoService.cobrarComanda(orderId, request);
            return ResponseEntity.ok(new GenericResponse(200, "Pago registrado exitosamente", pago));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error cobrando comanda", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }

    @Operation(summary = "Resumen financiero del turno (Arqueo de caja)")
    @GetMapping("/turnos/{idTurno}/resumen")
    public ResponseEntity<GenericResponse> getResumenTurno(
            @PathVariable Long idTurno,
            @RequestParam Long tenantId) {
        try {
            ResumenTurnoCorteDTO resumen = cajaTurnoService.obtenerResumenTurno(tenantId, idTurno);
            return ResponseEntity.ok(new GenericResponse(200, "Resumen de turno", resumen));
        } catch (Exception e) {
            log.error("Error obteniendo resumen de turno", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }

    @Operation(summary = "Corte y rendimiento por mesero (opcionalmente acotado a un día)")
    @GetMapping("/cortes/mesero/{idMesero}")
    public ResponseEntity<GenericResponse> getCorteMesero(
            @PathVariable Long idMesero,
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long idTurno,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        try {
            CorteMeseroDTO corte = cajaTurnoService.obtenerCorteMesero(tenantId, idMesero, idTurno, fecha);
            return ResponseEntity.ok(new GenericResponse(200, "Corte de mesero", corte));
        } catch (Exception e) {
            log.error("Error obteniendo corte de mesero", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }

    @Operation(summary = "Liquidar propinas acumuladas a un mesero")
    @PostMapping("/propinas/liquidar")
    public ResponseEntity<GenericResponse> liquidarPropinas(@Valid @RequestBody LiquidarPropinasRequest request) {
        try {
            LiquidacionPropina liq = cajaTurnoService.liquidarPropinasMesero(request);
            return ResponseEntity.ok(new GenericResponse(200, "Propinas liquidadas exitosamente", liq));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error liquidando propinas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }

    @Operation(summary = "Listar meseros del negocio para corte de propinas")
    @GetMapping("/meseros")
    public ResponseEntity<GenericResponse> getMeseros(@RequestParam Long tenantId) {
        try {
            List<TenantUser> users = tenantUserRepository.findByTenantId(tenantId, Pageable.unpaged()).getContent();
            List<Map<String, Object>> meseros = users.stream()
                    .filter(u -> u.getRol() == RoleEnum.MESERO || u.getRol() == RoleEnum.ADMIN)
                    .map(u -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", u.getId());
                        map.put("nombre", u.getNombre());
                        map.put("email", u.getEmail());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new GenericResponse(200, "Lista de meseros", meseros));
        } catch (Exception e) {
            log.error("Error obteniendo lista de meseros", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, e.getMessage(), null));
        }
    }
}
