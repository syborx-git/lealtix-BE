package com.lealtixservice.service.impl;

import com.lealtixservice.dto.CouponValidationResponse;
import com.lealtixservice.entity.Campaign;
import com.lealtixservice.entity.Coupon;
import com.lealtixservice.entity.PromotionReward;
import com.lealtixservice.entity.Tenant;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.enums.CouponStatus;
import com.lealtixservice.repository.CouponRepository;
import com.lealtixservice.repository.TenantRepository;
import com.lealtixservice.service.CouponValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de validación de cupones.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponValidationServiceImpl implements CouponValidationService {

    private final CouponRepository couponRepository;
    private final TenantRepository tenantRepository;

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCouponByQrToken(String qrToken, Long tenantId) {
        log.debug("Validando cupón por QR token para tenant: {}", tenantId);

        // 1. Buscar cupón por QR token con relaciones precargadas (optimizado)
        Coupon coupon = couponRepository.findByQrTokenWithRelations(qrToken)
                .orElse(null);

        if (coupon == null) {
            return CouponValidationResponse.invalidCoupon("Cupón no encontrado");
        }

        // 2. Validar que el cupón pertenece al tenant
        Campaign campaign = coupon.getCampaign();
        if (!campaign.getBusinessId().equals(tenantId)) {
            log.warn("Intento de validar cupón de otro tenant. Cupón tenant: {}, Request tenant: {}",
                    campaign.getBusinessId(), tenantId);
            return CouponValidationResponse.invalidCoupon("Cupón no válido para este negocio");
        }

        return validateCoupon(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCouponByCode(String couponCode, Long tenantId) {
        log.debug("Validando cupón por código para tenant: {}", tenantId);

        // Usar consulta optimizada con JOIN FETCH
        Coupon coupon = couponRepository.findByCodeWithRelations(couponCode)
                .orElse(null);

        if (coupon == null) {
            return CouponValidationResponse.invalidCoupon("Cupón no encontrado");
        }

        Campaign campaign = coupon.getCampaign();
        if (!campaign.getBusinessId().equals(tenantId)) {
            log.warn("Intento de validar cupón de otro tenant");
            return CouponValidationResponse.invalidCoupon("Cupón no válido para este negocio");
        }

        return validateCoupon(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCouponByQrTokenForCustomer(String qrToken) {
        log.debug("Validando cupón por QR token desde perspectiva del cliente");

        // 1. Buscar cupón por QR token con relaciones precargadas (optimizado)
        Coupon coupon = couponRepository.findByQrTokenWithRelations(qrToken)
                .orElse(null);

        if (coupon == null) {
            return CouponValidationResponse.invalidCoupon("Cupón no encontrado");
        }

        // 2. Validar el cupón (sin restricción de tenant)
        return validateCoupon(coupon);
    }

    /**
     * Lógica común de validación de cupón.
     */
    private CouponValidationResponse validateCoupon(Coupon coupon) {
        Campaign campaign = coupon.getCampaign();
        TenantCustomer customer = coupon.getCustomer();
        Tenant tenant = customer.getTenant();

        // 1. Verificar si ya fue redimido
        if (coupon.getStatus() == CouponStatus.REDEEMED) {
            return CouponValidationResponse.alreadyRedeemed(
                    coupon.getCode(),
                    coupon.getRedeemedAt(),
                    campaign.getTitle()
            );
        }

        // 2. Verificar si está expirado
        if (coupon.isExpired()) {
            return CouponValidationResponse.expired(
                    coupon.getCode(),
                    coupon.getExpiresAt(),
                    campaign.getTitle()
            );
        }

        // 3. Verificar si está cancelado
        if (coupon.getStatus() == CouponStatus.CANCELLED) {
            return CouponValidationResponse.invalidCoupon("Este cupón ha sido cancelado");
        }

        // 4. Verificar si está activo (o enviado)
        if (coupon.getStatus() != CouponStatus.ACTIVE && coupon.getStatus() != CouponStatus.SENT) {
            return CouponValidationResponse.invalidCoupon("Cupón no disponible para redención");
        }

        // 5. Obtener información del beneficio y reward
        String benefit = getBenefitDescription(campaign);
        PromotionReward reward = campaign.getPromotionReward();

        // 6. Cupón válido - incluir datos completos del reward si existe
        if (reward != null) {
            return CouponValidationResponse.validCoupon(
                    coupon.getCode(),
                    coupon.getStatus(),
                    coupon.getExpiresAt(),
                    campaign.getTitle(),
                    campaign.getDescription(),
                    benefit,
                    customer.getName(),
                    customer.getEmail(),
                    campaign.getId(),
                    tenant.getId(),
                    tenant.getNombreNegocio(),
                    reward.getDescription(),
                    reward.getMinPurchaseAmount(),
                    reward.getUsageLimit(),
                    reward.getNumericValue(),
                    reward.getUsageCount(),
                    reward.getRewardType()
            );
        } else {
            // Sin reward, usar factory method sin datos de reward
            return CouponValidationResponse.validCoupon(
                    coupon.getCode(),
                    coupon.getStatus(),
                    coupon.getExpiresAt(),
                    campaign.getTitle(),
                    campaign.getDescription(),
                    benefit,
                    customer.getName(),
                    customer.getEmail(),
                    campaign.getId(),
                    tenant.getId(),
                    tenant.getNombreNegocio()
            );
        }
    }

    /**
     * Obtiene la descripción del beneficio desde PromotionReward si existe.
     */
    private String getBenefitDescription(Campaign campaign) {
        PromotionReward reward = campaign.getPromotionReward();
        if (reward != null && reward.getDescription() != null) {
            return reward.getDescription();
        }
        return campaign.getDescription();
    }
}

