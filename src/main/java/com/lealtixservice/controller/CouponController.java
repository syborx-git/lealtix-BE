package com.lealtixservice.controller;

import com.lealtixservice.dto.CouponResponseDTO;
import com.lealtixservice.dto.GenericResponse;
import com.lealtixservice.entity.Coupon;
import com.lealtixservice.exception.BusinessRuleException;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@Tag(name = "Coupon", description = "Operaciones relacionadas con cupones de bienvenida")
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "Obtener un cupón por su código")
    @GetMapping("/{code}")
    public ResponseEntity<GenericResponse> getByCode(@PathVariable String code) {
        try {
            Optional<Coupon> coupon = couponService.findByCode(code);
            if (coupon.isPresent()) {
                CouponResponseDTO dto = couponService.toDTO(coupon.get());
                return ResponseEntity.ok(new GenericResponse(200, "Cupón encontrado", dto));
            } else {
                return ResponseEntity.ok(new GenericResponse(404, "Cupón no encontrado", null));
            }
        } catch (Exception e) {
            log.error("Error obteniendo cupón por código: {}", code, e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Listar todos los cupones de un cliente")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<GenericResponse> getByCustomerId(@PathVariable Long customerId) {
        try {
            List<Coupon> coupons = couponService.findByCustomerId(customerId);
            List<CouponResponseDTO> dtos = couponService.toDTOList(coupons);
            return ResponseEntity.ok(new GenericResponse(200, "Cupones encontrados", dtos));
        } catch (Exception e) {
            log.error("Error obteniendo cupones del cliente: {}", customerId, e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Asignar un cupón a un cliente")
    @PostMapping("/assign")
    public ResponseEntity<GenericResponse> assignCoupon(@RequestBody com.lealtixservice.dto.AssignCouponRequest request) {
        try {
            log.info("POST /api/coupons/assign - customerId={}, campaignId={}",
                    request != null ? request.getCustomerId() : null,
                    request != null ? request.getCampaignId() : null);

            Coupon coupon = couponService.assignCoupon(request);
            CouponResponseDTO dto = couponService.toDTO(coupon);
            return ResponseEntity.ok(new GenericResponse(200, "Cupón asignado exitosamente", dto));
        } catch (ResourceNotFoundException ex) {
            log.warn("Recurso no encontrado al asignar cupón: {}", ex.getMessage());
            return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), null));
        } catch (BusinessRuleException ex) {
            log.warn("Regla de negocio no satisfecha al asignar cupón: {}", ex.getMessage());
            return ResponseEntity.ok(new GenericResponse(422, ex.getMessage(), null));
        } catch (IllegalArgumentException ex) {
            log.warn("Parámetros inválidos al asignar cupón: {}", ex.getMessage());
            return ResponseEntity.ok(new GenericResponse(400, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error al asignar cupón", e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Canjear un cupón (REDEEM)")
    @PostMapping("/redeem/{code}")
    public ResponseEntity<GenericResponse> redeemCoupon(
            @PathVariable String code,
            @RequestBody(required = false) RedeemRequest request) {
        try {
            String metadata = request != null ? request.getMetadata() : null;
            Coupon redeemed = couponService.redeemCoupon(code, metadata);
            CouponResponseDTO dto = couponService.toDTO(redeemed);
            return ResponseEntity.ok(new GenericResponse(200, "Cupón canjeado exitosamente", dto));
        } catch (ResourceNotFoundException ex) {
            log.warn("Cupón no encontrado: {}", code);
            return ResponseEntity.ok(new GenericResponse(404, ex.getMessage(), null));
        } catch (BusinessRuleException ex) {
            log.warn("Error de negocio al canjear cupón {}: {}", code, ex.getMessage());
            return ResponseEntity.ok(new GenericResponse(422, ex.getMessage(), null));
        } catch (Exception e) {
            log.error("Error canjeando cupón: {}", code, e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Validar si un cupón puede ser canjeado")
    @GetMapping("/{code}/validate")
    public ResponseEntity<GenericResponse> validateCoupon(@PathVariable String code) {
        try {
            Optional<Coupon> couponOpt = couponService.findByCode(code);
            if (couponOpt.isEmpty()) {
                return ResponseEntity.ok(new GenericResponse(404, "Cupón no encontrado", null));
            }

            Coupon coupon = couponOpt.get();
            boolean canBeRedeemed = coupon.canBeRedeemed();

            ValidationResponse validation = ValidationResponse.builder()
                    .code(coupon.getCode())
                    .status(coupon.getStatus().name())
                    .canBeRedeemed(canBeRedeemed)
                    .isExpired(coupon.isExpired())
                    .expiresAt(coupon.getExpiresAt())
                    .message(canBeRedeemed ? "Cupón válido y listo para canjear" :
                            "Cupón no puede ser canjeado. Estado: " + coupon.getStatus())
                    .build();

            return ResponseEntity.ok(new GenericResponse(200, "Validación completada", validation));
        } catch (Exception e) {
            log.error("Error validando cupón: {}", code, e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    @Operation(summary = "Verificar si un cliente tiene cupón activo para una campaña")
    @GetMapping("/check/{customerId}/{campaignId}")
    public ResponseEntity<GenericResponse> hasActiveCoupon(
            @PathVariable Long customerId,
            @PathVariable Long campaignId) {
        try {
            boolean hasActive = couponService.hasActiveCouponForCampaign(customerId, campaignId);
            CheckResponse response = CheckResponse.builder()
                    .customerId(customerId)
                    .campaignId(campaignId)
                    .hasActiveCoupon(hasActive)
                    .message(hasActive ? "El cliente tiene un cupón activo para esta campaña" :
                            "El cliente no tiene cupones activos para esta campaña")
                    .build();
            return ResponseEntity.ok(new GenericResponse(200, "Verificación completada", response));
        } catch (Exception e) {
            log.error("Error verificando cupón activo para customer {} y campaña {}", customerId, campaignId, e);
            return ResponseEntity.ok(new GenericResponse(500, "Error interno del servidor", null));
        }
    }

    // DTOs internos para requests/responses

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RedeemRequest {
        private String metadata; // JSON string con datos adicionales del punto de venta
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ValidationResponse {
        private String code;
        private String status;
        private Boolean canBeRedeemed;
        private Boolean isExpired;
        private java.time.LocalDateTime expiresAt;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CheckResponse {
        private Long customerId;
        private Long campaignId;
        private Boolean hasActiveCoupon;
        private String message;
    }
}

