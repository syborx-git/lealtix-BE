package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.compra.GenerarOrdenCompraRequest;
import com.lealtixservice.dto.compra.RestockDirectoRequest;
import com.lealtixservice.dto.compra.SurtirOrdenCompraRequest;
import com.lealtixservice.service.OrdenCompraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory/ordenes-compra")
@Tag(name = "Ordenes de Compra y Restock", description = "Módulo de compras, lista de reposición y restock automático directo a áreas")
@RequiredArgsConstructor
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;

    @Operation(summary = "Obtener sugerencia de restock automático y lista de compras")
    @GetMapping({"/sugerencia/{tenantId}", "/sugerencia"})
    public ResponseEntity<GenericResponse> getSugerencia(
            @PathVariable(required = false) Long tenantId,
            @RequestParam(name = "tenantId", required = false) Long queryTenantId,
            @RequestParam(required = false, defaultValue = "1.5") Double factor) {
        Long resolved = tenantId != null ? tenantId : queryTenantId;
        if (resolved == null) {
            return ResponseEntity.badRequest().body(new GenericResponse(400, "tenantId es requerido", null));
        }
        return ResponseEntity.ok(new GenericResponse(200, "Sugerencia calculada", ordenCompraService.obtenerSugerenciaRestock(resolved, factor)));
    }

    @Operation(summary = "Generar y guardar una Orden de Compra / Lista de Compras")
    @PostMapping("/generar")
    public ResponseEntity<GenericResponse> generarOrden(@RequestBody GenerarOrdenCompraRequest request) {
        return ResponseEntity.ok(ordenCompraService.generarOrdenCompra(request));
    }

    @Operation(summary = "Listar órdenes de compra por tenant")
    @GetMapping({"/tenant/{tenantId}", ""})
    public ResponseEntity<GenericResponse> listarOrdenes(
            @PathVariable(required = false) Long tenantId,
            @RequestParam(name = "tenantId", required = false) Long queryTenantId,
            @RequestParam(required = false) String estado) {
        Long resolved = tenantId != null ? tenantId : queryTenantId;
        if (resolved == null) {
            return ResponseEntity.badRequest().body(new GenericResponse(400, "tenantId es requerido", null));
        }
        return ResponseEntity.ok(ordenCompraService.listarOrdenes(resolved, estado));
    }

    @Operation(summary = "Obtener detalle de una orden de compra")
    @GetMapping("/{id}")
    public ResponseEntity<GenericResponse> obtenerOrden(@PathVariable Long id) {
        return ResponseEntity.ok(ordenCompraService.obtenerOrden(id));
    }

    @Operation(summary = "Surtir orden de compra y enviar stock directamente a Cocina/Barra/Bodega")
    @PostMapping("/{id}/surtir")
    public ResponseEntity<GenericResponse> surtirOrden(
            @PathVariable Long id,
            @RequestBody SurtirOrdenCompraRequest request) {
        return ResponseEntity.ok(ordenCompraService.surtirOrden(id, request));
    }

    @Operation(summary = "Restock Automático Directo en 1-Clic a Cocina/Barra (crea y surte la orden en una sola transacción)")
    @PostMapping("/restock-directo")
    public ResponseEntity<GenericResponse> restockDirecto(@RequestBody RestockDirectoRequest request) {
        return ResponseEntity.ok(ordenCompraService.restockDirecto(request));
    }

    @Operation(summary = "Cancelar orden de compra")
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<GenericResponse> cancelarOrden(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        String motivo = body != null && body.get("motivo") != null ? body.get("motivo").toString() : null;
        return ResponseEntity.ok(ordenCompraService.cancelarOrden(id, motivo));
    }
}
