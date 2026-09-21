package com.lealtixservice.controller;

import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.dto.MermaRequest;
import com.lealtixservice.service.MermaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/mermas")
@RequiredArgsConstructor
public class MermaController {

    private final MermaService mermaService;

    @PostMapping
    public ResponseEntity<GenericResponse> registrarMerma(@RequestBody MermaRequest request) {
        GenericResponse response = mermaService.registrarMerma(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/administrativa")
    public ResponseEntity<GenericResponse> registrarMermaAdministrativa(@RequestBody MermaRequest request) {
        GenericResponse response = mermaService.registrarMermaAdministrativa(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<GenericResponse> listarPorTenant(@PathVariable Long tenantId) {
        return ResponseEntity.ok(mermaService.listarPorTenant(tenantId));
    }

    @GetMapping("/order/{orderId}/insumos-usados")
    public ResponseEntity<GenericResponse> resolverInsumosUsados(@PathVariable UUID orderId) {
        return ResponseEntity.ok(mermaService.resolverInsumosUsados(orderId));
    }
}