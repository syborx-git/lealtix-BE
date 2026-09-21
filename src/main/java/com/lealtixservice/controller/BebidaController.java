package com.lealtixservice.controller;

import com.lealtixservice.dto.CrearBebidaRequest;
import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.service.BebidaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bebidas")
@RequiredArgsConstructor
public class BebidaController {

    private final BebidaService bebidaService;

    /** Catálogo de bebidas para el POS: stock disponible calculado por tipo_bebida. */
    @GetMapping("/catalog/tenant/{tenantId}")
    public ResponseEntity<GenericResponse> catalogoPos(@PathVariable Long tenantId) {
        return ResponseEntity.ok(bebidaService.catalogoPos(tenantId));
    }

    /** Alta transaccional: bebida + receta (preparadas) en una sola transacción. */
    @PostMapping
    public ResponseEntity<GenericResponse> crearBebida(@RequestBody CrearBebidaRequest request) {
        return ResponseEntity.ok(bebidaService.crearBebida(request));
    }
}