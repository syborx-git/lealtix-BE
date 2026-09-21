package com.lealtixservice.controller;

import com.lealtixservice.dto.AddSeatRequest;
import com.lealtixservice.dto.AssignItemsRequest;
import com.lealtixservice.dto.ComandaAsientoDTO;
import com.lealtixservice.dto.ComandaPagoDTO;
import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.SeatSettleRequest;
import com.lealtixservice.dto.SeatSplitResult;
import com.lealtixservice.dto.UpdateSeatAliasRequest;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.service.ComandaAsientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@Slf4j
@Tag(name = "Comanda Asientos", description = "Gestión de asientos/personas de una comanda para dividir la cuenta")
@RestController
@RequestMapping("/api/comanda-asientos")
@RequiredArgsConstructor
public class ComandaAsientoController {

    private final ComandaAsientoService comandaAsientoService;

    @Operation(summary = "Listar asientos de una comanda")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<GenericResponse> listSeats(@PathVariable UUID orderId) {
        try {
            List<ComandaAsientoDTO> seats = comandaAsientoService.listSeats(orderId);
            return ResponseEntity.ok(new GenericResponse(200, "Asientos de la comanda", seats));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse(404, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error listando asientos de la comanda {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Añadir un asiento/persona a una comanda")
    @PostMapping("/order/{orderId}")
    public ResponseEntity<GenericResponse> addSeat(
            @PathVariable UUID orderId,
            @Valid @RequestBody(required = false) AddSeatRequest request) {
        try {
            AddSeatRequest body = request != null ? request : new AddSeatRequest();
            ComandaAsientoDTO seat = comandaAsientoService.addSeat(orderId, body);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new GenericResponse(201, "Asiento añadido exitosamente", seat));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse(404, ex.getMessage(), null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error añadiendo asiento a la comanda {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Renombrar el alias (persona) de un asiento")
    @PatchMapping("/{seatId}/alias")
    public ResponseEntity<GenericResponse> renameSeat(
            @PathVariable UUID seatId,
            @Valid @RequestBody UpdateSeatAliasRequest request) {
        try {
            ComandaAsientoDTO seat = comandaAsientoService.renameSeat(seatId, request);
            return ResponseEntity.ok(new GenericResponse(200, "Alias actualizado exitosamente", seat));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse(404, ex.getMessage(), null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error actualizando alias del asiento {}", seatId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Asignar ítems de la comanda a un asiento")
    @PostMapping("/{seatId}/assign-items")
    public ResponseEntity<GenericResponse> assignItems(
            @PathVariable UUID seatId,
            @Valid @RequestBody AssignItemsRequest request) {
        try {
            List<ComandaAsientoDTO> seats = comandaAsientoService.assignItems(seatId, request);
            return ResponseEntity.ok(new GenericResponse(200, "Ítems asignados al asiento", seats));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse(404, ex.getMessage(), null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error asignando ítems al asiento {}", seatId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Quitar un asiento de una comanda")
    @DeleteMapping("/{seatId}")
    public ResponseEntity<GenericResponse> deleteSeat(@PathVariable UUID seatId) {
        try {
            comandaAsientoService.deleteSeat(seatId);
            return ResponseEntity.ok(new GenericResponse(200, "Asiento eliminado exitosamente", null));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse(404, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error eliminando asiento {}", seatId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Cobrar asientos: genera sub-comandas con folio derivado (ej: 12345-A)")
    @PostMapping("/order/{orderId}/settle")
    public ResponseEntity<GenericResponse> settleSeats(
            @PathVariable UUID orderId,
            @jakarta.validation.Valid @RequestBody SeatSettleRequest request) {
        try {
            SeatSplitResult result = comandaAsientoService.settleSeats(orderId, request);
            return ResponseEntity.ok(new GenericResponse(200, "Cuenta dividida y cobrada exitosamente", result));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse(404, ex.getMessage(), null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error cobrando asientos de la comanda {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Sub-comandas (pagos por asiento) de una comanda original")
    @GetMapping("/order/{orderId}/payments")
    public ResponseEntity<GenericResponse> getSubComandas(@PathVariable UUID orderId) {
        try {
            List<ComandaPagoDTO> pagos = comandaAsientoService.getSubComandas(orderId);
            return ResponseEntity.ok(new GenericResponse(200, "Sub-comandas de la comanda", pagos));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse(404, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error obteniendo sub-comandas de la comanda {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse(500, "Error interno del servidor", null));
        }
    }
}