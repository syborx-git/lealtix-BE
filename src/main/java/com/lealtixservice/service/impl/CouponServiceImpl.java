package com.lealtixservice.service.impl;

import com.lealtixservice.dto.CouponResponseDTO;
import com.lealtixservice.entity.Campaign;
import com.lealtixservice.entity.Coupon;
import com.lealtixservice.entity.PromotionReward;
import com.lealtixservice.entity.TenantCustomer;
import com.lealtixservice.enums.CouponStatus;
import com.lealtixservice.exception.BusinessRuleException;
import com.lealtixservice.exception.ResourceNotFoundException;
import com.lealtixservice.dto.AssignCouponRequest;
import com.lealtixservice.enums.RewardType;
import com.lealtixservice.repository.CampaignRepository;
import com.lealtixservice.repository.CouponRepository;
import com.lealtixservice.repository.PromotionRewardRepository;
import com.lealtixservice.repository.TenantCustomerRepository;
import com.lealtixservice.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final TenantCustomerRepository customerRepository;
    private final CampaignRepository campaignRepository;
    private final PromotionRewardRepository promotionRewardRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Genera un cupón de bienvenida para un cliente en una campaña específica.
     *
     * Reglas de negocio:
     * - El código del cupón es un UUID único
     * - El estado inicial es ACTIVE
     * - Se genera una URL de QR apuntando al endpoint de canje
     * - Si la campaña tiene endDate, el cupón expira en esa fecha
     * - NO se genera cupón si el cliente ya tiene uno activo para esa campaña
     *
     * @param campaign Campaña de bienvenida (debe estar ACTIVE)
     * @param customer Cliente que recibe el cupón
     * @return Cupón generado y persistido
     * @throws BusinessRuleException si el cliente ya tiene un cupón activo para esta campaña
     */
    @Override
    public Coupon generateWelcomeCoupon(Campaign campaign, TenantCustomer customer) {
        log.info("Generando cupón de bienvenida para customer {} en campaña {}",
                customer.getId(), campaign.getId());

        // Log adicional para verificar que las entidades fueron cargadas correctamente
        try {
            log.debug("Detalles de la campaña - id: {}, title: {}, businessId: {}, status: {}, template: {}, promotionReward: {}",
                campaign.getId(),
                campaign.getTitle(),
                campaign.getBusinessId(),
                campaign.getStatus(),
                campaign.getTemplate() != null ? campaign.getTemplate().getId() : "null",
                campaign.getPromotionReward() != null ? campaign.getPromotionReward().getId() : "null");

            log.debug("Detalles del customer - id: {}, name: {}, email: {}, tenant: {}",
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getTenant() != null ? customer.getTenant().getId() : "null");
        } catch (Exception e) {
            log.error("Error al loggear detalles de campaña/customer: {}", e.getMessage());
        }

        // Validar que el cliente no tenga ya un cupón activo para esta campaña
        boolean hasActiveCoupon = couponRepository.hasActiveCouponForCampaign(
                customer.getId(), campaign.getId());

        if (hasActiveCoupon) {
            log.warn("El customer {} ya tiene un cupón activo para la campaña {}",
                    customer.getId(), campaign.getId());
            throw new BusinessRuleException(
                    "El cliente ya tiene un cupón activo para esta campaña de bienvenida");
        }

        // Generar código único usando UUID
        String code = generateUniqueCode();

        // Generar URL del QR (apunta al endpoint de canje)
        String qrUrl = generateQrUrl(code);

        // Determinar fecha de expiración (basada en campaign.endDate si existe)
        LocalDateTime expiresAt = null;
        if (campaign.getEndDate() != null) {
            // Convertir LocalDate a LocalDateTime (final del día)
            expiresAt = campaign.getEndDate().atTime(23, 59, 59);
        }

        // Construir el cupón
        Coupon coupon = Coupon.builder()
                .code(code)
                .campaign(campaign)
                .customer(customer)
                .status(CouponStatus.ACTIVE)
                .qrUrl(qrUrl)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .build();

        // Persistir
        Coupon saved = couponRepository.save(coupon);
        log.info("Cupón {} generado exitosamente para customer {} en campaña {}",
                saved.getCode(), customer.getId(), campaign.getId());

        return saved;
    }

    /**
     * Genera un código único basado en UUID (12 caracteres alfanuméricos en mayúsculas).
     * Intenta hasta 5 veces en caso de colisión (muy improbable).
     *
     * @return Código único
     */
    private String generateUniqueCode() {
        int maxAttempts = 5;
        for (int i = 0; i < maxAttempts; i++) {
            String code = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 12)
                    .toUpperCase();

            if (!couponRepository.existsByCode(code)) {
                return code;
            }
            log.warn("Código de cupón {} ya existe, reintentando... (intento {}/{})",
                    code, i + 1, maxAttempts);
        }

        // Fallback: UUID completo sin guiones
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    /**
     * Genera la URL del código QR para canjear el cupón.
     * Formato: {baseUrl}/api/coupons/redeem/{code}
     *
     * @param code Código del cupón
     * @return URL completa para el QR
     */
    private String generateQrUrl(String code) {
        return baseUrl + "/api/coupons/redeem/" + code;
    }

    @Override
    public Optional<Coupon> findByCode(String code) {
        return couponRepository.findByCode(code);
    }

    @Override
    public Optional<Coupon> findByCustomerAndCampaign(Long customerId, Long campaignId) {
        // Busca el cupón activo más reciente para este cliente y campaña
        List<Coupon> activeCoupons = couponRepository.findActiveByCustomerAndCampaign(customerId, campaignId);
        return activeCoupons.stream()
                .filter(coupon -> coupon.canBeRedeemed())
                .findFirst();
    }

    @Override
    public List<Coupon> findByCustomerId(Long customerId) {
        return couponRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional
    public Coupon redeemCoupon(String code, String metadata) {
        log.info("Canjeando cupón con código: {}", code);

        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró un cupón con el código: " + code));

        // Validar que el cupón pueda ser canjeado
        if (!coupon.canBeRedeemed()) {
            throw new BusinessRuleException(
                    "El cupón no puede ser canjeado. Estado actual: " + coupon.getStatus());
        }

        // Canjear el cupón (usando la nueva firma con redeemedBy y metadata)
        // redeemedBy = "SYSTEM" para mantener compatibilidad con el método legacy
        coupon.redeem("SYSTEM", metadata);
        Coupon saved = couponRepository.save(coupon);

        log.info("Cupón {} canjeado exitosamente", code);
        return saved;
    }

    @Override
    public boolean hasActiveCouponForCampaign(Long customerId, Long campaignId) {
        return couponRepository.hasActiveCouponForCampaign(customerId, campaignId);
    }

    @Override
    @Transactional
    public Coupon assignCoupon(AssignCouponRequest request) {
        if (request == null || request.getCustomerId() == null) {
            throw new IllegalArgumentException("customerId es obligatorio");
        }

        log.info("Asignando cupón a cliente: customerId={}, campaignId={}",
                request.getCustomerId(), request.getCampaignId());

        TenantCustomer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + request.getCustomerId()));

        Long tenantId = (customer.getTenant() != null && customer.getTenant().getId() != null)
                ? customer.getTenant().getId()
                : 1L;

        Campaign campaign;
        if (request.getCampaignId() != null) {
            campaign = campaignRepository.findById(request.getCampaignId())
                    .orElseThrow(() -> new ResourceNotFoundException("Campaña no encontrada con id: " + request.getCampaignId()));
        } else {
            // Campaña personalizada o por defecto
            String title = (request.getTitle() != null && !request.getTitle().isBlank())
                    ? request.getTitle()
                    : "Descuento Especial";
            RewardType rewardType = request.getRewardType() != null ? request.getRewardType() : RewardType.PERCENT_DISCOUNT;
            BigDecimal numericValue = request.getDiscountValue() != null ? request.getDiscountValue() : BigDecimal.valueOf(20.0);
            String desc = (request.getDescription() != null && !request.getDescription().isBlank())
                    ? request.getDescription()
                    : (rewardType == RewardType.PERCENT_DISCOUNT ? numericValue + "% de descuento en tu consumo" : "$" + numericValue + " de descuento");

            // Buscar si ya existe una campaña activa con el mismo título en este tenant
            Optional<Campaign> existingCamp = campaignRepository.findByBusinessId(tenantId).stream()
                    .filter(c -> com.lealtixservice.enums.CampaignStatus.ACTIVE.equals(c.getStatus()))
                    .filter(c -> c.getTitle() != null && c.getTitle().equalsIgnoreCase(title))
                    .findFirst();

            if (existingCamp.isPresent()) {
                campaign = existingCamp.get();
            } else {
                int days = (request.getDaysValid() != null && request.getDaysValid() > 0) ? request.getDaysValid() : 90;
                LocalDate startDate = LocalDate.now();
                LocalDate endDate = startDate.plusDays(days);

                Campaign newCamp = Campaign.builder()
                        .businessId(tenantId)
                        .title(title)
                        .subtitle("Beneficio exclusivo para cliente")
                        .description(desc)
                        .callToAction("Obtener beneficio")
                        .channels("email")
                        .isAutomatic(true)
                        .isDraft(false)
                        .status(com.lealtixservice.enums.CampaignStatus.ACTIVE)
                        .promoType(com.lealtixservice.enums.PromoType.DISCOUNT)
                        .segmentation("all")
                        .startDate(startDate)
                        .endDate(endDate)
                        .totalSent(0)
                        .totalFailed(0)
                        .build();

                campaign = campaignRepository.save(newCamp);

                PromotionReward reward = PromotionReward.builder()
                        .campaign(campaign)
                        .rewardType(rewardType)
                        .numericValue(numericValue)
                        .description(desc)
                        .minPurchaseAmount(BigDecimal.ZERO)
                        .usageCount(0)
                        .build();

                promotionRewardRepository.save(reward);
                campaign.setPromotionReward(reward);
            }
        }

        // Si el cliente ya tiene un cupón activo para esta campaña, devolver el existente
        boolean hasActive = couponRepository.hasActiveCouponForCampaign(customer.getId(), campaign.getId());
        if (hasActive) {
            log.info("El cliente {} ya cuenta con un cupón activo para la campaña {}, retornando el existente",
                    customer.getId(), campaign.getId());
            Optional<Coupon> existingCoupon = couponRepository.findByCustomerId(customer.getId()).stream()
                    .filter(c -> c.getCampaign() != null && c.getCampaign().getId().equals(campaign.getId()))
                    .filter(c -> CouponStatus.ACTIVE.equals(c.getStatus()) && !c.isExpired())
                    .findFirst();
            if (existingCoupon.isPresent()) {
                return existingCoupon.get();
            }
        }

        // Generar cupón usando las funciones del sistema
        return generateWelcomeCoupon(campaign, customer);
    }

    @Override
    public CouponResponseDTO toDTO(Coupon coupon) {
        if (coupon == null) {
            return null;
        }

        Campaign campaign = coupon.getCampaign();
        PromotionReward reward = campaign != null ? campaign.getPromotionReward() : null;

        return CouponResponseDTO.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .status(coupon.getStatus())
                .expiresAt(coupon.getExpiresAt())
                .createdAt(coupon.getCreatedAt())
                .redeemedAt(coupon.getRedeemedAt())
                .qrToken(coupon.getQrToken())
                .qrUrl(coupon.getQrUrl())
                .redeemedBy(coupon.getRedeemedBy())
                .redemptionMetadata(coupon.getRedemptionMetadata())
                .expired(coupon.isExpired())
                // Información de la campaña
                .campaignId(campaign != null ? campaign.getId() : null)
                .campaignTitle(campaign != null ? campaign.getTitle() : null)
                // Información del cliente
                .customerId(coupon.getCustomer() != null ? coupon.getCustomer().getId() : null)
                .customerName(coupon.getCustomer() != null ? coupon.getCustomer().getName() : null)
                // Información del reward (completa)
                .rewardDescription(reward != null ? reward.getDescription() : null)
                .minPurchaseAmount(reward != null ? reward.getMinPurchaseAmount() : null)
                .usageLimit(reward != null ? reward.getUsageLimit() : null)
                .usageCount(reward != null ? reward.getUsageCount() : null)
                .rewardType(reward != null ? reward.getRewardType() : null)
                .numericValue(reward != null ? reward.getNumericValue() : null)
                .build();
    }

    @Override
    public List<CouponResponseDTO> toDTOList(List<Coupon> coupons) {
        if (coupons == null) {
            return List.of();
        }
        return coupons.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}

