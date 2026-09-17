package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.StockRequestRequest;
import com.lealtixservice.service.StockRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock-requests")
@RequiredArgsConstructor
public class StockRequestController {

    private final StockRequestService stockRequestService;

    @PostMapping
    public ResponseEntity<GenericResponse> crearSolicitud(@RequestBody StockRequestRequest request) {
        return ResponseEntity.ok(stockRequestService.crearSolicitud(request));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<GenericResponse> listarPorTenant(@PathVariable Long tenantId) {
        return ResponseEntity.ok(stockRequestService.listarPorTenant(tenantId));
    }

    @GetMapping("/pendientes/tenant/{tenantId}")
    public ResponseEntity<GenericResponse> contarPendientes(@PathVariable Long tenantId) {
        return ResponseEntity.ok(stockRequestService.contarPendientesPorArea(tenantId));
    }
}